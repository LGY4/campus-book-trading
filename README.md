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
