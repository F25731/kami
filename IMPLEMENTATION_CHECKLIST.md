# 多项目卡密授权中心 - 实施清单

## ✅ 已完成项目

### 后端核心代码（100%）

#### 数据层 - Mapper（7个）
- ✅ CardPackageMapper.java
- ✅ ProjectApiKeyMapper.java
- ✅ UserEntitlementMapper.java
- ✅ CardIssueOrderMapper.java
- ✅ ApiCallLogMapper.java
- ✅ ConsumeLogMapper.java
- ✅ DeviceBindLogMapper.java

#### 业务层 - Service（6个）
- ✅ CardPackageService.java
- ✅ ProjectApiKeyService.java
- ✅ UserEntitlementService.java
- ✅ OpenApiAuthService.java
- ✅ ApiCallLogService.java
- ✅ CardService.java（新增7个OpenAPI方法）

#### 控制层 - Controller（3个）
- ✅ ProjectOpenApiController.java（10个核心API）
- ✅ CardPackageController.java
- ✅ ProjectApiKeyController.java

#### 实体类（4个）
- ✅ CardPackage.java（已有）
- ✅ ProjectApiKey.java（已有）
- ✅ UserEntitlement.java（已有）
- ✅ CardIssueOrder.java
- ✅ ApiCallLog.java
- ✅ DeviceBindLog.java
- ✅ Card.java（新增9个字段）

#### 配置与工具
- ✅ WebConfig.java（预留拦截器注册位）
- ✅ AsyncConfig.java（启用@Async支持）
- ✅ ProjectTokenFixRunner.java（启动时修复默认项目token）
- ✅ TokenGenerator.java（已有）
- ✅ SignatureUtil.java（已有）
- ✅ ProjectService.java（补充getById别名）
- ✅ CardMapper.java（更新支持新字段）

#### 数据库
- ✅ complete_migration.sql（完整迁移脚本）

#### 文档
- ✅ API_DOCUMENTATION.md（完整API使用文档）
- ✅ IMPLEMENTATION_GUIDE.md（实施指南）

---

## 📋 待执行步骤

### 1. 数据库初始化（必须）

```bash
# 进入数据库
mysql -u root -p kami

# 执行迁移脚本
source d:/BaiduNetdiskDownload/卡密系统/databaes/complete_migration.sql
```

**检查点：**
- 验证所有表已创建
- 检查 `projects` 表中是否存在默认项目
- 确认 `cards` 表新字段已添加

### 2. 项目启动验证

```bash
# 启动后端
cd backend
mvn spring-boot:run

# 观察启动日志
# 应看到：[ProjectTokenFix] Default project token fixed: <7位随机码>
```

### 3. 创建第一个 API Key

```bash
# 使用管理后台或直接调用接口
POST http://localhost:8080/admin/projects/1/api-keys
Content-Type: application/json

{
  "key_name": "测试密钥",
  "permissions": "[\"cards:verify\",\"cards:consume\",\"cards:status\"]",
  "environment": "production"
}

# 响应中会包含 api_secret，请立即保存！
```

### 4. 测试开放 API

使用 [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) 中的 Python 示例：

```python
# 1. 生成卡密
result = call_api("/cards/generate", {
    "quantity": 5,
    "card_type": "count",
    "count_value": 100
})

# 2. 验证卡密
result = call_api("/cards/verify", {
    "card_key": "从上一步获取的卡密"
})

# 3. 消费卡密
result = call_api("/cards/consume", {
    "card_key": "...",
    "biz_id": "test_order_001",
    "amount": 1
})
```

---

## 🔧 可选优化项（P3）

### 前端页面（未实现）

需要创建以下 Vue 页面：

1. **CardPackageManagePage.vue** - 套餐管理
   - 列表、新建、编辑、启用/禁用
   - 关联项目选择
   - 价格配置

2. **ProjectApiKeyManagePage.vue** - API Key 管理
   - 列表、创建、轮换、删除
   - 权限配置（多选）
   - IP 白名单设置
   - 使用统计

3. **ProjectApiDocPage.vue** - API 文档展示
   - 自动生成接口文档
   - 显示当前项目的 project_token
   - 在线调试工具

4. **CardIssueOrdersPage.vue** - 发卡订单
   - 订单列表
   - 过滤：时间范围、状态、API Key

5. **UserEntitlementsPage.vue** - 用户权益管理
   - 权益列表
   - 搜索用户
   - 权益详情（来源卡密、剩余次数、到期时间）

6. **DeviceBindManagePage.vue** - 设备绑定管理
   - 绑定日志
   - 批量解绑

7. **ApiCallLogsPage.vue** - API 调用日志
   - 实时日志流
   - 过滤：项目、API Key、端点、状态码
   - 统计图表

### WebHook 功能（未实现）

在业务关键节点触发 WebHook：

```java
// CardService 中添加
private void triggerWebhook(Long projectId, String event, Map<String, Object> data) {
    if (webhookService != null) {
        webhookService.trigger(projectId, event, data);
    }
}

// 触发时机：
// - card.consumed
// - card.redeemed
// - card.generated
// - entitlement.consumed
// - device.bound
// - device.unbound
```

### 品牌信息清理（未实现）

需要清理原项目中的品牌信息：

**后端：**
```bash
grep -r "小小怪" backend/src/
grep -r "xxg" backend/src/ | grep -i comment
```

**前端：**
```bash
grep -r "小小怪" src/
grep -r "xxg" src/components/
```

**数据库：**
```sql
-- 检查表注释和字段注释中的品牌信息
SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA='kami';
```

### 性能优化

1. **Redis 缓存优化**
   - API Key 查询缓存（TTL: 5分钟）
   - 项目配置缓存
   - 用户权益缓存

2. **数据库索引优化**
   - 分析慢查询日志
   - 为高频查询添加组合索引

3. **异步处理**
   - WebHook 异步触发
   - 日志异步批量写入

### 监控与告警

1. **Prometheus 指标**
   - API 调用量
   - 签名验证失败率
   - 卡密消费 QPS
   - 响应延迟分布

2. **告警规则**
   - 签名失败率 > 5%（可能遭受攻击）
   - API 错误率 > 10%
   - 响应延迟 P99 > 1000ms

---

## 🎯 生产环境部署检查清单

### 安全配置

- [ ] 修改默认管理员密码
- [ ] 配置 API Key 过期策略
- [ ] 启用 IP 白名单
- [ ] 配置 HTTPS 证书
- [ ] 设置 Redis 密码
- [ ] 数据库用户权限最小化

### 性能配置

- [ ] 调整 JVM 堆内存（-Xmx4g -Xms4g）
- [ ] 配置数据库连接池（HikariCP）
- [ ] Redis 持久化策略（AOF）
- [ ] Nginx 反向代理 + 负载均衡

### 监控配置

- [ ] 配置日志收集（ELK / Loki）
- [ ] 接入监控平台（Prometheus + Grafana）
- [ ] 配置告警通知（钉钉/企业微信）
- [ ] 定期数据库备份（每日凌晨）

### 文档交付

- [ ] API 使用文档交付客户
- [ ] 管理后台操作手册
- [ ] 故障排查手册
- [ ] SDK 示例代码（Python/Java/PHP）

---

## 📊 核心技术特性总结

### 项目隔离
- 每个项目独立的 7 位访问码（project_token）
- 所有查询强制带 project_id 条件
- API Key 与项目一对一绑定

### 签名验证
- HMAC-SHA256(apiKey + timestamp + nonce + body + secret)
- 时间戳防重放（±5分钟窗口）
- Nonce 去重（Redis TTL = 15分钟）

### 幂等性
- 卡密消费：biz_id 唯一索引
- 发卡订单：(project_id, order_no) 唯一索引
- 权益消费：(project_id, entitlement_id, biz_id) 唯一索引

### 并发控制
- 权益扣减：乐观锁（WHERE remaining >= N）
- 卡密消费：Redisson 分布式锁（已有）
- API 调用限流：基于 Redis 的滑动窗口（待实现）

### 审计日志
- API 调用日志（请求体、响应体、耗时、IP）
- 卡密消费日志（扣减前后次数、业务场景）
- 设备绑定日志（绑定/解绑操作、原因）
- 权益兑换日志（来源卡密、兑换时间）

---

## 🚀 下一步建议

### 第一优先级（本周内）
1. 执行数据库迁移
2. 启动项目验证所有接口正常
3. 创建测试项目和 API Key
4. 编写自动化测试用例

### 第二优先级（2周内）
1. 实现核心前端页面（套餐管理、API Key 管理）
2. 编写客户端 SDK（Python/Java）
3. 部署到测试环境，邀请内部用户体验

### 第三优先级（1个月内）
1. 实现 WebHook 功能
2. 接入监控告警系统
3. 性能压测与优化
4. 生产环境部署

---

**当前状态：后端核心功能已全部完成，可直接进行数据库迁移和接口测试。**
