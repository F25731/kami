// 精简模拟数据，仅用于本地界面兜底展示。

export const mockAdmins = [
  { id: 1, username: 'admin', password: 'admin123', role: 'admin', email: 'admin@example.com', create_time: '2026-06-21 09:00:00' }
]

export const mockUsers = [
  { id: 1, username: 'demo', password: '123456', role: 'user', email: 'demo@example.com', status: 1, create_time: '2026-06-21 09:00:00' }
]

export const mockCards = [
  { id: 1, card_key: 'YUNYI-TIME-000001', card_type: 'time', duration: 30, status: 0, create_time: '2026-06-21 09:00:00' },
  { id: 2, card_key: 'YUNYI-COUNT-000002', card_type: 'count', total_count: 100, remaining_count: 100, status: 0, create_time: '2026-06-21 09:10:00' }
]

export const mockApiKeys = [
  { id: 1, name: '默认接口密钥', api_key: 'ak_demo_local', status: 1, create_time: '2026-06-21 09:00:00' }
]

export const mockSettings = {
  systemName: '云逸卡密授权中心',
  systemDescription: '私有化多项目卡密授权中心',
  defaultLanguage: 'zh-CN',
  timezone: 'Asia/Shanghai'
}

export const mockSlides = [
  { id: 1, title: '项目授权', description: '按项目隔离卡密、套餐和接口密钥。', icon: 'project' },
  { id: 2, title: '发卡记录', description: '跟踪外部商城或接口发卡结果。', icon: 'records' }
]

export const mockFeatures = [
  { id: 1, title: '多项目管理', description: '统一维护不同应用的授权规则。', icon: 'project' },
  { id: 2, title: '卡密核销', description: '支持时间卡、次数卡和机器码绑定。', icon: 'key' },
  { id: 3, title: '接口密钥', description: '为外部系统提供受控 API 访问。', icon: 'api' }
]

export const mockDeleteKey = (keyId) => new Promise(resolve => {
  setTimeout(() => {
    const index = mockCards.findIndex(item => item.id === keyId)
    if (index >= 0) mockCards.splice(index, 1)
    resolve({ success: index >= 0, message: index >= 0 ? '删除成功' : '卡密不存在' })
  }, 200)
})

export const mockLogin = (username, password, userType) => new Promise(resolve => {
  setTimeout(() => {
    const source = userType === 'admin' ? mockAdmins : mockUsers
    const user = source.find(item => item.username === username && item.password === password)
    if (user) {
      const { password: _password, ...safeUser } = user
      resolve({ success: true, userInfo: safeUser, token: 'mock-token' })
    } else {
      resolve({ success: false, message: '账号或密码错误' })
    }
  }, 200)
})

export const mockCheckLoginStatus = () => ({ isLoggedIn: false, userInfo: null })
export const mockLogout = () => ({ success: true })