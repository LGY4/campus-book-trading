# Campus Book Trading Platform

校园二手书流转交易平台 — 基于 Spring Boot + Vue 的全栈二手书交易平台。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + MySQL 8.4 |
| 前端 | Vue 2.7 + Element UI 2.15 + Axios |
| 认证 | JWT + BCrypt |
| 通信 | WebSocket (STOMP) |
| 部署 | Docker + Nginx |

## 快速启动 (Docker)

### 前置条件

- [Docker](https://docs.docker.com/get-docker/) 20.10+
- [Docker Compose](https://docs.docker.com/compose/install/) v2+

### 一键启动

```bash
git clone https://github.com/LGY4/campus-book-trading.git
cd campus-book-trading
docker-compose up -d --build
```

等待构建完成后（首次约 5-10 分钟），访问：

- 前端页面: http://localhost
- 后端 API: http://localhost:8088
- 数据库: localhost:3306

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |

### 常用命令

```bash
# 查看日志
docker-compose logs -f backend

# 停止服务
docker-compose down

# 重建并启动
docker-compose up -d --build

# 清除数据（包括数据库）
docker-compose down -v
```

## 云端部署 (Railway)

[Railway](https://railway.app) 提供免费额度（$5/月），支持 Docker 部署，自带 MySQL 插件。

### 第一步：注册并创建项目

1. 打开 https://railway.app，用 GitHub 账号登录
2. 点击 **New Project** → **Provision MySQL**，创建数据库
3. 记录 MySQL 插件的连接信息（自动生在 Variables 中）

### 第二步：部署后端

1. 点击 **+ New** → **GitHub Repo** → 选择 `campus-book-trading`
2. Railway 检测到 `railway.json`，使用 `backend/Dockerfile` 构建
3. 进入该服务的 **Variables**，添加：

| 变量 | 值 |
|------|------|
| `SERVER_PORT` | `${{PORT}}` |
| `DB_URL` | `${{MYSQL_URL}}`（引用 MySQL 插件变量） |
| `DB_USERNAME` | `${{MYSQL_USER}}` |
| `DB_PASSWORD` | `${{MYSQL_PASSWORD}}` |
| `UPLOAD_PATH` | `/app/uploads/` |

4. 等待构建完成，服务变为 Active

### 第三步：部署前端

1. 再次点击 **+ New** → **GitHub Repo** → 同一个仓库
2. 进入服务 **Settings**：
   - **Root Directory** 设为 `frontend`
   - **Dockerfile Path** 设为 `Dockerfile`
3. 在 **Variables** 中添加：

| 变量 | 值 |
|------|------|
| `BACKEND_URL` | backend 服务的内网地址，格式 `backend.railway.internal:PORT`（在 backend 服务 Settings 中查看） |

4. 等待构建完成，前端会获得公网 URL

### 第四步：验证

1. 访问前端公网 URL
2. 用 `admin` / `admin123` 登录
3. 测试浏览书籍、发布、聊天等功能

### 注意事项

- 文件上传存储在容器内，重新部署会丢失（演示用途可接受）
- 免费额度 $5/月，轻度使用足够
- MySQL 数据由 Railway 持久化管理
- 首次构建较慢（下载 Maven 依赖），后续有缓存

## 本地开发

### 后端

```bash
cd backend
./mvnw spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run serve
```

前端开发服务器会自动读取后端端口并配置代理。

## 项目结构

```
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/booktrading/
│   │   ├── controller/         # REST API 控制器
│   │   ├── service/            # 业务逻辑层
│   │   ├── mapper/             # MyBatis-Plus 数据访问层
│   │   ├── entity/             # 数据库实体
│   │   ├── config/             # 配置类
│   │   └── interceptor/        # 拦截器 (JWT认证、操作日志)
│   ├── src/main/resources/
│   │   ├── application.yml     # 应用配置
│   │   └── sql/init.sql        # 数据库初始化脚本
│   └── Dockerfile
├── frontend/                   # Vue 前端
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   ├── api/                # API 调用封装
│   │   ├── router/             # 路由配置
│   │   ├── store/              # Vuex 状态管理
│   │   └── utils/              # 工具函数
│   ├── nginx.conf              # Nginx 配置 (Docker 生产环境)
│   └── Dockerfile
├── docker-compose.yml          # Docker 编排
└── README.md
```

## 功能模块

- **用户系统**: 注册、登录、个人资料、头像上传、信誉分
- **书籍管理**: 发布、编辑、搜索、分类浏览、图片上传
- **交易流程**: 下单、支付(模拟)、发货、收货、退款、纠纷处理
- **评价系统**: 星级评分、文字评论、图片评论、追评
- **书籍交换**: 以书换书请求、接受/拒绝、完成交换
- **即时聊天**: WebSocket 实时消息、图片发送、会话管理
- **管理后台**: 用户管理、书籍审核、订单管理、评论管理、轮播管理、操作日志、数据统计

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| SERVER_PORT | 8088 | 后端端口 |
| DB_URL | jdbc:mysql://localhost:3306/book_trading | 数据库连接 |
| DB_USERNAME | root | 数据库用户名 |
| DB_PASSWORD | (空) | 数据库密码 |
| UPLOAD_PATH | ./uploads/ | 文件上传路径 |
