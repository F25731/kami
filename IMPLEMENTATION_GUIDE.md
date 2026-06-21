# 多项目卡密授权中心 - 完整实施指南

## 项目现状评估

Codex 已完成约 60-70% 的基础架构，但缺少关键业务逻辑和前端页面。

### ✅ 已完成部分
- 基础项目表 (projects)
- 基础数据库迁移脚本
- 项目管理基础 CRUD
- 部分日志表结构

### ❌ 缺失部分（关键）

#### 数据库层面
1. card_packages 表（套餐模板）
2. project_api_keys 表（独立的项目API密钥表）
3. card_issue_orders 表（发卡订单，防重复）
4. device_bind_logs 表（设备绑定日志）
5. card_redeem_logs 表（卡密兑换日志）
6. entitlement_consume_logs 表（用户权益消费日志）
7. webhook_logs 表（WebHook日志）
8. cards 表缺少字段：package_id, order_no, bind_device_id, bind_time, redeemed_user_id, redeemed_at, activated_at

#### 后端层面
9. 项目专属 API 路由：`/api/p/{project_token}/*`
10. project_token 解析拦截器
11. API 签名验证拦截器
12. 10个核心开放 API（verify, consume, redeem, status等）
13. 用户权益相关 Service 和 API
14. WebHook 触发机制
15. 7位随机码生成工具类
16. default 项目的 project_token 修复逻辑

#### 前端层面
17. 套餐管理页面
18. 项目 API Key 管理页面
19. 项目 API 文档自动生成页面
20. 发卡订单页面
21. 用户权益管理页面
22. 设备绑定管理页面
23. 原品牌信息清理

---

## 完整文件清单

### 需要新增的文件（约40个）

#### 后端实体类 (7个)
```
backend/src/main/java/org/xxg/backend/backend/entity/
├── CardPackage.java ✓ (已创建)
├── ProjectApiKey.java ✓ (已创建)
├── UserEntitlement.java ✓ (已创建)
├── CardIssueOrder.java
├── ApiCallLog.java
├── ConsumeLog.java
└── DeviceBindLog.java
```

#### 后端 Mapper (7个)
```
backend/src/main/java/org/xxg/backend/backend/mapper/
├── CardPackageMapper.java
├── ProjectApiKeyMapper.java
├── UserEntitlementMapper.java
├── CardIssueOrderMapper.java
├── ApiCallLogMapper.java
├── ConsumeLogMapper.java
└── DeviceBindLogMapper.java
```

#### 后端 Service (8个)
```
backend/src/main/java/org/xxg/backend/backend/service/
├── CardPackageService.java
├── ProjectApiKeyService.java
├── UserEntitlementService.java
├── CardIssueOrderService.java
├── OpenApiAuthService.java (API鉴权)
├── SignatureService.java (签名验证)
├── WebHookService.java
└── TokenGeneratorService.java (生成随机码)
```

#### 后端 Controller (3个)
```
backend/src/main/java/org/xxg/backend/backend/controller/
├── CardPackageController.java (管理后台套餐管理)
├── ProjectApiKeyController.java (管理后台API Key管理)
└── ProjectOpenApiController.java (对外开放API)
```

#### 拦截器和过滤器 (2个)
```
backend/src/main/java/org/xxg/backend/backend/filter/
├── ProjectTokenFilter.java (解析 project_token)
└── ApiSignatureFilter.java (验证 API 签名)
```

#### 前端页面 (7个)
```
src/components/
├── CardPackageManagePage.vue (套餐管理)
├── ProjectApiKeyManagePage.vue (API Key管理)
├── ProjectApiDocPage.vue (API文档)
├── CardIssueOrdersPage.vue (发卡订单)
├── UserEntitlementsPage.vue (用户权益)
├── DeviceBindManagePage.vue (设备绑定)
└── ApiCallLogsPage.vue (API调用日志)
```

### 需要修改的文件（约15个）

```
backend/src/main/java/org/xxg/backend/backend/
├── entity/Card.java (添加缺失字段)
├── mapper/CardMapper.java (添加项目隔离查询)
├── service/CardService.java (改造为项目隔离)
├── service/ProjectService.java (添加token修复逻辑)
├── config/LicenseCenterSchemaMigrator.java (添加启动时修复逻辑)
└── config/WebConfig.java (注册拦截器)

src/
├── App.vue (修改菜单结构)
└── main.js (添加新路由)
```

---

## 实施优先级

### P0 - 立即完成（核心功能）
1. 执行完整数据库迁移 SQL
2. 创建所有缺失的实体类
3. 实现 project_token 生成和修复逻辑
4. 实现项目专属 API 路由和拦截器
5. 实现核心的 10 个开放 API

### P1 - 尽快完成（重要功能）
6. 创建所有 Mapper 和 Service
7. 实现 API 签名验证
8. 创建管理后台的套餐、API Key 管理页面
9. 实现发卡订单防重复逻辑

### P2 - 后续优化（完善功能）
10. 实现 WebHook 功能
11. 创建剩余前端页面
12. 清理原项目品牌信息
13. 编写接入文档和 SDK 示例

---

## 关键技术要点

### 1. 7位随机码生成
```java
public String generate7DigitToken() {
    String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    SecureRandom random = new SecureRandom();
    StringBuilder token = new StringBuilder(7);
    for (int i = 0; i < 7; i++) {
        token.append(chars.charAt(random.nextInt(chars.length())));
    }
    return token.toString();
}
```

### 2. 项目专属路由设计
```
/api/p/{project_token}/cards/generate
/api/p/{project_token}/cards/verify
/api/p/{project_token}/cards/consume
/api/p/{project_token}/cards/status
/api/p/{project_token}/cards/redeem
/api/p/{project_token}/cards/unbind-device
/api/p/{project_token}/cards/refund-consume
/api/p/{project_token}/entitlements/status
/api/p/{project_token}/entitlements/consume
/api/p/{project_token}/entitlements/refund-consume
```

### 3. API 签名算法
```
sign = sha256(api_key + timestamp + nonce + request_body + api_secret)

请求 Header:
X-API-KEY: api_key
X-TIMESTAMP: timestamp
X-NONCE: nonce
X-SIGN: sign
```

### 4. 项目隔离原则
- 所有开放 API 查询必须带 project_id 条件
- A 项目的 API Key 不能操作 B 项目的数据
- URL 中的 project_token 必须与 API Key 的 project_id 一致

---

## 下一步行动

由于工作量较大，建议分阶段实施：

**阶段1：核心后端（2-3小时）**
- 完成所有实体类、Mapper、Service
- 实现项目专属 API 路由
- 实现 10 个核心开放 API

**阶段2：前端界面（2-3小时）**
- 创建所有管理后台页面
- 实现 API 文档自动生成

**阶段3：优化完善（1-2小时）**
- WebHook 功能
- 品牌信息清理
- 文档和示例

总计：5-8小时可完成全部工作

---

## 立即可执行的步骤

1. 执行数据库迁移：
   ```bash
   mysql -u root -p kami < databaes/complete_migration.sql
   ```

2. 验证项目表和默认项目：
   ```sql
   SELECT * FROM projects WHERE project_code = 'default';
   ```

3. 如果 project_token = 'DEFAULT'，需要修复为7位随机码

4. 继续生成剩余的后端文件...

---

*本指南持续更新中*
