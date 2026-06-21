import { ElMessage, ElMessageBox } from 'element-plus'

// API鏈嶅姟閰嶇疆
// 浼樺厛浣跨敤鐜鍙橀噺涓殑閰嶇疆锛屽鏋滄病鏈夊垯鏍规嵁鐜鑷姩鍒ゆ柇
// 寮€鍙戠幆澧冧娇鐢?http://localhost:8080/api
// 鐢熶骇鐜浣跨敤 /api (閫氳繃Nginx鍙嶅悜浠ｇ悊)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

/**
 * 閫氱敤鐨凙PI璇锋眰鍑芥暟
 */
async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  
  // Get token from storage
  const token = localStorage.getItem('token');
  
  const defaultOptions = {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    },
  };

  const config = { ...defaultOptions, ...options };
  // Merge headers carefully
  config.headers = { ...defaultOptions.headers, ...options.headers };

  // If body is FormData, let the browser set the Content-Type header
  if (options.body instanceof FormData) {
    delete config.headers['Content-Type'];
  }

  try {
    const response = await fetch(url, config);
    
    if (response.status === 401) {
       if (endpoint.includes('/login') || endpoint.includes('/refresh')) {
           throw new Error('Authentication failed');
       }

       if (isRefreshing) {
         return new Promise((resolve, reject) => {
           failedQueue.push({ resolve, reject });
         }).then(newToken => {
           config.headers['Authorization'] = `Bearer ${newToken}`;
           return fetch(url, config).then(res => res.json());
         });
       }

       isRefreshing = true;
       const refreshToken = localStorage.getItem('refreshToken');
       
       if (!refreshToken) {
           isRefreshing = false;
           throw new Error('No refresh token available');
       }

       try {
           const refreshResponse = await fetch(`${API_BASE_URL}/auth/refresh`, {
               method: 'POST',
               headers: { 'Content-Type': 'application/json' },
               body: JSON.stringify({ refreshToken })
           });

           const refreshData = await refreshResponse.json();

           if (refreshData.success) {
               const newToken = refreshData.data.token;
               localStorage.setItem('token', newToken);
               if (refreshData.data.refreshToken) {
                   localStorage.setItem('refreshToken', refreshData.data.refreshToken);
               }
               
               processQueue(null, newToken);
               isRefreshing = false;
               
               // Retry original request
               config.headers['Authorization'] = `Bearer ${newToken}`;
               const retryResponse = await fetch(url, config);
               return await retryResponse.json();
           } else {
               throw new Error('Refresh failed');
           }
       } catch (refreshError) {
           processQueue(refreshError, null);
           isRefreshing = false;
           // Clear auth data
           localStorage.removeItem('token');
           localStorage.removeItem('refreshToken');
           localStorage.removeItem('user');
           localStorage.removeItem('userInfo');
           localStorage.removeItem('isLoggedIn');

           ElMessageBox.alert('当前登录已过期，请重新登录。', '登录过期', {
             confirmButtonText: '确定',
             type: 'warning',
             showClose: false,
             callback: () => { window.location.href = '/' }
           });
           
           // Throw to stop execution
           throw refreshError;
       }
    }

    if (!response.ok) {
      let errorMsg = `HTTP error! status: ${response.status}`;
      try {
        const errorText = await response.text();
        if (errorText) {
          try {
             const errorJson = JSON.parse(errorText);
             if (errorJson.message) errorMsg = errorJson.message;
             else errorMsg = errorText;
          } catch (e) {
             errorMsg = errorText;
          }
        }
      } catch (e) {
        // ignore
      }
      throw new Error(errorMsg);
    }
    
    // Check content type before parsing JSON
    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
      return await response.json();
    } else {
      // For non-JSON responses (like simple strings), return text
      return await response.text();
    }
  } catch (error) {
    console.error('API request failed:', error);
    throw error;
  }
}

/**
 * 璁よ瘉API鏈嶅姟
 */
export const authApi = {
  /**
   * 绠＄悊鍛樼櫥褰?
   */
  async loginAdmin(username, password, totpCode) {
    return await apiRequest('/auth/admin/login', {
      method: 'POST',
      body: JSON.stringify({ username, password, totpCode })
    });
  },

  /**
   * 鐢ㄦ埛鐧诲綍
   */
  async loginUser(username, password) {
    return await apiRequest('/auth/user/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
  },

  /**
   * 鍙戦€侀偖绠遍獙璇佺爜
   */
  async sendEmailCode(email, type = 'register') {
    return await apiRequest('/auth/email-code', {
      method: 'POST',
      body: JSON.stringify({ email, type })
    });
  },

  /**
   * 鐢ㄦ埛娉ㄥ唽
   */
  async register(data) {
    return await apiRequest('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  /**
   * 缁戝畾娉ㄥ唽
   */
  async registerBind(data) {
    return await apiRequest('/auth/register-bind', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  async logout(id, role) {
    return await apiRequest('/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ id, role })
    });
  },

  async updateAdmin(data) {
    return await apiRequest('/auth/admin/update', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  /**
   * 鑾峰彇褰撳墠鐢ㄦ埛淇℃伅
   */
  async getUserInfo() {
    return await apiRequest('/auth/user/info');
  },

  /**
   * 鑾峰彇TOTP閰嶇疆淇℃伅
   */
  async setupTotp(id) {
    return await apiRequest('/auth/totp/setup', {
      method: 'POST',
      body: JSON.stringify({ id })
    });
  },

  /**
   * 鍚敤TOTP
   */
  async enableTotp(id, secret, code) {
    return await apiRequest('/auth/totp/enable', {
      method: 'POST',
      body: JSON.stringify({ id, secret, code })
    });
  },

  /**
   * 绂佺敤TOTP
   */
  async disableTotp(id) {
    return await apiRequest('/auth/totp/disable', {
      method: 'POST',
      body: JSON.stringify({ id })
    });
  },

  /**
   * 鍙戦€侀噸缃瘑鐮侀獙璇佺爜
   */
  async sendResetCode(username, email) {
    return await apiRequest('/auth/reset-code', {
      method: 'POST',
      body: JSON.stringify({ username, email })
    });
  },

  /**
   * 閲嶇疆瀵嗙爜
   */
  async resetPassword(data) {
    return await apiRequest('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  /**
   * 鑾峰彇缁戝畾Token
   */
  async getBindToken() {
    return await apiRequest('/auth/bind/token', {
      method: 'GET'
    });
  },

  /**
   * 鍙戦€乀OTP鎭㈠楠岃瘉鐮?
   */
  async sendRecoveryCode(username) {
    return await apiRequest('/auth/totp/recovery-code', {
      method: 'POST',
      body: JSON.stringify({ username })
    });
  },

  /**
   * 閫氳繃鎭㈠鐮佺鐢═OTP
   */
  async disableTotpByRecovery(username, code) {
    return await apiRequest('/auth/totp/disable-by-recovery', {
      method: 'POST',
      body: JSON.stringify({ username, code })
    });
  }
};

/**
 * 绯荤粺鐩戞帶API鏈嶅姟
 */
export const monitorApi = {
  /**
   * 鑾峰彇鏁版嵁搴撶姸鎬?
   */
  async getDatabaseStatus() {
    return await apiRequest('/monitor/database');
  },

  /**
   * 鑾峰彇绯荤粺璧勬簮鐘舵€?
   */
  async getSystemStatus() {
    return await apiRequest('/monitor/system');
  },

  /**
   * 鑾峰彇API鏈嶅姟鐘舵€?
   */
  async getApiStatus() {
    return await apiRequest('/monitor/api');
  },

  /**
   * 鑾峰彇鍦ㄧ嚎鐢ㄦ埛淇℃伅
   */
  async getOnlineUsers() {
    return await apiRequest('/monitor/users');
  },

  /**
   * 鑾峰彇鎵€鏈夌洃鎺ф暟鎹?
   */
  async getAllMonitorData() {
    return await apiRequest('/monitor/all');
  }
};

/**
 * 鐢ㄦ埛绠＄悊API鏈嶅姟 (绠＄悊鍛?
 */
export const userApi = {
  // 鑾峰彇鐢ㄦ埛鍒楄〃 (鍒嗛〉, 鎼滅储)
  async getUsers(page = 1, size = 10, keyword = '') {
    const params = new URLSearchParams({ page, size });
    if (keyword) params.append('keyword', keyword);
    return await apiRequest(`/admin/users?${params.toString()}`);
  },

  // 鍒涘缓鐢ㄦ埛
  async createUser(userData) {
    return await apiRequest('/admin/users', {
      method: 'POST',
      body: JSON.stringify(userData)
    });
  },

  // 鏇存柊鐢ㄦ埛
  async updateUser(id, userData) {
    return await apiRequest(`/admin/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(userData)
    });
  },

  // 鍒犻櫎鐢ㄦ埛
  async deleteUser(id) {
    return await apiRequest(`/admin/users/${id}`, {
      method: 'DELETE'
    });
  },

  // 鏇存柊鐢ㄦ埛鐘舵€?
  async updateUserStatus(id, status) {
    return await apiRequest(`/admin/users/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    });
  }
};

/**
 * 鍦ㄧ嚎鐢ㄦ埛绠＄悊API鏈嶅姟
 */
export const onlineUserApi = {
  /**
   * 鐢ㄦ埛涓婄嚎
   */
  async userLogin(userId, username, nickname) {
    return await apiRequest('/online/login', {
      method: 'POST',
      body: JSON.stringify({
        userId: userId.toString(),
        username,
        nickname
      })
    });
  },

  /**
   * 鐢ㄦ埛涓嬬嚎
   */
  async userLogout(userId) {
    return await apiRequest('/online/logout', {
      method: 'POST',
      body: JSON.stringify({
        userId: userId.toString()
      })
    });
  },

  /**
   * 鍙戦€佸績璺筹紝鏇存柊鐢ㄦ埛娲诲姩鏃堕棿
   */
  async sendHeartbeat(userId) {
    return await apiRequest('/online/heartbeat', {
      method: 'POST',
      body: JSON.stringify({
        userId: userId.toString()
      })
    });
  },

  /**
   * 妫€鏌ョ敤鎴锋槸鍚﹀湪绾?
   */
  async checkUserOnline(userId) {
    return await apiRequest(`/online/check/${userId}`);
  },

  /**
   * 鑾峰彇鍦ㄧ嚎鐢ㄦ埛鍒楄〃
   */
  async getOnlineUsers() {
    return await apiRequest('/online/list');
  }
};

/**
 * 鍗″瘑绠＄悊API鏈嶅姟
 */
export const cardApi = {
  /**
   * 鎵归噺鍒涘缓鍗″瘑
   */
  async createCards(data) {
    return await apiRequest('/cards/admin/create', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  /**
   * 鑾峰彇鎵€鏈夊崱瀵?
   */
  async getAllCards() {
    return await apiRequest('/cards/admin/all');
  },

  /**
   * 鑾峰彇鎸囧畾API Key鐨勫崱瀵?
   */
  async getApiKeyCards(apiKeyId) {
    return await apiRequest(`/cards/apikey/${apiKeyId}`);
  },

  /**
   * 鑾峰彇鍗″瘑浣跨敤瓒嬪娍
   */
  async getUsageTrend(days = 7) {
    return await apiRequest(`/cards/trend?days=${days}`);
  },

  /**
   * 浣跨敤鍗″瘑
   */
  async useCard(cardKey, deviceId = 'Unknown', ipAddress = '') {
    return await apiRequest('/cards/use', {
      method: 'POST',
      body: JSON.stringify({ 
        card_key: cardKey,
        device_id: deviceId,
        ip_address: ipAddress
      })
    });
  },

  /**
   * 鑾峰彇鐢ㄦ埛鐨勫崱瀵?
   */
  async getUserCards(userId) {
    return await apiRequest(`/cards/user/${userId}`);
  },

  /**
   * 鍒犻櫎鍗″瘑
   */
  async deleteCard(cardId) {
    return await apiRequest(`/cards/${cardId}`, {
      method: 'DELETE'
    });
  },

  /**
   * 绠＄悊鍛橈細缂栬緫鍗″瘑
   */
  async updateCard(cardId, data) {
    return await apiRequest(`/cards/admin/${cardId}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  /**
   * 绠＄悊鍛橈細鏆傚仠(2) / 鎭㈠鍚敤(1)
   */
  async updateAdminStatus(cardId, status) {
    return await apiRequest(`/cards/admin/${cardId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
  },

  /**
   * 鍏紑椤碉細鏌ヨ鍗″瘑鏄惁宸茬粦瀹氭満鍣ㄧ爜锛堟棤闇€鐧诲綍锛?
   */
  async publicMachineBindQuery(cardKey) {
    return await apiRequest('/public/cards/machine-bind/query', {
      method: 'POST',
      body: JSON.stringify({ card_key: cardKey })
    });
  },

  /**
   * 鍏紑椤碉細瑙ｇ粦鏈哄櫒鐮佷笌璁惧 ID锛堟棤闇€鐧诲綍锛岄』宸茬粦瀹氾級
   */
  async publicMachineUnbind(cardKey) {
    return await apiRequest('/public/cards/machine-bind/unbind', {
      method: 'POST',
      body: JSON.stringify({ card_key: cardKey })
    });
  }
};

/**
 * 绯荤粺璁剧疆API鏈嶅姟
 */
export const settingsApi = {
  /**
   * 鑾峰彇鎵€鏈夎缃?
   */
  async getAllSettings() {
    return await apiRequest('/settings/all');
  },

  /**
   * 鎵归噺淇濆瓨璁剧疆
   */
  async saveSettings(settings) {
    return await apiRequest('/settings/save', {
      method: 'POST',
      body: JSON.stringify(settings)
    });
  },

  /**
   * 鍙戦€佹祴璇曢偖浠?
   */
  async sendTestEmail(to, settings = {}) {
    return await apiRequest('/settings/email/test', {
      method: 'POST',
      body: JSON.stringify({ to, ...settings })
    });
  }
};

/**
 * 鐢ㄦ埛缁熻API鏈嶅姟
 */
export const statsApi = {
  /**
   * 鑾峰彇浠〃鐩樼粺璁℃暟鎹?
   */
  async getDashboardStats() {
    return await apiRequest('/stats/dashboard');
  },

  /**
   * 鑾峰彇鐢ㄦ埛娲昏穬搴︾粺璁?
   */
  async getUserActivityStats(period = '7d') {
    let days = period;
    // 濡傛灉浼犲叆鐨勬槸 '7d' 鏍煎紡锛屾彁鍙栨暟瀛?
    if (typeof period === 'string' && period.endsWith('d')) {
        days = period.replace('d', '');
    }
    return await apiRequest(`/stats/user-activity?days=${days}`);
  },

  /**
   * 鑾峰彇鍗＄墖浣跨敤瓒嬪娍
   */
  async getCardUsageTrends(period = '7') {
    const days = parseInt(period);
    return await apiRequest(`/cards/trend?days=${days}`);
  }
};

/**
 * 璁㈠崟API鏈嶅姟
 */
export const orderApi = {
  /**
   * 鍒涘缓璁㈠崟
   */
  async createOrder(data) {
    return await apiRequest('/orders', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  /**
   * 鑾峰彇鐢ㄦ埛璁㈠崟鍒楄〃
   * @param {number} userId 
   */
  async getOrders(userId) {
    return await apiRequest(`/orders?userId=${userId}`);
  },

  /**
   * 鑾峰彇鎵€鏈夎鍗曪紙绠＄悊鍛橈級
   */
  async getAllOrders(params = {}) {
    // Filter out empty params
    const queryParams = {};
    Object.keys(params).forEach(key => {
        if (params[key] !== null && params[key] !== '' && params[key] !== undefined) {
            queryParams[key] = params[key];
        }
    });
    const queryString = new URLSearchParams(queryParams).toString();
    return await apiRequest(`/orders/admin/all?${queryString}`);
  },

  /**
   * 鏇存柊璁㈠崟鐘舵€侊紙绠＄悊鍛橈級
   */
  async updateOrderStatus(orderNo, status) {
    return await apiRequest('/orders/admin/updateStatus', {
      method: 'POST',
      body: JSON.stringify({ orderNo, status })
    });
  }
};

/**
 * API Key绠＄悊鏈嶅姟
 */
export const apiKeyApi = {
  async getAllApiKeys() {
    return await apiRequest('/admin/apikeys');
  },

  async createApiKey(data) {
    return await apiRequest('/admin/apikeys', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  async updateApiKey(id, data) {
    return await apiRequest(`/admin/apikeys/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  async deleteApiKey(id) {
    return await apiRequest(`/admin/apikeys/${id}`, {
      method: 'DELETE'
    });
  },

  async assignUser(id, userId) {
    return await apiRequest(`/admin/apikeys/${id}/users`, {
      method: 'POST',
      body: JSON.stringify({ userId })
    });
  },

  async unassignUser(id, userId) {
    return await apiRequest(`/admin/apikeys/${id}/users/${userId}`, {
      method: 'DELETE'
    });
  },

  async getAllUsers() {
    return await apiRequest('/admin/users?size=9999');
  }
};

/**
 * 鍗″瘑瀹氫环API鏈嶅姟
 */
export const pricingApi = {
  getAllPricing() {
    return apiRequest('/pricing');
  },
  
  addPricing(data) {
    return apiRequest('/pricing', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },
  
  updatePricing(id, data) {
    return apiRequest(`/pricing/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },
  
  deletePricing(id) {
    return apiRequest(`/pricing/${id}`, {
      method: 'DELETE'
    });
  }
};

/**
 * 鐢ㄦ埛涓汉淇℃伅API鏈嶅姟
 */
export const userProfileApi = {
  /**
   * 鑾峰彇涓汉淇℃伅
   */
  async getProfile() {
    return await apiRequest('/user/profile');
  },

  /**
   * 鏇存柊涓汉淇℃伅
   */
  async updateProfile(data) {
    return await apiRequest('/user/profile', {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  /**
   * 涓婁紶澶村儚
   */
  async uploadAvatar(file) {
    const formData = new FormData();
    formData.append('file', file);
    return await apiRequest('/user/avatar', {
      method: 'POST',
      body: formData
    });
  },

  /**
   * 淇敼瀵嗙爜
   */
  async changePassword(oldPassword, newPassword) {
    return await apiRequest('/user/password', {
      method: 'POST',
      body: JSON.stringify({ oldPassword, newPassword })
    });
  },

  /**
   * 鑾峰彇绀句氦璐﹀彿缁戝畾鍒楄〃
   */
  async getSocialBindings() {
    return await apiRequest('/user/social');
  },

  /**
   * 缁戝畾绀句氦璐﹀彿
   */
  async bindSocial(token) {
    return await apiRequest('/user/social/bind', {
      method: 'POST',
      body: JSON.stringify({ token })
    });
  },

  /**
   * 瑙ｇ粦绀句氦璐﹀彿
   */
  async unbindSocial(type) {
    return await apiRequest('/user/social/unbind', {
      method: 'POST',
      body: JSON.stringify({ type })
    });
  }
};

/**
 * 绯荤粺缁存姢API鏈嶅姟
 */
export const maintenanceApi = {
  /**
   * 鑾峰彇缁存姢鐘舵€?
   */
  async getStatus() {
    return await apiRequest('/maintenance/status');
  },

  /**
   * 鏇存柊缁存姢璁剧疆
   */
  async updateSettings(settings) {
    return await apiRequest('/maintenance/update', {
      method: 'POST',
      body: JSON.stringify(settings)
    });
  },

  /**
   * 鍒涘缓澶囦唤
   */
  async createBackup() {
    return await apiRequest('/backup/create', {
        method: 'POST'
    });
  },

  /**
   * 娓呯悊缂撳瓨
   */
  async clearCache() {
    return await apiRequest('/maintenance/clear-cache', {
        method: 'POST'
    });
  },

  /**
   * 娓呯悊鏃ュ織
   */
  async clearLogs() {
    return await apiRequest('/maintenance/clear-logs', {
        method: 'POST'
    });
  }
};

/**
 * Project management API.
 */
export const projectApi = {
  async getProjects() {
    return await apiRequest('/admin/projects');
  },

  async getProject(id) {
    return await apiRequest(`/admin/projects/${id}`);
  },

  async createProject(data) {
    return await apiRequest('/admin/projects', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  async updateProject(id, data) {
    return await apiRequest(`/admin/projects/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  async updateStatus(id, status) {
    return await apiRequest(`/admin/projects/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
  },

  async regenerateToken(id) {
    return await apiRequest(`/admin/projects/${id}/regenerate-token`, {
      method: 'POST'
    });
  },

  async deleteProject(id) {
    return await apiRequest(`/admin/projects/${id}`, {
      method: 'DELETE'
    });
  },

  async getApiKeys(projectId) {
    return await apiRequest(`/admin/projects/${projectId}/api-keys`);
  },

  async createApiKey(projectId, data) {
    return await apiRequest(`/admin/projects/${projectId}/api-keys`, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  async rotateApiKey(projectId, id) {
    return await apiRequest(`/admin/projects/${projectId}/api-keys/${id}/rotate`, {
      method: 'POST'
    });
  },

  async updateApiKeyStatus(projectId, id, status) {
    return await apiRequest(`/admin/projects/${projectId}/api-keys/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
  }
};

/**
 */

export default {
  authApi,
  monitorApi,
  onlineUserApi,
  cardApi,
  settingsApi,
  statsApi,
  orderApi,
  apiKeyApi,
  userApi,
  userProfileApi,
  maintenanceApi,
  pricingApi,
  projectApi
};


