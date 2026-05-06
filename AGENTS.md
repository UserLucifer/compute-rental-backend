# AGENTS.md

## 使命

用最小且正确的改动解决用户的请求。

优先级：
1. **正确性**：类型安全、Spring Bean 生命周期、线程安全。
2. **最小变更**：不要修改没有问题的内容。
3. **保持现有模式**：遵循已有目录结构。
4. **低维护成本**：简单优先于巧妙。
5. **清晰验证**：确保代码无错误并能正常编译。

---

## 默认工作模式

### 修改前：
- **先阅读相关文件。**
- **分析 Spring Context**：追踪 `@Component`、`@Service`、`@Mapper` 的注入方式。
- **追踪数据库流程**：理解 MyBatis-Plus 实体映射和 QueryWrapper 逻辑。
- **理解现有抽象**，再决定是否新增抽象。

### 修改代码时：
- **JDK 17 标准**：在合适的局部变量中使用 `var`，在合适的 DTO 中使用 `record`。
- **Spring Boot 3 约定**：遵循 **Spring Security 6 Lambda DSL** 和相关注解规范。
- **修复根因**，不要只修表象。
- **优先选择最小可行补丁。**
- **复用已有工具、辅助类和组件**，例如 Redis 锁工具、Result 包装类。

### 避免：
- 不必要的重命名或文件移动。
- 没有充分理由就引入新依赖。
- **不要尝试打开浏览器进行测试。** 专注于编译和逻辑验证。

---

## 简单性与框架规则

- **直接逻辑** 优先于间接封装。
- **MyBatis-Plus**：优先使用 LambdaQueryWrapper 进行类型安全的数据库操作。
- **资源管理**：确保 Redis key 设置合适的 TTL，Redis 锁必须在 `finally` 中释放。
- **安全性**：始终考虑改动对 JWT 认证和 Spring Security 过滤器的影响。

---

## 变更范围纪律

改动应紧密围绕当前任务。
- 除非正确性需要，不要修改无关文件。
- 不要把顺手清理混进同一次变更。
- 如果改动影响 API，确保同步更新 Knife4j/OpenAPI 注解。
- 如果任何后端 API 的 endpoint、请求参数、请求体、响应体、路径、方法、鉴权要求或 API 注解发生变化，必须在同一次变更中同步更新 `E:\业务开发\qianduan\后端api文档.md`。

---

## 验证（Backend-Fast 规则）

**验证顺序：**
1. **编译检查**：运行 `mvn compile`，确保没有语法或依赖错误。
2. **类型安全**：确保没有引入 unchecked conversion 或 raw type。
3. **定向测试**：如果相关逻辑已有单元测试，运行 `mvn test "-Dtest=..."`。
4. **禁止 UI 测试**：不要通过浏览器或交互式会话验证后端改动。

最终回复中必须报告：
- 改了什么以及为什么改。
- **确认 `mvn compile` 是否成功。**
- 剩余风险或未验证区域，例如 MQ 消息流。

---

## 沟通

保持简洁、具体、技术化。
- **先给计划**：对于非简单任务，先给出简短计划和可能触及的文件。
- **尽早暴露发现**：包括错误假设、隐藏约束或根因发现。

---

## 测试与构建命令（Maven）

主要安装命令：
`mvn clean install "-DskipTests"`

主要开发/运行命令：
`mvn spring-boot:run`

主要定向测试命令：
`mvn test "-Dtest=[file_path]"`

主要编译检查：
`mvn compile`

主要 lint/checkstyle：
`mvn checkstyle:check`（如适用）

---

## 最终质量标准

变更完成必须满足：
- **正确且类型安全。**
- **编译就绪，零错误。**
- **最小且易理解。**
- **与现有项目模式一致。**

---

## PowerShell 命令规则

本项目默认在 PowerShell 中执行命令。

执行 Maven 命令时，所有 `-D` 参数必须给整个参数加引号，避免 PowerShell 将参数拆分或误解析。尤其是参数中包含逗号、冒号、`#`、`=`、方括号等特殊字符时，必须加引号。

正确示例：

```powershell
mvn test "-Dtest=UserServiceTest,OrderServiceTest"
mvn test "-Dtest=UserServiceTest#shouldCreateUser"
mvn dependency:get "-Dartifact=com.example:demo:1.0.0"
mvn compile "-DskipTests"
```

错误示例：

```powershell
mvn test -Dtest=UserServiceTest,OrderServiceTest
mvn dependency:get -Dartifact=com.example:demo:1.0.0
```
