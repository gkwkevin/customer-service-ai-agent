# 客服 AI 智能问答系统

基于 Spring Boot 微服务架构的智能客服系统，集成 DeepSeek AI 模型提供智能问答能力，支持 WebSocket 实时通信。

## 项目架构

本项目采用多模块微服务架构，包含以下服务模块：

| 服务模块 | 端口 | 说明 |
|---------|------|------|
| gateway-service | 8080 | API 网关，统一入口 |
| chat-service | 8081 | 聊天服务，用户认证、WebSocket 通信 |
| ai-service | 8082 | AI 服务，DeepSeek 模型集成 |
| ticket-service | 8083 | 工单服务，问题跟踪管理 |

## 技术栈

- **后端框架**: Spring Boot 2.x, Spring Cloud Gateway
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **搜索引擎**: Elasticsearch
- **AI 模型**: DeepSeek
- **实时通信**: WebSocket
- **前端**: Thymeleaf, 原生 JavaScript/CSS
- **构建工具**: Maven

## 功能特性

### 用户端
- 与 AI 客服进行实时对话
- 智能问答，自动回复常见问题

### 客服端
- 人工客服接入处理
- 会话管理（待处理、进行中、已完成）
- 创建工单、转接会话

### 管理端
- 用户管理
- 系统配置

### 工单系统
- 工单创建与分配
- 问题跟踪与处理

## 快速开始

### 环境要求
- JDK 1.8+
- MySQL 8.0+
- Redis 6.0+
- Elasticsearch 7.x+

### 数据库初始化

执行 [doc/init.sql](doc/init.sql) 初始化数据库结构和初始数据。

### 配置文件

修改各服务的 `application.yml` 文件，配置数据库、Redis、Elasticsearch 等连接信息。

### 启动服务

```bash
# 1. 启动 Gateway 服务
cd gateway-service
mvn spring-boot:run

# 2. 启动 Chat 服务
cd chat-service
mvn spring-boot:run

# 3. 启动 AI 服务
cd ai-service
mvn spring-boot:run

# 4. 启动 Ticket 服务
cd ticket-service
mvn spring-boot:run
```

或者使用 Maven 打包后运行：

```bash
# 打包所有模块
mvn clean package

# 运行各服务
java -jar gateway-service/target/gateway-service-*.jar
java -jar chat-service/target/chat-service-*.jar
java -jar ai-service/target/ai-service-*.jar
java -jar ticket-service/target/ticket-service-*.jar
```

## 访问系统

| 入口 | 地址 | 说明 |
|-----|------|------|
| 用户聊天 | http://localhost:8081/ | 用户与 AI 对话 |
| 客服工作台 | http://localhost:8081/agent | 客服人员使用 |
| 管理后台 | http://localhost:8081/admin | 管理员使用 |
| 工单系统 | http://localhost:8083/ticket/list | 工单管理 |


## 项目结构
customer-service-ai-agent/
├── gateway-service/        # 网关服务
├── chat-service/           # 聊天服务（核心）
│   ├── src/main/java/      # Java 源码
│   └── src/main/resources/ # 配置文件、静态资源、页面模板
├── ai-service/             # AI 服务
├── ticket-service/         # 工单服务
├── doc/
│   └── init.sql            # 数据库初始化脚本
└── pom.xml                 # 父工程 Maven 配置

## 核心模块说明

### chat-service
- **Controller**: 页面路由、API 接口
- **Service**: 用户服务、会话服务、消息服务
- **WebSocket**: 实时消息推送
- **JWT**: 用户认证与授权

### ai-service
- **DeepSeekService**: 调用 DeepSeek AI 模型
- **Elasticsearch**: 知识库搜索

### ticket-service
- 工单 CRUD 操作
- 工单状态流转

## 开发计划

### 第一阶段：基础架构（已完成）
- [x] 基础微服务架构搭建
- [x] Spring Cloud Gateway 网关配置
- [x] 统一异常处理与返回格式
- [x] 用户认证与授权（JWT）
- [x] 角色权限控制（RBAC）

### 第二阶段：核心功能（已完成）
- [x] WebSocket 实时通信
- [x] 用户注册登录
- [x] AI 智能问答集成（DeepSeek）
- [x] RAG 检索增强生成（Elasticsearch）
- [x] Function Calling 函数调用（转人工）
- [x] 客服工作台
- [x] 会话管理（待处理、进行中、已完成）
- [x] 工单系统
- [x] 消息历史记录

### 第三阶段：AI 能力增强（进行中）
- [ ] 意图识别优化
- [ ] 多轮对话上下文保持
- [ ] 情感分析（判断用户情绪）
- [ ] 相似问题推荐

### 第四阶段：客服功能完善
- [ ] 客服在线状态管理
- [ ] 客服负载均衡分配
- [ ] 快捷回复（常用语）
- [ ] 会话标签/分类
- [ ] 会话满意度评价
- [ ] 客服工作量统计
- [ ] 实时排队提示

### 第五阶段：数据统计分析
- [ ] 会话量统计（日/周/月）
- [ ] 响应时长分析
- [ ] 解决率统计
- [ ] 热点问题分析
- [ ] 客服绩效报表
- [ ] 用户满意度统计
- [ ] 数据可视化大屏

## 贡献指南

欢迎提交 Issue 和 Pull Request 来改进项目。

## 许可证

MIT License
