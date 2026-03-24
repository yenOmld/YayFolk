# Travelate 后端服务运行指南

## 项目结构

```
travelate-backend/
├── src/
│   ├── main/java/com/travelate/backend/
│   │   ├── controller/       # 控制器
│   │   ├── service/          # 服务层
│   │   ├── repository/       # 数据访问层
│   │   ├── entity/           # 实体类
│   │   ├── dto/              # 数据传输对象
│   │   ├── config/           # 配置类
│   │   └── TravelateApplication.java  # 应用主类
│   └── main/resources/
│       └── application.properties  # 应用配置
└── pom.xml                   # Maven配置
```

## 环境要求

1. **JDK 17** 或更高版本
2. **Maven 3.8+**
3. **MySQL 8.0+**

## 数据库配置

1. 确保MySQL服务已启动
2. 已创建 `travelate` 数据库
3. 已创建 `user` 表（如果表不存在，应用启动时会自动创建）

## 运行步骤

### 1. 配置数据库连接

编辑 `src/main/resources/application.properties` 文件，修改数据库连接信息：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/travelate?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=123456  # 替换为你的MySQL密码
```

### 2. 编译和运行

#### 使用Maven

```bash
# 编译项目
mvn clean package

# 运行应用
mvn spring-boot:run
```

#### 使用IDE

1. 导入项目到IDE（IntelliJ IDEA 或 Eclipse）
2. 等待依赖下载完成
3. 运行 `TravelateApplication.java` 类

## 接口说明

### 1. 注册接口

**URL**: `POST http://localhost:8080/api/register`

**请求体**:
```json
{
  "username": "13800138000",  // 手机号或邮箱
  "verificationCode": "123456",  // 验证码
  "password": "Password123!",  // 密码
  "confirmPassword": "Password123!",  // 确认密码
  "nickname": "测试用户"  // 可选，昵称
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10000,
    "username": "13800138000",
    "phone": "13800138000",
    "email": null,
    "nickname": "测试用户",
    "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=13800138000",
    "status": 1,
    "lastLoginTime": null,
    "createTime": "2024-01-01T00:00:00",
    "updateTime": null
  }
}
```

### 2. 登录接口

**URL**: `POST http://localhost:8080/api/login`

**请求体**:
```json
{
  "username": "13800138000",  // 手机号/邮箱/用户名
  "password": "Password123!"  // 密码
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10000,
    "username": "13800138000",
    "phone": "13800138000",
    "email": null,
    "nickname": "测试用户",
    "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=13800138000",
    "status": 1,
    "lastLoginTime": "2024-01-01T00:00:00",
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00"
  }
}
```

### 3. 发送验证码接口

**URL**: `POST http://localhost:8080/api/send-code`

**请求参数**:
- `username`: 手机号或邮箱

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": "验证码已发送"
}
```

## 前端配置

前端已配置为连接到 `http://localhost:8080/api`，无需修改。

## 注意事项

1. 验证码功能目前是模拟实现，实际项目中需要集成短信或邮箱服务
2. 密码使用 BCrypt 加密存储
3. 支持国际手机号格式（如 +1234567890）
4. 支持邮箱格式验证
5. 密码规则：8-20位，包含字母、数字和特殊字符

## 常见问题

1. **数据库连接失败**
   - 检查MySQL服务是否运行
   - 检查数据库连接配置是否正确
   - 确保 `travelate` 数据库已创建

2. **端口冲突**
   - 默认使用 8080 端口，如果被占用，可以修改 `application.properties` 中的 `server.port`

3. **依赖下载失败**
   - 确保网络连接正常
   - 可以尝试更换Maven镜像源
