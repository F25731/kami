# 客户端 SDK 使用指南

## 概述

本目录包含多项目卡密授权中心的官方客户端 SDK，支持 Python 和 Java。

## Python SDK

### 安装依赖

```bash
pip install requests
```

### 快速开始

```python
from kami_client import KamiClient

# 初始化客户端
client = KamiClient(
    base_url="https://api.example.com",
    project_token="abc123X",  # 从管理后台获取
    api_key="your_api_key",
    api_secret="your_api_secret"
)

# 验证卡密
result = client.verify_card("ABCD-EFGH-IJKL")
print(result)

# 消费卡密（幂等操作）
result = client.consume_card(
    card_key="ABCD-EFGH-IJKL",
    biz_id="user123_order_001",  # 业务唯一ID
    amount=1
)
print(result)
```

### API 方法列表

#### 卡密操作

- `generate_cards()` - 生成卡密
- `verify_card()` - 验证卡密
- `get_card_status()` - 查询卡密状态
- `consume_card()` - 消费卡密
- `redeem_card()` - 兑换卡密到用户权益
- `unbind_device()` - 解绑设备
- `refund_consume()` - 退还消费

#### 用户权益操作

- `get_entitlement_status()` - 查询用户权益状态
- `consume_entitlement()` - 消费用户权益
- `refund_entitlement()` - 退还权益消费

---

## Java SDK

### Maven 依赖

```xml
<dependencies>
    <!-- OkHttp -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- Jackson -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
</dependencies>
```

### 快速开始

```java
import com.example.kami.sdk.KamiClient;
import java.util.Map;

public class Example {
    public static void main(String[] args) {
        // 初始化客户端
        KamiClient client = new KamiClient(
            "https://api.example.com",
            "abc123X",  // 从管理后台获取
            "your_api_key",
            "your_api_secret"
        );

        // 验证卡密
        Map<String, Object> result = client.verifyCard("ABCD-EFGH-IJKL");
        System.out.println(result);

        // 消费卡密（幂等操作）
        result = client.consumeCard(
            "ABCD-EFGH-IJKL",
            "user123_order_001",  // 业务唯一ID
            1,
            null
        );
        System.out.println(result);
    }
}
```

### API 方法列表

#### 卡密操作

- `generateCards()` - 生成卡密
- `verifyCard()` - 验证卡密
- `getCardStatus()` - 查询卡密状态
- `consumeCard()` - 消费卡密
- `redeemCard()` - 兑换卡密到用户权益
- `unbindDevice()` - 解绑设备
- `refundConsume()` - 退还消费

#### 用户权益操作

- `getEntitlementStatus()` - 查询用户权益状态
- `consumeEntitlement()` - 消费用户权益
- `refundEntitlement()` - 退还权益消费

---

## 完整示例

### 场景1：软件授权（direct_license 模式）

```python
# 用户启动软件时验证卡密
result = client.verify_card(card_key)
if result["success"] and result["status"] == 0:
    print("卡密有效，允许使用")

    # 每次使用功能时消费次数
    result = client.consume_card(
        card_key=card_key,
        biz_id=f"user_{user_id}_feature_{feature_id}_{timestamp}",
        amount=1
    )
```

### 场景2：网站会员系统（redeem_to_account 模式）

```python
# 用户输入卡密兑换
result = client.redeem_card(
    card_key=input_card_key,
    user_id=current_user_id
)

if result["success"]:
    print(f"兑换成功！获得 {result['total_count']} 次使用权益")

# 用户使用功能时消费权益
result = client.consume_entitlement(
    user_id=current_user_id,
    entitlement_type="vip_monthly",
    biz_id=f"usage_{timestamp}_{feature_id}",
    amount=1
)
```

### 场景3：设备绑定

```python
# 首次使用时绑定设备
result = client.consume_card(
    card_key=card_key,
    biz_id=f"first_use_{device_id}",
    device_id=device_id,
    amount=1
)

# 用户更换设备时解绑
result = client.unbind_device(
    card_key=card_key,
    reason="用户申请更换设备"
)
```

---

## 错误处理

所有 API 响应都包含 `success` 字段，建议检查该字段：

```python
result = client.verify_card(card_key)

if result["success"]:
    # 成功
    print("卡密状态:", result["status"])
else:
    # 失败
    print("错误码:", result.get("code"))
    print("错误信息:", result.get("message"))
```

常见错误码：

- `CARD_NOT_FOUND` - 卡密不存在
- `CARD_NOT_ACTIVE` - 卡密未激活
- `INSUFFICIENT` - 次数不足
- `UNAUTHORIZED` - 鉴权失败
- `SIGNATURE_INVALID` - 签名验证失败
- `TIMESTAMP_EXPIRED` - 时间戳过期

---

## 幂等性说明

以下操作支持幂等（通过 `biz_id` 或 `order_no` 实现）：

1. **消费卡密** - 同一 `biz_id` 多次调用只会扣减一次
2. **生成卡密** - 同一 `order_no` 多次调用只会生成一次
3. **消费权益** - 同一 `biz_id` 多次调用只会扣减一次

建议在所有消费类操作中使用唯一的 `biz_id`，格式如：
- `{user_id}_{timestamp}_{feature_id}`
- `order_{order_no}_{item_id}`

---

## 性能建议

1. **复用客户端实例** - 避免频繁创建 KamiClient 对象
2. **合理设置超时时间** - 默认 30 秒，可根据网络环境调整
3. **错误重试** - 网络错误时建议指数退避重试
4. **并发控制** - 高并发场景建议使用连接池

---

## 安全建议

1. **保护 API Secret** - 不要将 Secret 写入代码库，使用环境变量
2. **使用 HTTPS** - 生产环境必须使用 HTTPS
3. **IP 白名单** - 在管理后台配置 IP 白名单
4. **最小权限原则** - 为不同场景创建不同权限的 API Key

---

## 技术支持

- API 文档：[API_DOCUMENTATION.md](../../API_DOCUMENTATION.md)
- 实施指南：[IMPLEMENTATION_CHECKLIST.md](../../IMPLEMENTATION_CHECKLIST.md)
- 问题反馈：请通过管理后台提交工单
