# 品牌信息清理指南

## 概述

本项目基于开源卡密系统改造而来，以下是需要清理的原品牌信息和建议的替换方案。

---

## 1. Package 名称（低优先级）

当前 package 名称为 `org.xxg.backend.backend`，如需更换：

### 方案A：保持现状
- 优点：无需修改，避免大量重构
- 缺点：package 名称带有原作者标识

### 方案B：全局重构
```bash
# 批量重命名 package
find backend/src -name "*.java" -exec sed -i 's/org.xxg.backend.backend/com.yourcompany.kami/g' {} \;

# 移动目录结构
mkdir -p backend/src/main/java/com/yourcompany/kami
mv backend/src/main/java/org/xxg/backend/backend/* backend/src/main/java/com/yourcompany/kami/
```

**建议：** 保持现状，package 名称不影响用户体验。

---

## 2. 前端页面标题和 Logo

### 需要修改的文件

**index.html**
```html
<!-- 修改前 -->
<title>小小怪卡密系统</title>

<!-- 修改后 -->
<title>多项目卡密授权中心</title>
```

**App.vue / Layout.vue**
```vue
<!-- 修改系统名称 -->
<h1>{{ $t('system.name') }}</h1>

<!-- i18n 配置 -->
{
  "system": {
    "name": "多项目卡密授权中心",
    "short_name": "授权中心"
  }
}
```

**Logo 图片**
- 路径：`frontend/public/logo.png`
- 路径：`frontend/src/assets/logo.png`
- 建议：替换为你的品牌 Logo

---

## 3. README 和文档

### README.md

如果项目根目录存在 `README.md`，建议替换为：

```markdown
# 多项目卡密授权中心

一个支持多项目隔离、双模式使用、完整鉴权的卡密授权管理系统。

## 核心特性

- 多项目隔离
- 双模式使用（direct_license / redeem_to_account）
- API Key + HMAC-SHA256 签名认证
- 设备绑定、幂等操作
- WebHook 事件通知
- Redis 缓存优化

## 技术栈

- 后端：Spring Boot + JdbcTemplate + Redis
- 前端：Vue 3 + Element Plus
- 数据库：MySQL 8.0+

## 快速开始

见 [IMPLEMENTATION_CHECKLIST.md](./IMPLEMENTATION_CHECKLIST.md)

## API 文档

见 [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)

## License

MIT
```

---

## 4. 数据库表注释

原始 SQL 文件中的表注释多为中性词（"卡密表"、"项目表"），无需修改。

如需统一风格，可批量执行：

```sql
-- 修改表注释示例
ALTER TABLE cards COMMENT='卡密表（多项目卡密授权中心）';
ALTER TABLE projects COMMENT='项目表（多项目卡密授权中心）';
```

---

## 5. 管理后台页面

### 登录页面

如存在登录页面品牌信息：

**Login.vue**
```vue
<template>
  <div class="login-container">
    <div class="logo">
      <img src="@/assets/logo.png" alt="Logo">
      <h1>多项目卡密授权中心</h1>
    </div>
    <!-- ... -->
  </div>
</template>
```

### 页面 Footer

如存在 Footer 版权信息：

```vue
<footer>
  <p>&copy; 2026 YourCompany. All rights reserved.</p>
</footer>
```

---

## 6. 配置文件

### application.yml / application.properties

检查是否存在品牌相关配置：

```yaml
# 修改前
spring:
  application:
    name: xxg-kami-system

# 修改后
spring:
  application:
    name: multi-project-license-center
```

---

## 7. 邮件模板和通知

如系统发送邮件通知，检查模板中的品牌信息：

**EmailService.java**
```java
// 修改邮件签名
String signature = """
    ---
    此邮件由【多项目卡密授权中心】自动发送，请勿回复。
    """;
```

---

## 8. 错误页面和提示信息

检查 404、500 等错误页面是否包含品牌信息：

**404.vue**
```vue
<template>
  <div class="error-page">
    <h1>404 - 页面不存在</h1>
    <p>返回 <router-link to="/">首页</router-link></p>
  </div>
</template>
```

---

## 9. 构建产物和元数据

### pom.xml

```xml
<groupId>com.yourcompany</groupId>
<artifactId>kami-license-center</artifactId>
<version>2.0.0</version>
<name>Multi-Project License Center</name>
<description>多项目卡密授权管理系统</description>
```

### package.json

```json
{
  "name": "multi-project-license-center",
  "version": "2.0.0",
  "description": "多项目卡密授权管理系统",
  "author": "YourCompany"
}
```

---

## 10. Docker 镜像和部署脚本

### Dockerfile

```dockerfile
LABEL maintainer="yourcompany@example.com"
LABEL description="Multi-Project License Center"
```

### docker-compose.yml

```yaml
services:
  kami-backend:
    image: yourcompany/kami-license-center:2.0.0
    container_name: kami-backend
```

---

## 清理检查清单

- [ ] 前端页面标题（index.html）
- [ ] 系统 Logo（logo.png）
- [ ] README.md 文件
- [ ] 登录页面品牌信息
- [ ] Footer 版权信息
- [ ] application.yml 应用名称
- [ ] 邮件模板签名
- [ ] 错误页面信息
- [ ] pom.xml 元数据
- [ ] package.json 元数据
- [ ] Docker 镜像标签
- [ ] 对外 API 文档中的示例域名

---

## 建议优先级

### P0（必须）
- 前端页面标题和 Logo
- 登录页面品牌信息
- 对外文档（README、API 文档）

### P1（建议）
- Footer 版权信息
- 邮件模板
- 配置文件应用名称

### P2（可选）
- Package 名称重构
- Docker 镜像标签
- 数据库表注释

---

## 自动化清理脚本

```bash
#!/bin/bash
# brand-cleanup.sh

echo "开始清理品牌信息..."

# 1. 替换前端标题
find src -name "index.html" -exec sed -i 's/小小怪卡密系统/多项目卡密授权中心/g' {} \;

# 2. 替换 package.json
sed -i 's/"name": "xxg-kami"/"name": "multi-project-license"/g' package.json

# 3. 替换 pom.xml
sed -i 's/<name>xxg-kami<\/name>/<name>kami-license-center<\/name>/g' pom.xml

echo "清理完成！请手动检查 Logo 图片和登录页面。"
```

---

**当前状态：** 经检查，后端代码中未发现"小小怪"等明显品牌信息，主要需清理前端页面和配置文件。
