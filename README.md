# GamingBar

GamingBar 是一个大学生游戏约玩平台示例项目，当前版本基于 `API文档_v2.4.md` 实现了登录、用户资料、游戏字典、房间管理和房间聊天等核心能力。

## 项目介绍

本项目包含两个子工程：

- `backend`：提供 RESTful API，负责鉴权、业务规则、房间状态流转、失效清理和数据访问
- `front`：提供联调界面，覆盖短信登录、资料修改、游戏列表、创建房间、房间列表、房间详情、我的房间和聊天

当前实现重点：

- 统一返回结构：`code`、`message`、`data`
- JWT Bearer Token 鉴权
- 房间失效判定与同步清理
- 一人一房业务约束
- 房主退出等同解散房间
- 聊天消息发送与历史查询

## 技术栈

### 后端

- Java 17
- Spring Boot 3
- MyBatis
- Flyway
- H2 Database
- JJWT
- JUnit 5 + MockMvc

### 前端

- React 18
- TypeScript
- Vite
- React Router
- Axios

## 目录结构

```text
Gaming-Bar/
├─ backend/                    后端工程
│  ├─ pom.xml
│  ├─ src/main/java/com/gamingbar
│  ├─ src/main/resources/application.yml
│  └─ src/main/resources/db/migration
├─ front/                      前端工程
│  ├─ package.json
│  ├─ src/
│  └─ vite.config.ts
└─ API文档_v2.4.md             接口文档
```

## 环境要求

- Java 17
- Maven 3.9+
- Node.js 20+
- npm 10+

## 后端启动

### 1. 进入后端目录

```powershell
cd D:\Gaming-Bar\backend
```

### 2. 启动后端服务

```powershell
mvn spring-boot:run
```

默认启动地址：

- 服务地址：`http://localhost:8080`
- H2 控制台：`http://localhost:8080/h2-console`

默认数据库连接：

- JDBC URL：`jdbc:h2:mem:gamingbar`
- 用户名：`sa`
- 密码：空

### 3. 后端测试

```powershell
mvn test
```

## 前端启动

### 1. 进入前端目录

```powershell
cd D:\Gaming-Bar\front
```

### 2. 安装依赖

```powershell
npm install
```

### 3. 启动前端开发服务器

```powershell
npm run dev
```

默认访问地址：

- 前端地址：`http://localhost:5173`

前端已在 `vite.config.ts` 中配置代理：

- `/api -> http://localhost:8080`

### 4. 前端构建

```powershell
npm run build
```

## 数据库说明

项目当前默认使用 **H2 内存数据库**，并通过 Flyway 在后端启动时自动创建表结构和初始化基础数据。

已完成的数据库内容：

- `t_user`
- `t_game`
- `t_sms_code`
- `t_room`
- `t_room_user`
- `t_message`
- 基础游戏字典初始化数据 5 条

对应脚本：

- [V1__init_schema.sql](/D:/Gaming-Bar/backend/src/main/resources/db/migration/V1__init_schema.sql)
- [V2__seed_games.sql](/D:/Gaming-Bar/backend/src/main/resources/db/migration/V2__seed_games.sql)

注意事项：

- 当前数据库可直接用于本地开发、接口测试和联调
- 当前数据库是内存库，后端进程停止后数据会丢失
- 如果要长期保存数据或正式部署，需要改成 MySQL 等持久化数据库
- 当前表结构和接口语义按文档设计，迁移到 MySQL 时主要需要修改 `application.yml` 的数据源配置

## 已实现接口范围

- `POST /api/auth/sms/send`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `PUT /api/users/profile`
- `GET /api/games`
- `POST /api/rooms`
- `GET /api/rooms`
- `GET /api/rooms/{roomId}`
- `POST /api/rooms/{roomId}/join`
- `POST /api/rooms/{roomId}/leave`
- `DELETE /api/rooms/{roomId}`
- `GET /api/rooms/my`
- `POST /api/rooms/{roomId}/messages`
- `GET /api/rooms/{roomId}/messages`

## 联调建议

推荐启动顺序：

1. 启动后端 `mvn spring-boot:run`
2. 确认 Flyway 已自动建表并写入游戏基础数据
3. 启动前端 `npm run dev`
4. 打开前端页面，通过短信发送和登录流程开始联调

## 当前验证结果

已通过的验证：

- 后端测试：`mvn test`
- 前端构建：`npm run build`

## 参考文档

- 接口文档：[API文档_v2.4.md](/D:/Gaming-Bar/API文档_v2.4.md)
