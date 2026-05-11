# 算力租赁平台后端

Spring Boot 3.2.x 后端工程骨架，按《算力租赁平台_正式开发版_v1.1.md》初始化。

## 技术栈

- JDK 17
- Spring Boot 3.2.x
- Spring Security 6 + JWT
- MyBatis-Plus
- MySQL 8
- Redis
- RabbitMQ
- Spring Scheduler + Redis Lock
- WebSocket
- Knife4j / springdoc-openapi
- Maven

## 启动前配置

后端只保留两套 yml 配置：

- `src/main/resources/application.yml`：本地环境，默认激活 `local`。
- `src/main/resources/application-prod.yml`：生产环境，使用 `SPRING_PROFILES_ACTIVE=prod`。

本地开发直接改 `application.yml` 或通过环境变量覆盖数据库、Redis、RabbitMQ、JWT 密钥等配置。真实密钥不要提交到仓库。

生产环境使用 `prod` profile，并通过环境变量或部署平台密钥注入配置：

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://mysql.internal:3306/compute_rental
DB_USERNAME=compute_prod
DB_PASSWORD=<secret>
REDIS_HOST=redis.internal
REDIS_PASSWORD=<secret>
RABBITMQ_HOST=rabbitmq.internal
RABBITMQ_USERNAME=compute_prod
RABBITMQ_PASSWORD=<secret>
JWT_SECRET=<32-bytes-minimum-secret>
API_TOKEN_ENCRYPTION_SECRET=<32-bytes-minimum-secret>
MAIL_USERNAME=<mail-user>
MAIL_PASSWORD=<mail-password>
WEBSOCKET_ALLOWED_ORIGINS=https://admin.example.com,https://app.example.com
```

`prod` profile 会强制校验生产配置：禁止开发占位密钥、本机 DB/Redis/RabbitMQ、RabbitMQ `guest/guest`、空 Redis 密码、通配 WebSocket Origin，并默认关闭 Knife4j/springdoc。

定时任务配置分两类：`app.order.auto-pause-delay` 是自动暂停业务延迟；`app.scheduler.auto-pause-cron` 是扫描频率。默认自动暂停每秒扫描，并按 `autoPauseAt` 截断运行片段，测试加速不要写入配置文件，需要时通过环境变量临时覆盖。

默认开发库名：

```text
compute_rental
```

## 常用命令

```bash
mvn compile
mvn clean install -DskipTests
mvn spring-boot:run
```

如后续存在单元测试，可使用：

```bash
mvn test -Dtest=ClassName
```

## API 文档

默认开发环境启动后访问：

```text
http://localhost:8080/doc.html
http://localhost:8080/swagger-ui.html
```

生产环境 `prod` profile 默认关闭 API 文档入口。

## 包结构

```text
com.compute.rental
├── common      # 统一响应、错误码、异常、分页、工具类
├── config      # MyBatis-Plus、Redis、RabbitMQ、OpenAPI、时区等配置
├── security    # Spring Security 6 + JWT 基础结构
├── websocket   # WebSocket 基础结构
├── scheduler   # Scheduler + Redis Lock 基础结构
└── modules     # 业务模块：user、wallet、product、order、commission、system
```

## SQL 目录

后续建表脚本放在：

```text
src/main/resources/sql
```

当前基础脚本：

```text
src/main/resources/sql/schema.sql
src/main/resources/sql/init-data.sql
```

本地初始化示例：

```bash
mysql -h <host> -P <port> -u <user> -p < src/main/resources/sql/schema.sql
mysql -h <host> -P <port> -u <user> -p compute_rental < src/main/resources/sql/init-data.sql
```

`init-data.sql` 包含 dev 管理员、基础系统配置、三级佣金规则、租赁周期规则和后台菜单。生产环境上线前必须替换默认管理员密码哈希。
