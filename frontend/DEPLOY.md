# 卡密授权中心 - 前端部署指南

## 快速开始

### 1. 安装依赖

```bash
cd frontend
npm install
```

如果使用 yarn 或 pnpm：

```bash
yarn install
# 或
pnpm install
```

### 2. 开发环境运行

```bash
npm run dev
```

访问：http://localhost:3000

### 3. 生产环境构建

```bash
npm run build
```

构建产物在 `dist/` 目录。

## 环境配置

创建 `.env` 文件配置环境变量：

```bash
# API Base URL
VITE_API_BASE_URL=http://localhost:8080

# 其他配置...
```

不同环境使用不同文件：
- `.env.development` - 开发环境
- `.env.production` - 生产环境

## 后端连接配置

### 开发环境

开发环境使用 Vite 代理转发，已在 `vite.config.js` 中配置：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    },
    '/admin': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 生产环境

生产环境需要配置 Nginx 或其他 Web 服务器反向代理。

#### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    
    root /var/www/kami-frontend/dist;
    index index.html;

    # 前端路由（SPA）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 代理API请求到后端
    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /admin {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

#### Apache 配置示例

```apache
<VirtualHost *:80>
    ServerName yourdomain.com
    DocumentRoot /var/www/kami-frontend/dist

    <Directory /var/www/kami-frontend/dist>
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
        
        # SPA路由支持
        RewriteEngine On
        RewriteBase /
        RewriteRule ^index\.html$ - [L]
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </Directory>

    # 代理API请求
    ProxyPass /api http://localhost:8080/api
    ProxyPassReverse /api http://localhost:8080/api
    
    ProxyPass /admin http://localhost:8080/admin
    ProxyPassReverse /admin http://localhost:8080/admin
</VirtualHost>
```

## Docker 部署

### Dockerfile

```dockerfile
# 构建阶段
FROM node:18-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# 生产阶段
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### nginx.conf

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /admin {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - kami-network

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/kami?useSSL=false
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=yourpassword
    depends_on:
      - db
    networks:
      - kami-network

  db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=yourpassword
      - MYSQL_DATABASE=kami
    volumes:
      - mysql-data:/var/lib/mysql
      - ./database/complete_migration.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - kami-network

networks:
  kami-network:
    driver: bridge

volumes:
  mysql-data:
```

### 构建和运行

```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 常见问题

### 1. API请求404

检查：
- 后端是否正常运行在 8080 端口
- Nginx/Apache 代理配置是否正确
- 浏览器开发者工具 Network 标签查看实际请求URL

### 2. 路由刷新404

确保配置了 SPA 路由重写规则（`try_files` 或 `RewriteRule`）。

### 3. 跨域问题

生产环境通过反向代理解决，确保前后端在同一域名下。

### 4. 玻璃拟态效果不显示

检查浏览器是否支持 `backdrop-filter` CSS属性。旧版浏览器可能不支持。

### 5. ECharts图表不显示

检查：
- 容器是否有明确的宽高
- 数据格式是否正确
- 浏览器控制台是否有错误

## 性能优化建议

1. **启用 Gzip 压缩**

```nginx
gzip on;
gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
gzip_vary on;
```

2. **CDN 加速**

将静态资源上传到 CDN，修改 `vite.config.js`：

```javascript
build: {
  rollupOptions: {
    output: {
      assetFileNames: 'assets/[name].[hash][extname]'
    }
  }
}
```

3. **懒加载路由**

路由配置中已使用动态导入：

```javascript
component: () => import('@/views/Dashboard.vue')
```

4. **图片优化**

使用 WebP 格式，压缩图片大小。

## 监控和日志

### 前端错误监控

集成 Sentry 或其他监控服务：

```javascript
// main.js
import * as Sentry from "@sentry/vue"

Sentry.init({
  app,
  dsn: "YOUR_SENTRY_DSN",
  integrations: [
    new Sentry.BrowserTracing({
      routingInstrumentation: Sentry.vueRouterInstrumentation(router),
    }),
  ],
  tracesSampleRate: 1.0,
})
```

### Nginx 访问日志

```nginx
access_log /var/log/nginx/kami-access.log combined;
error_log /var/log/nginx/kami-error.log warn;
```

## 安全建议

1. **HTTPS 部署**

使用 Let's Encrypt 免费证书：

```bash
sudo certbot --nginx -d yourdomain.com
```

2. **安全头配置**

```nginx
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "no-referrer-when-downgrade" always;
add_header Content-Security-Policy "default-src 'self' http: https: data: blob: 'unsafe-inline'" always;
```

3. **敏感信息保护**

不要在前端代码中硬编码 API Secret、密钥等敏感信息。

## 维护

### 更新依赖

```bash
# 检查过期依赖
npm outdated

# 更新依赖
npm update

# 更新 package.json
npx npm-check-updates -u
npm install
```

### 备份

定期备份：
- 构建产物 `dist/`
- 配置文件
- Nginx/Apache 配置

## 支持

如遇到问题，请检查：
1. 浏览器控制台错误信息
2. Network 标签查看请求详情
3. 后端日志
4. Web 服务器日志

---

部署完成后，访问配置的域名即可使用卡密授权中心！
