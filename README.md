# 云逸卡密授权中心

私有化多项目卡密授权中心，用于项目授权、卡密发放、接口密钥和发卡记录管理。

## 本地开发

### 前端

```bash
npm install
npm run dev
```

### 后端

```bash
cd backend
./mvnw -DskipTests package
```

Windows PowerShell：

```powershell
cd backend
.\mvnw.cmd -DskipTests package
```

## 部署流程

1. 初始化数据库结构。
2. 启动后端服务。
3. 部署前端构建产物。
4. 登录管理后台配置基础信息、项目和 API 密钥。
5. 在“发卡记录”中追踪外部系统同步的发卡结果。

## 模块

- 项目管理
- 卡密管理
- 套餐模板
- API 管理
- 发卡记录
- 用户管理
- 系统设置