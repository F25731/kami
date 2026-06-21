# 卡密授权中心 - 前端说明

## 项目结构

```
frontend/
├── index.html              # HTML入口
├── package.json            # 依赖配置
├── vite.config.js         # Vite构建配置
├── src/
│   ├── main.js            # 应用入口
│   ├── App.vue            # 根组件
│   ├── router/
│   │   └── index.js       # 路由配置
│   ├── stores/
│   │   └── project.js     # Pinia状态管理
│   ├── layouts/
│   │   └── MainLayout.vue # 主布局（含侧边栏、顶栏）
│   └── views/
│       ├── Dashboard.vue      # 数据看板
│       ├── ApiKeys.vue        # API密钥管理
│       ├── Cards.vue          # 卡密管理
│       ├── Packages.vue       # 套餐模板
│       ├── ApiDocs.vue        # API文档
│       ├── Orders.vue         # 发卡订单
│       ├── Entitlements.vue   # 用户权益
│       ├── Logs.vue           # 调用日志
│       └── Settings.vue       # 项目设置
```

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Vue Router** - 官方路由管理
- **Pinia** - 轻量级状态管理
- **Element Plus** - UI组件库
- **Axios** - HTTP客户端
- **ECharts** - 数据可视化图表
- **Vite** - 下一代前端构建工具
- **Sass** - CSS预处理器

## 安装和运行

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 开发环境运行

```bash
npm run dev
```

访问 http://localhost:3000

### 3. 生产环境构建

```bash
npm run build
```

构建产物输出到 `dist/` 目录

## 设计特点

### 玻璃拟态（Glassmorphism）设计

整个UI采用玻璃拟态设计风格：

- **半透明背景** - `rgba(255, 255, 255, 0.1)`
- **毛玻璃效果** - `backdrop-filter: blur(20px)`
- **柔和边框** - `border: 1px solid rgba(255, 255, 255, 0.2)`
- **动态渐变背景** - 15秒循环的渐变动画
- **阴影层次** - `box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1)`

### 多项目架构

- **左侧项目选择器** - 支持多项目切换
- **项目隔离** - 每个项目独立的数据和配置
- **localStorage持久化** - 记住当前选择的项目

### 响应式设计

- 自适应布局，支持不同屏幕尺寸
- Grid布局实现卡片自动排列
- 侧边栏可折叠优化小屏幕体验

## 页面功能

### 1. Dashboard（数据看板）

- 实时统计卡片：总卡密、API调用、成功率、延迟
- API调用趋势图表（24h/7d/30d）
- 响应延迟分布直方图
- 卡密使用情况进度条
- 最近发卡订单列表
- 快捷操作按钮

### 2. API密钥管理

- 密钥列表展示（名称、状态、环境、使用统计）
- 创建新密钥（权限配置、IP白名单、过期时间）
- 密钥轮换（生成新密钥，旧密钥立即失效）
- API Secret仅创建时显示一次
- 启用/禁用密钥开关
- 权限、IP白名单可折叠查看

### 3. 卡密管理

- 卡密列表（卡密、套餐、类型、剩余、状态、设备绑定）
- 搜索和筛选（状态、套餐）
- 批量生成卡密（选择套餐、数量、订单号）
- 查看卡密详情
- 解绑设备
- 删除卡密

### 4. 套餐模板

- 套餐卡片网格展示
- 创建套餐（名称、代码、类型、次数/天数、价格）
- 套餐代码自动生成
- 编辑套餐
- 删除套餐（已生成的卡密不受影响）

### 5. API文档

- 项目Token和Base URL展示
- 认证方式说明（HMAC-SHA256）
- 签名计算方法
- Python示例代码
- 10个API端点详细文档（参数、请求、响应示例）
- 错误码说明表

### 6. 发卡订单

- 订单列表（订单号、套餐、数量、来源、状态）
- 日期范围筛选
- 查看订单详情
- 显示订单中所有卡密
- 导出卡密为TXT文件

### 7. 用户权益

- 权益列表（用户ID、套餐、类型、总量、剩余、状态）
- 状态筛选
- 剩余量颜色提示（高/中/低）
- 查看权益详情
- 消耗记录时间线

### 8. 调用日志

- 实时统计卡片（今日调用、成功率、平均响应时间）
- 日志列表（时间、路径、方法、状态、响应时间、IP）
- 响应时间颜色分级（快/正常/慢）
- HTTP状态码颜色标识
- 查看详细日志（请求体、响应体、错误信息）
- 时间范围筛选

### 9. 项目设置

**基本设置**
- 项目信息编辑
- 项目Token重新生成
- 使用模式切换
- 项目启用/停用

**设备绑定**
- 启用设备绑定开关
- 绑定类型选择（机器码/Android ID/自定义）
- 允许换绑配置

**WebHook**
- 启用WebHook通知
- WebHook URL配置
- 签名密钥生成
- 通知事件选择
- 测试WebHook

**高级设置**
- 卡密前缀配置
- 卡密长度设置
- 默认过期时间
- 删除项目（危险操作）

## API集成

### Axios配置

- Base URL: `http://localhost:8080`
- 超时时间: 30秒
- 请求拦截器：自动添加Authorization token
- 响应拦截器：统一错误处理

### 代理配置

开发环境通过Vite代理转发请求到后端：

```javascript
proxy: {
  '/api': 'http://localhost:8080',
  '/admin': 'http://localhost:8080'
}
```

## 状态管理

使用Pinia管理全局状态：

- `projects` - 项目列表
- `currentProject` - 当前选中的项目
- `loadProjects()` - 加载项目列表
- `switchProject()` - 切换当前项目
- `createProject()` - 创建新项目

## 待实现功能

以下功能前端已完成UI，需要后端API支持：

1. 用户登录/注册系统
2. 项目成员管理和权限控制
3. 更详细的统计图表数据API
4. WebSocket实时推送
5. 导出Excel报表
6. 多语言国际化

## 浏览器兼容性

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

需要支持 CSS `backdrop-filter` 属性以获得玻璃拟态效果。

## 部署

### Nginx配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /var/www/kami-frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /admin {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 开发建议

1. **颜色主题** - 可在 `App.vue` 中修改渐变背景颜色
2. **组件复用** - 卡片、表格等可抽取为公共组件
3. **类型检查** - 建议引入 TypeScript 增强类型安全
4. **测试** - 建议添加 Vitest 单元测试
5. **性能优化** - 大列表可使用虚拟滚动（vue-virtual-scroller）

## 参考资源

- [Vue 3 文档](https://cn.vuejs.org/)
- [Element Plus 文档](https://element-plus.org/)
- [ECharts 文档](https://echarts.apache.org/)
- [Glassmorphism 设计](https://hype4.academy/tools/glassmorphism-generator)
