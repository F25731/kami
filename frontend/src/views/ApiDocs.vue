<template>
  <div class="api-docs-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>API文档</h2>
        <p>项目 {{ projectStore.currentProject?.projectName }} 的API接口文档</p>
      </div>
      <div class="header-actions">
        <el-button @click="copyBaseUrl">
          <i class="el-icon-link"></i>
          复制API地址
        </el-button>
      </div>
    </div>

    <!-- API基础信息 -->
    <div class="api-info glass-panel">
      <h3>基础信息</h3>
      <div class="info-grid">
        <div class="info-item">
          <label>项目Token</label>
          <div class="info-value">
            <code>{{ projectStore.currentProject?.projectToken }}</code>
            <el-button size="small" text @click="copyToClipboard(projectStore.currentProject?.projectToken)">
              <i class="el-icon-copy-document"></i>
            </el-button>
          </div>
        </div>

        <div class="info-item">
          <label>API Base URL</label>
          <div class="info-value">
            <code>{{ baseUrl }}</code>
            <el-button size="small" text @click="copyToClipboard(baseUrl)">
              <i class="el-icon-copy-document"></i>
            </el-button>
          </div>
        </div>

        <div class="info-item">
          <label>认证方式</label>
          <span>HMAC-SHA256签名</span>
        </div>

        <div class="info-item">
          <label>请求格式</label>
          <span>JSON (application/json)</span>
        </div>
      </div>
    </div>

    <!-- 认证说明 -->
    <div class="auth-section glass-panel">
      <h3>认证方式</h3>
      <p>所有API请求需要在请求头中包含以下认证信息：</p>

      <div class="headers-table">
        <table>
          <thead>
            <tr>
              <th>请求头</th>
              <th>说明</th>
              <th>示例</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>X-API-KEY</code></td>
              <td>API密钥</td>
              <td><code>your_api_key_here</code></td>
            </tr>
            <tr>
              <td><code>X-TIMESTAMP</code></td>
              <td>当前Unix时间戳（秒）</td>
              <td><code>1672531200</code></td>
            </tr>
            <tr>
              <td><code>X-NONCE</code></td>
              <td>随机字符串（防重放）</td>
              <td><code>abc123xyz</code></td>
            </tr>
            <tr>
              <td><code>X-SIGN</code></td>
              <td>HMAC-SHA256签名</td>
              <td><code>a1b2c3d4...</code></td>
            </tr>
          </tbody>
        </table>
      </div>

      <h4>签名计算方法</h4>
      <div class="code-block">
        <pre><code>// 1. 构造待签名字符串
const signStr = apiKey + timestamp + nonce + requestBody + apiSecret

// 2. 计算HMAC-SHA256
const signature = CryptoJS.HmacSHA256(signStr, apiSecret).toString()

// 3. 将signature放入X-SIGN请求头</code></pre>
      </div>

      <h4>Python示例</h4>
      <div class="code-block">
        <pre><code>import hmac
import hashlib
import time
import json
import requests

api_key = "your_api_key"
api_secret = "your_api_secret"
project_token = "{{ projectStore.currentProject?.projectToken }}"

timestamp = str(int(time.time()))
nonce = "random_string_" + str(time.time())
body = json.dumps({"card_key": "YOUR-CARD-KEY"})

sign_str = api_key + timestamp + nonce + body + api_secret
signature = hmac.new(api_secret.encode(), sign_str.encode(), hashlib.sha256).hexdigest()

headers = {
    "Content-Type": "application/json",
    "X-API-KEY": api_key,
    "X-TIMESTAMP": timestamp,
    "X-NONCE": nonce,
    "X-SIGN": signature
}

response = requests.post(
    f"{{ baseUrl }}/cards/verify",
    headers=headers,
    data=body
)</code></pre>
        <el-button class="copy-code-btn" size="small" @click="copyCode('python')">
          复制代码
        </el-button>
      </div>
    </div>

    <!-- API端点列表 -->
    <div class="endpoints-section">
      <div v-for="endpoint in endpoints" :key="endpoint.path" class="endpoint-card glass-panel">
        <div class="endpoint-header">
          <div class="endpoint-title">
            <el-tag :type="endpoint.method === 'POST' ? 'primary' : 'success'" size="small">
              {{ endpoint.method }}
            </el-tag>
            <code>{{ endpoint.path }}</code>
          </div>
          <h4>{{ endpoint.name }}</h4>
        </div>

        <div class="endpoint-content">
          <p class="endpoint-desc">{{ endpoint.description }}</p>

          <div class="endpoint-section" v-if="endpoint.params">
            <h5>请求参数</h5>
            <table class="params-table">
              <thead>
                <tr>
                  <th>参数名</th>
                  <th>类型</th>
                  <th>必填</th>
                  <th>说明</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="param in endpoint.params" :key="param.name">
                  <td><code>{{ param.name }}</code></td>
                  <td><code>{{ param.type }}</code></td>
                  <td>
                    <el-tag :type="param.required ? 'danger' : 'info'" size="small">
                      {{ param.required ? '是' : '否' }}
                    </el-tag>
                  </td>
                  <td>{{ param.desc }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="endpoint-section" v-if="endpoint.request">
            <h5>请求示例</h5>
            <div class="code-block">
              <pre><code>{{ endpoint.request }}</code></pre>
            </div>
          </div>

          <div class="endpoint-section" v-if="endpoint.response">
            <h5>响应示例</h5>
            <div class="code-block">
              <pre><code>{{ endpoint.response }}</code></pre>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 错误码说明 -->
    <div class="error-codes glass-panel">
      <h3>错误码说明</h3>
      <table class="params-table">
        <thead>
          <tr>
            <th>错误码</th>
            <th>说明</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><code>PROJECT_NOT_FOUND</code></td>
            <td>项目不存在或已停用</td>
          </tr>
          <tr>
            <td><code>INVALID_API_KEY</code></td>
            <td>API Key无效或已停用</td>
          </tr>
          <tr>
            <td><code>SIGNATURE_INVALID</code></td>
            <td>签名验证失败</td>
          </tr>
          <tr>
            <td><code>TIMESTAMP_EXPIRED</code></td>
            <td>请求时间戳已过期（±5分钟内有效）</td>
          </tr>
          <tr>
            <td><code>NONCE_REPLAYED</code></td>
            <td>Nonce已被使用，请勿重放请求</td>
          </tr>
          <tr>
            <td><code>CARD_NOT_FOUND</code></td>
            <td>卡密不存在</td>
          </tr>
          <tr>
            <td><code>CARD_EXPIRED</code></td>
            <td>卡密已过期</td>
          </tr>
          <tr>
            <td><code>CARD_DEPLETED</code></td>
            <td>卡密次数/时长已耗尽</td>
          </tr>
          <tr>
            <td><code>DEVICE_MISMATCH</code></td>
            <td>设备ID不匹配</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useProjectStore } from '@/stores/project'
import { ElMessage } from 'element-plus'

const projectStore = useProjectStore()

const baseUrl = computed(() => {
  const token = projectStore.currentProject?.projectToken || '{project_token}'
  return `${window.location.origin}/api/p/${token}`
})

const endpoints = ref([
  {
    method: 'POST',
    path: '/cards/generate',
    name: '生成卡密',
    description: '批量生成卡密',
    params: [
      { name: 'package_code', type: 'string', required: true, desc: '套餐代码' },
      { name: 'quantity', type: 'int', required: true, desc: '生成数量' },
      { name: 'order_no', type: 'string', required: false, desc: '订单号（用于幂等）' },
      { name: 'expire_time', type: 'string', required: false, desc: '过期时间（ISO格式）' }
    ],
    request: `{
  "package_code": "monthly-vip",
  "quantity": 10,
  "order_no": "ORDER123456"
}`,
    response: `{
  "success": true,
  "data": {
    "cards": [
      "CARD-XXXX-XXXX-XXXX",
      "CARD-YYYY-YYYY-YYYY"
    ]
  }
}`
  },
  {
    method: 'POST',
    path: '/cards/verify',
    name: '验证卡密',
    description: '验证卡密是否有效',
    params: [
      { name: 'card_key', type: 'string', required: true, desc: '卡密' }
    ],
    request: `{
  "card_key": "CARD-XXXX-XXXX-XXXX"
}`,
    response: `{
  "success": true,
  "data": {
    "card_type": "count",
    "status": 1,
    "remaining_count": 100,
    "expire_time": "2024-12-31T23:59:59"
  }
}`
  },
  {
    method: 'POST',
    path: '/cards/consume',
    name: '消耗卡密',
    description: '消耗卡密的次数或时长',
    params: [
      { name: 'card_key', type: 'string', required: true, desc: '卡密' },
      { name: 'consume_count', type: 'int', required: false, desc: '消耗次数（次数卡）' },
      { name: 'biz_id', type: 'string', required: true, desc: '业务ID（用于幂等）' },
      { name: 'device_id', type: 'string', required: false, desc: '设备ID（首次消耗时绑定）' }
    ],
    request: `{
  "card_key": "CARD-XXXX-XXXX-XXXX",
  "consume_count": 1,
  "biz_id": "biz_20240101_001",
  "device_id": "device_abc123"
}`,
    response: `{
  "success": true,
  "data": {
    "remaining_count": 99
  }
}`
  },
  {
    method: 'POST',
    path: '/cards/redeem',
    name: '兑换卡密',
    description: '将卡密兑换为用户权益',
    params: [
      { name: 'card_key', type: 'string', required: true, desc: '卡密' },
      { name: 'user_id', type: 'string', required: true, desc: '用户ID' }
    ],
    request: `{
  "card_key": "CARD-XXXX-XXXX-XXXX",
  "user_id": "user_123"
}`,
    response: `{
  "success": true,
  "data": {
    "entitlement_id": 1001
  }
}`
  },
  {
    method: 'POST',
    path: '/cards/status',
    name: '查询卡密状态',
    description: '查询卡密的详细状态信息',
    params: [
      { name: 'card_key', type: 'string', required: true, desc: '卡密' }
    ],
    request: `{
  "card_key": "CARD-XXXX-XXXX-XXXX"
}`,
    response: `{
  "success": true,
  "data": {
    "card_type": "count",
    "status": 1,
    "remaining_count": 100,
    "bind_device_id": "device_abc123",
    "expire_time": "2024-12-31T23:59:59"
  }
}`
  },
  {
    method: 'POST',
    path: '/entitlements/consume',
    name: '消耗用户权益',
    description: '消耗用户兑换后的权益',
    params: [
      { name: 'user_id', type: 'string', required: true, desc: '用户ID' },
      { name: 'consume_count', type: 'int', required: false, desc: '消耗次数' },
      { name: 'biz_id', type: 'string', required: true, desc: '业务ID（用于幂等）' }
    ],
    request: `{
  "user_id": "user_123",
  "consume_count": 1,
  "biz_id": "biz_20240101_002"
}`,
    response: `{
  "success": true,
  "data": {
    "remaining_count": 99
  }
}`
  }
])

function copyToClipboard(text) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

function copyBaseUrl() {
  copyToClipboard(baseUrl.value)
}

function copyCode(lang) {
  const codeElement = document.querySelector('.code-block pre code')
  if (codeElement) {
    copyToClipboard(codeElement.textContent)
  }
}
</script>

<style scoped lang="scss">
.api-docs-page {
  padding: 24px;
  color: white;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  margin-bottom: 24px;
  border-radius: 16px;

  .header-left {
    h2 {
      color: white;
      font-size: 24px;
      margin: 0 0 8px 0;
    }

    p {
      color: rgba(255, 255, 255, 0.7);
      font-size: 14px;
      margin: 0;
    }
  }
}

.api-info, .auth-section, .error-codes {
  padding: 24px;
  margin-bottom: 24px;
  border-radius: 16px;

  h3 {
    font-size: 20px;
    margin: 0 0 20px 0;
  }

  h4 {
    font-size: 16px;
    margin: 20px 0 12px 0;
  }

  p {
    color: rgba(255, 255, 255, 0.8);
    line-height: 1.6;
    margin-bottom: 16px;
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;

  .info-item {
    label {
      display: block;
      font-size: 13px;
      color: rgba(255, 255, 255, 0.6);
      margin-bottom: 8px;
    }

    .info-value {
      display: flex;
      align-items: center;
      gap: 8px;

      code {
        flex: 1;
        padding: 8px 12px;
        background: rgba(0, 0, 0, 0.2);
        border-radius: 6px;
        font-family: 'Courier New', monospace;
        font-size: 14px;
      }
    }
  }
}

.headers-table, .params-table {
  width: 100%;
  margin: 16px 0;
  border-collapse: collapse;

  th, td {
    padding: 12px;
    text-align: left;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  th {
    background: rgba(255, 255, 255, 0.05);
    font-weight: 600;
    font-size: 14px;
  }

  td {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.8);
  }

  code {
    background: rgba(255, 255, 255, 0.1);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    font-size: 13px;
  }
}

.code-block {
  position: relative;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  padding: 16px;
  margin: 12px 0;
  overflow-x: auto;

  pre {
    margin: 0;

    code {
      font-family: 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.6;
      color: #4facfe;
    }
  }

  .copy-code-btn {
    position: absolute;
    top: 12px;
    right: 12px;
  }
}

.endpoints-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.endpoint-card {
  padding: 24px;
  border-radius: 16px;

  .endpoint-header {
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .endpoint-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      code {
        font-family: 'Courier New', monospace;
        font-size: 15px;
        color: #4facfe;
      }
    }

    h4 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
    }
  }

  .endpoint-content {
    .endpoint-desc {
      color: rgba(255, 255, 255, 0.7);
      margin-bottom: 20px;
    }

    .endpoint-section {
      margin-bottom: 24px;

      &:last-child {
        margin-bottom: 0;
      }

      h5 {
        font-size: 15px;
        margin: 0 0 12px 0;
        color: rgba(255, 255, 255, 0.9);
      }
    }
  }
}
</style>
