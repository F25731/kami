# 多项目卡密授权中心 - API 使用文档

## 概述

本系统为**多项目卡密授权中心**，支持：
- 多项目隔离：每个项目拥有独立的 7 位随机访问码 (project_token)
- 两种使用模式：
  - `direct_license`：软件/APP 直接验证卡密
  - `redeem_to_account`：网站/商城，用户兑换卡密到账户权益
- API Key 权限管理
- 签名验证、时间戳防重放、IP 白名单
- 设备绑定、幂等操作

---

## 认证方式

所有开放 API 使用 **HMAC-SHA256 签名认证**。

### 请求头

```
X-API-KEY: <your_api_key>
X-TIMESTAMP: <unix_timestamp_seconds>
X-NONCE: <random_string>
X-SIGN: <signature>
```

### 签名算法

```
sign = sha256(api_key + timestamp + nonce + request_body + api_secret)
```

- `request_body`：POST 请求的 JSON 字符串（GET 请求为空字符串）
- `timestamp`：Unix 秒级时间戳，服务端允许 ±5 分钟误差
- `nonce`：随机字符串，用于防重放攻击（同一 nonce 在 15 分钟内不可重复使用）
- `api_secret`：创建 API Key 时生成，**仅展示一次，请妥善保管**

### 示例（Python）

```python
import hashlib
import time
import random
import string
import requests
import json

API_KEY = "your_api_key_here"
API_SECRET = "your_api_secret_here"
PROJECT_TOKEN = "abc123X"  # 7位项目访问码
BASE_URL = f"https://yourdomain.com/api/p/{PROJECT_TOKEN}"

def generate_signature(api_key, timestamp, nonce, body, api_secret):
    data = api_key + str(timestamp) + nonce + body + api_secret
    return hashlib.sha256(data.encode('utf-8')).hexdigest()

def call_api(endpoint, payload):
    timestamp = int(time.time())
    nonce = ''.join(random.choices(string.ascii_letters + string.digits, k=16))
    body = json.dumps(payload) if payload else ""
    
    signature = generate_signature(API_KEY, timestamp, nonce, body, API_SECRET)
    
    headers = {
        "Content-Type": "application/json",
        "X-API-KEY": API_KEY,
        "X-TIMESTAMP": str(timestamp),
        "X-NONCE": nonce,
        "X-SIGN": signature
    }
    
    response = requests.post(BASE_URL + endpoint, headers=headers, json=payload)
    return response.json()

# 示例：验证卡密
result = call_api("/cards/verify", {"card_key": "XXXX-YYYY-ZZZZ"})
print(result)
```

---

## API 端点列表

所有 API 路径格式：`/api/p/{project_token}/<endpoint>`

### 1. 生成卡密

**POST** `/api/p/{project_token}/cards/generate`

生成新的卡密（需要 `cards:generate` 权限）。

**请求体**

```json
{
  "order_no": "ORDER20260621001",  // 订单号（幂等，可选）
  "quantity": 10,                   // 生成数量
  "card_type": "count",             // 卡密类型: count/time/hybrid/permanent
  "count_value": 100,               // 次数值（次数卡）
  "duration_days": 30,              // 有效天数（时间卡）
  "is_permanent": false,            // 是否永久卡
  "package_id": 123,                // 套餐模板ID（可选）
  "remark": "测试批次"
}
```

**响应**

```json
{
  "success": true,
  "cards": [
    {
      "id": 10001,
      "card_key": "ABCD-EFGH-IJKL",
      "card_type": "count",
      "total_count": 100,
      "remaining_count": 100,
      "status": 0,
      "create_time": "2026-06-21T10:30:00"
    }
  ]
}
```

---

### 2. 验证卡密

**POST** `/api/p/{project_token}/cards/verify`

验证卡密是否存在及其状态（需要 `cards:verify` 权限）。

**请求体**

```json
{
  "card_key": "ABCD-EFGH-IJKL"
}
```

**响应**

```json
{
  "success": true,
  "card_type": "count",
  "status": 0,           // 0=未使用, 1=已使用, 2=已禁用
  "remaining_count": 100,
  "expire_time": null
}
```

---

### 3. 查询卡密状态

**POST** `/api/p/{project_token}/cards/status`

同 `/cards/verify`，返回卡密当前状态。

---

### 4. 消费卡密

**POST** `/api/p/{project_token}/cards/consume`

扣减卡密次数（需要 `cards:consume` 权限）。

**请求体**

```json
{
  "card_key": "ABCD-EFGH-IJKL",
  "biz_id": "user123_2026062110",   // 业务ID（幂等，必填）
  "device_id": "device_abc",         // 设备ID（可选）
  "amount": 1                        // 扣减次数（默认1）
}
```

**响应**

```json
{
  "success": true,
  "remaining_count": 99,
  "idempotent": false  // true=重复请求已忽略
}
```

**错误码**

- `CARD_NOT_FOUND`：卡密不存在
- `CARD_NOT_ACTIVE`：卡密未激活
- `INSUFFICIENT`：次数不足

---

### 5. 兑换卡密到用户权益

**POST** `/api/p/{project_token}/cards/redeem`

将卡密兑换为用户账户权益（适用于 `redeem_to_account` 模式）。

**请求体**

```json
{
  "card_key": "ABCD-EFGH-IJKL",
  "user_id": "user_12345"  // 用户唯一ID
}
```

**响应**

```json
{
  "success": true,
  "entitlement_id": 501,
  "entitlement_type": "vip_monthly",
  "total_count": 100,
  "remaining_count": 100
}
```

---

### 6. 解绑设备

**POST** `/api/p/{project_token}/cards/unbind-device`

解除卡密与设备的绑定（需要 `cards:unbind_device` 权限）。

**请求体**

```json
{
  "card_key": "ABCD-EFGH-IJKL",
  "reason": "用户申请更换设备"
}
```

**响应**

```json
{
  "success": true
}
```

---

### 7. 退还消费

**POST** `/api/p/{project_token}/cards/refund-consume`

退还之前的消费次数（需要 `cards:refund_consume` 权限）。

**请求体**

```json
{
  "card_key": "ABCD-EFGH-IJKL",
  "biz_id": "user123_2026062110"  // 原消费时的业务ID
}
```

**响应**

```json
{
  "success": true,
  "remaining_count": 100
}
```

---

### 8. 查询用户权益状态

**POST** `/api/p/{project_token}/entitlements/status`

查询用户在指定权益类型下的剩余次数和有效期。

**请求体**

```json
{
  "user_id": "user_12345",
  "entitlement_type": "vip_monthly"  // 权益类型（对应套餐 package_code）
}
```

**响应**

```json
{
  "success": true,
  "has_entitlement": true,
  "entitlement_id": 501,
  "total_count": 100,
  "remaining_count": 85,
  "is_permanent": false,
  "start_time": "2026-06-21T10:00:00",
  "expire_time": "2026-07-21T10:00:00",
  "status": "active"
}
```

---

### 9. 消费用户权益

**POST** `/api/p/{project_token}/entitlements/consume`

扣减用户权益次数（需要 `entitlements:consume` 权限）。

**请求体**

```json
{
  "user_id": "user_12345",
  "entitlement_type": "vip_monthly",
  "amount": 1,
  "biz_id": "order_20260621_001"  // 业务ID（幂等，必填）
}
```

**响应**

```json
{
  "success": true,
  "entitlement_id": 501,
  "remaining_count": 84,
  "is_permanent": false
}
```

---

### 10. 退还权益消费

**POST** `/api/p/{project_token}/entitlements/refund-consume`

退还之前的权益消费（需要 `entitlements:refund_consume` 权限）。

**请求体**

```json
{
  "user_id": "user_12345",
  "entitlement_type": "vip_monthly",
  "amount": 1,
  "biz_id": "order_20260621_001"  // 原消费时的业务ID
}
```

**响应**

```json
{
  "success": true,
  "message": "权益已退还"
}
```

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| `UNAUTHORIZED` | 鉴权失败（签名错误、API Key 无效等） |
| `PROJECT_NOT_FOUND` | 项目不存在或已停用 |
| `MISSING_HEADERS` | 缺少必要的请求头 |
| `INVALID_TIMESTAMP` | 时间戳格式无效 |
| `TIMESTAMP_EXPIRED` | 时间戳已过期（±5分钟内有效） |
| `NONCE_REPLAYED` | Nonce 已被使用（防重放） |
| `INVALID_API_KEY` | API Key 无效或已停用 |
| `IP_BLOCKED` | 客户端 IP 不在白名单中 |
| `SIGNATURE_INVALID` | 签名验证失败 |
| `FORBIDDEN` | API Key 无权限 |
| `CARD_NOT_FOUND` | 卡密不存在 |
| `CARD_NOT_ACTIVE` | 卡密未激活 |
| `CARD_ALREADY_USED` | 卡密已被使用 |
| `INSUFFICIENT` | 次数不足 |
| `NO_ENTITLEMENT` | 用户无有效权益 |
| `DUPLICATE_ORDER` | 订单号重复 |
| `LOG_NOT_FOUND` | 未找到对应的消费记录 |
| `ALREADY_REFUNDED` | 该消费记录已退还 |

---

## 管理后台 API

### 套餐管理

- **GET** `/admin/packages?projectId=1` - 获取套餐列表
- **POST** `/admin/packages` - 创建套餐
- **PUT** `/admin/packages/{id}` - 更新套餐
- **PATCH** `/admin/packages/{id}/status` - 启用/禁用套餐
- **DELETE** `/admin/packages/{id}` - 删除套餐

### API Key 管理

- **GET** `/admin/projects/{projectId}/api-keys` - 获取 API Key 列表
- **POST** `/admin/projects/{projectId}/api-keys` - 创建 API Key
- **POST** `/admin/projects/{projectId}/api-keys/{id}/rotate` - 轮换密钥
- **PATCH** `/admin/projects/{projectId}/api-keys/{id}/status` - 启用/禁用
- **DELETE** `/admin/projects/{projectId}/api-keys/{id}` - 删除

---

## 最佳实践

1. **妥善保管 API Secret**：创建时仅展示一次，丢失需轮换密钥
2. **使用业务 ID 实现幂等**：`biz_id` 确保重复请求不会多次扣减
3. **订单号防重复发卡**：`order_no` 确保同一订单不会重复生成卡密
4. **设置合理的时间戳**：客户端与服务器时钟偏差不超过 5 分钟
5. **IP 白名单**：生产环境建议配置 IP 白名单增强安全性
6. **权限最小化**：根据业务需求分配最小必要权限

---

## 联系与支持

- 文档版本：2.0
- 更新日期：2026-06-21
- 技术支持：请通过管理后台提交工单
