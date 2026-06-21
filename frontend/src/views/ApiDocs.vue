<template>
  <div class="api-docs-page">
    <div class="page-header glass-panel">
      <div class="header-left">
        <h2>API文档</h2>
        <p>项目 {{ projectStore.currentProject?.projectName }} 的 API 接口文档</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="copyAllDocs">
          <el-icon><CopyDocument /></el-icon>
          复制全部文档
        </el-button>
        <el-button @click="copyBaseUrl">
          <el-icon><Link /></el-icon>
          复制API地址
        </el-button>
      </div>
    </div>

    <div class="api-info glass-panel">
      <h3>基础信息</h3>
      <div class="info-grid">
        <div class="info-item">
          <label>项目Token</label>
          <div class="info-value">
            <code>{{ projectStore.currentProject?.projectToken }}</code>
            <el-button size="small" text @click="copyToClipboard(projectStore.currentProject?.projectToken)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="info-item">
          <label>API Base URL</label>
          <div class="info-value">
            <code>{{ baseUrl }}</code>
            <el-button size="small" text @click="copyToClipboard(baseUrl)">
              <el-icon><CopyDocument /></el-icon>
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

    <div class="auth-section glass-panel">
      <h3>认证方式</h3>
      <p>所有 API 请求需要在请求头中包含以下认证信息：</p>

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
            <tr v-for="header in authHeaders" :key="header.name">
              <td><code>{{ header.name }}</code></td>
              <td>{{ header.desc }}</td>
              <td><code>{{ header.example }}</code></td>
            </tr>
          </tbody>
        </table>
      </div>

      <h4>签名计算方法</h4>
      <div class="code-block">
        <pre><code>{{ signCode }}</code></pre>
        <el-button class="copy-code-btn" size="small" @click="copyToClipboard(signCode)">复制代码</el-button>
      </div>

      <h4>Python示例</h4>
      <div class="code-block">
        <pre><code>{{ pythonCode }}</code></pre>
        <el-button class="copy-code-btn" size="small" @click="copyToClipboard(pythonCode)">复制代码</el-button>
      </div>
    </div>

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

          <div class="endpoint-section" v-if="endpoint.params?.length">
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
            <div class="code-block"><pre><code>{{ endpoint.request }}</code></pre></div>
          </div>

          <div class="endpoint-section" v-if="endpoint.response">
            <h5>响应示例</h5>
            <div class="code-block"><pre><code>{{ endpoint.response }}</code></pre></div>
          </div>
        </div>
      </div>
    </div>

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
          <tr v-for="error in errorCodes" :key="error.code">
            <td><code>{{ error.code }}</code></td>
            <td>{{ error.desc }}</td>
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
import { CopyDocument, Link } from '@element-plus/icons-vue'

const projectStore = useProjectStore()

const baseUrl = computed(() => {
  const token = projectStore.currentProject?.projectToken || '{project_token}'
  return window.location.origin + '/api/p/' + token
})

const authHeaders = [
  { name: 'X-API-KEY', desc: 'API密钥', example: 'your_api_key_here' },
  { name: 'X-TIMESTAMP', desc: '当前Unix时间戳（秒）', example: '1672531200' },
  { name: 'X-NONCE', desc: '随机字符串（防重放）', example: 'abc123xyz' },
  { name: 'X-SIGN', desc: 'HMAC-SHA256签名', example: 'a1b2c3d4...' }
]

const signCode = [
  '// 1. 构造待签名字符串',
  'const signStr = apiKey + timestamp + nonce + requestBody + apiSecret',
  '',
  '// 2. 计算HMAC-SHA256',
  'const signature = CryptoJS.HmacSHA256(signStr, apiSecret).toString()',
  '',
  '// 3. 将signature放入X-SIGN请求头'
].join('\n')

const pythonCode = computed(() => [
  'import hmac',
  'import hashlib',
  'import time',
  'import json',
  'import requests',
  '',
  'api_key = "your_api_key"',
  'api_secret = "your_api_secret"',
  'project_token = "' + (projectStore.currentProject?.projectToken || '{project_token}') + '"',
  '',
  'timestamp = str(int(time.time()))',
  'nonce = "random_string_" + str(time.time())',
  'body = json.dumps({"card_key": "YOUR-CARD-KEY"}, separators=(",", ":"))',
  '',
  'sign_str = api_key + timestamp + nonce + body + api_secret',
  'signature = hmac.new(api_secret.encode(), sign_str.encode(), hashlib.sha256).hexdigest()',
  '',
  'headers = {',
  '    "Content-Type": "application/json",',
  '    "X-API-KEY": api_key,',
  '    "X-TIMESTAMP": timestamp,',
  '    "X-NONCE": nonce,',
  '    "X-SIGN": signature',
  '}',
  '',
  'response = requests.post(',
  '    "' + baseUrl.value + '/cards/verify",',
  '    headers=headers,',
  '    data=body',
  ')',
  'print(response.json())'
].join('\n'))

const endpoints = ref([
  {
    method: 'POST',
    path: '/cards/generate',
    name: '生成卡密',
    description: '批量生成卡密。',
    params: [
      { name: 'package_code', type: 'string', required: true, desc: '套餐代码' },
      { name: 'quantity', type: 'int', required: true, desc: '生成数量' },
      { name: 'order_no', type: 'string', required: false, desc: '订单号，用于幂等' },
      { name: 'expire_time', type: 'string', required: false, desc: '过期时间，ISO格式' }
    ],
    request: '{\n  "package_code": "monthly-vip",\n  "quantity": 10,\n  "order_no": "ORDER123456"\n}',
    response: '{\n  "success": true,\n  "data": {\n    "cards": [\n      "CARD-XXXX-XXXX-XXXX",\n      "CARD-YYYY-YYYY-YYYY"\n    ]\n  }\n}'
  },
  {
    method: 'POST',
    path: '/cards/verify',
    name: '验证卡密',
    description: '验证卡密是否有效。',
    params: [
      { name: 'card_key', type: 'string', required: true, desc: '卡密' }
    ],
    request: '{\n  "card_key": "CARD-XXXX-XXXX-XXXX"\n}',
    response: '{\n  "success": true,\n  "data": {\n    "card_type": "count",\n    "status": 1,\n    "remaining_count": 100,\n    "expire_time": "2024-12-31T23:59:59"\n  }\n}'
  },
  {
    method: 'POST',
    path: '/cards/consume',
    name: '消费卡密',
    description: '消费卡密的次数或时长。',
    params: [
      { name: 'card_key', type: 'string', required: true, desc: '卡密' },
      { name: 'consume_count', type: 'int', required: false, desc: '消费次数，次数卡使用' },
      { name: 'biz_id', type: 'string', required: true, desc: '业务ID，用于幂等' },
      { name: 'device_id', type: 'string', required: false, desc: '设备ID，首次消费时可绑定' }
    ],
    request: '{\n  "card_key": "CARD-XXXX-XXXX-XXXX",\n  "consume_count": 1,\n  "biz_id": "biz_20240101_001",\n  "device_id": "device_abc123"\n}',
    response: '{\n  "success": true,\n  "data": {\n    "remaining_count": 99\n  }\n}'
  },
  {
    method: 'POST',
    path: '/cards/redeem',
    name: '兑换卡密',
    description: '将卡密兑换为用户权益。',
    params: [
      { name: 'card_key', type: 'string', required: true, desc: '卡密' },
      { name: 'user_id', type: 'string', required: true, desc: '用户ID' }
    ],
    request: '{\n  "card_key": "CARD-XXXX-XXXX-XXXX",\n  "user_id": "user_123"\n}',
    response: '{\n  "success": true,\n  "data": {\n    "entitlement_id": 1001\n  }\n}'
  },
  {
    method: 'POST',
    path: '/cards/status',
    name: '查询卡密状态',
    description: '查询卡密的详细状态信息。',
    params: [
      { name: 'card_key', type: 'string', required: true, desc: '卡密' }
    ],
    request: '{\n  "card_key": "CARD-XXXX-XXXX-XXXX"\n}',
    response: '{\n  "success": true,\n  "data": {\n    "card_type": "count",\n    "status": 1,\n    "remaining_count": 100,\n    "bind_device_id": "device_abc123",\n    "expire_time": "2024-12-31T23:59:59"\n  }\n}'
  },
  {
    method: 'POST',
    path: '/entitlements/consume',
    name: '消费用户权益',
    description: '消费用户兑换后的权益。',
    params: [
      { name: 'user_id', type: 'string', required: true, desc: '用户ID' },
      { name: 'consume_count', type: 'int', required: false, desc: '消费次数' },
      { name: 'biz_id', type: 'string', required: true, desc: '业务ID，用于幂等' }
    ],
    request: '{\n  "user_id": "user_123",\n  "consume_count": 1,\n  "biz_id": "biz_20240101_002"\n}',
    response: '{\n  "success": true,\n  "data": {\n    "remaining_count": 99\n  }\n}'
  }
])

const errorCodes = [
  { code: 'PROJECT_NOT_FOUND', desc: '项目不存在或已停用' },
  { code: 'INVALID_API_KEY', desc: 'API Key无效或已停用' },
  { code: 'SIGNATURE_INVALID', desc: '签名验证失败' },
  { code: 'TIMESTAMP_EXPIRED', desc: '请求时间戳已过期，默认5分钟内有效' },
  { code: 'NONCE_REPLAYED', desc: 'Nonce已被使用，请勿重放请求' },
  { code: 'CARD_NOT_FOUND', desc: '卡密不存在' },
  { code: 'CARD_EXPIRED', desc: '卡密已过期' },
  { code: 'CARD_DEPLETED', desc: '卡密次数或时长已耗尽' },
  { code: 'DEVICE_MISMATCH', desc: '设备ID不匹配' }
]

const allDocsText = computed(() => {
  const project = projectStore.currentProject
  const lines = [
    '# API文档',
    '',
    '项目名称：' + (project?.projectName || ''),
    '项目Token：' + (project?.projectToken || '{project_token}'),
    'API Base URL：' + baseUrl.value,
    '请求格式：JSON (application/json)',
    '认证方式：HMAC-SHA256签名',
    '',
    '## 认证请求头',
    ...authHeaders.map(header => '- ' + header.name + '：' + header.desc + '，示例：' + header.example),
    '',
    '## 签名计算方法',
    '~~~javascript',
    signCode,
    '~~~',
    '',
    '## Python示例',
    '~~~python',
    pythonCode.value,
    '~~~',
    '',
    '## 接口列表'
  ]

  endpoints.value.forEach(endpoint => {
    lines.push('', '### ' + endpoint.name, endpoint.method + ' ' + baseUrl.value + endpoint.path, endpoint.description)
    if (endpoint.params?.length) {
      lines.push('', '请求参数：')
      endpoint.params.forEach(param => {
        lines.push('- ' + param.name + ' (' + param.type + ', ' + (param.required ? '必填' : '选填') + ')：' + param.desc)
      })
    }
    if (endpoint.request) lines.push('', '请求示例：', '~~~json', endpoint.request, '~~~')
    if (endpoint.response) lines.push('', '响应示例：', '~~~json', endpoint.response, '~~~')
  })

  lines.push('', '## 错误码')
  errorCodes.forEach(error => lines.push('- ' + error.code + '：' + error.desc))
  return lines.join('\n')
})

async function copyToClipboard(text) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('已复制到剪贴板')
  }
}

function copyBaseUrl() {
  copyToClipboard(baseUrl.value)
}

function copyAllDocs() {
  copyToClipboard(allDocsText.value)
}
</script>

<style scoped lang="scss">
.api-docs-page {
  padding: 24px;
  color: #111827;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 24px;
  margin-bottom: 24px;
  border-radius: 16px;

  .header-left {
    h2 {
      color: #111827;
      font-size: 24px;
      margin: 0 0 8px 0;
    }

    p {
      color: #374151;
      font-size: 14px;
      margin: 0;
    }
  }
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.api-info,
.auth-section,
.error-codes,
.endpoint-card {
  padding: 24px;
  margin-bottom: 24px;
  border-radius: 16px;

  h3 {
    color: #111827;
    font-size: 20px;
    margin: 0 0 20px 0;
  }

  h4 {
    color: #111827;
    font-size: 16px;
    margin: 20px 0 12px 0;
  }

  p {
    color: #374151;
    line-height: 1.6;
    margin-bottom: 16px;
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.info-item {
  label {
    display: block;
    font-size: 13px;
    color: #1f2937;
    margin-bottom: 8px;
  }

  .info-value {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;

    code {
      flex: 1;
      min-width: 0;
      padding: 8px 12px;
      background: rgba(255, 255, 255, 0.28);
      border-radius: 6px;
      font-family: 'Courier New', monospace;
      font-size: 14px;
      overflow-wrap: anywhere;
    }
  }
}

.headers-table,
.params-table {
  width: 100%;
  margin: 16px 0;
  border-collapse: collapse;

  th,
  td {
    padding: 12px;
    text-align: left;
    border-bottom: 1px solid rgba(255, 255, 255, 0.22);
  }

  th {
    background: rgba(255, 255, 255, 0.16);
    color: #111827;
    font-weight: 700;
    font-size: 14px;
  }

  td {
    font-size: 13px;
    color: #111827;
  }

  code {
    background: rgba(255, 255, 255, 0.24);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    font-size: 13px;
  }
}

.code-block {
  position: relative;
  background: rgba(30, 41, 59, 0.24);
  border-radius: 10px;
  padding: 16px;
  margin: 12px 0;
  overflow-x: auto;

  pre {
    margin: 0;
    white-space: pre-wrap;

    code {
      font-family: 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.6;
      color: #111827;
      background: rgba(255, 255, 255, 0.28);
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
  .endpoint-header {
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.22);

    .endpoint-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      code {
        font-family: 'Courier New', monospace;
        font-size: 15px;
        color: #111827;
      }
    }

    h4 {
      margin: 0;
      font-size: 18px;
      font-weight: 700;
    }
  }

  .endpoint-desc {
    color: #374151;
  }

  .endpoint-section {
    margin-bottom: 24px;

    &:last-child {
      margin-bottom: 0;
    }

    h5 {
      font-size: 15px;
      margin: 0 0 12px 0;
      color: #111827;
    }
  }
}

@media (max-width: 720px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions,
  .header-actions .el-button {
    width: 100%;
  }
}
</style>
