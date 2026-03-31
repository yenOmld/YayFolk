# YayFolk 项目接口文档

## 1. 接口概述

YayFolk 项目提供了一套完整的 RESTful API，用于支持前端应用的各项功能。接口覆盖了用户认证、内容管理、活动预订、消息通信等多个领域，采用标准的 HTTP 方法和 JSON 数据格式。

### 1.1 基础 URL

所有接口的基础 URL 为：`/api`（根据部署环境可能有所不同）

### 1.2 认证方式

项目使用 JWT (JSON Web Token) 进行身份认证，认证信息通过 HTTP 请求头 `Authorization` 传递：

```
Authorization: Bearer {token}
```

### 1.3 响应格式

所有接口返回统一的 JSON 格式：

```json
{
  "code": 200,           // 状态码，200 表示成功
  "message": "success",  // 消息
  "data": {}             // 数据
}
```

## 2. 接口分类

### 2.1 认证相关接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 登录 | /login | POST | 用户登录 |
| 验证码登录 | /login/code | POST | 使用验证码登录 |
| 注册 | /register | POST | 用户注册 |
| 发送验证码 | /send-code | POST | 发送验证码 |
| 重置密码 | /reset-password | POST | 重置密码 |

### 2.2 发现模块接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 获取发现帖子列表 | /discover/posts | GET | 获取发现页面的帖子列表 |
| 获取帖子详情 | /discover/posts/{postId} | GET | 获取单个帖子的详细信息 |
| 获取分享帖子详情 | /discover/share/{postId} | GET | 获取分享的帖子详情 |
| 翻译帖子 | /post/translate | GET | 翻译帖子内容 |
| 翻译评论 | /comment/translate | GET | 翻译评论内容 |
| 创建帖子 | /discover/posts | POST | 创建新帖子 |
| 收藏/取消收藏帖子 | /discover/posts/{postId}/collect | POST | 切换帖子收藏状态 |
| 举报帖子 | /discover/posts/{postId}/report | POST | 举报帖子 |
| 创建评论 | /discover/posts/{postId}/comments | POST | 为帖子创建评论 |
| 点赞/取消点赞评论 | /discover/comments/{commentId}/like | POST | 切换评论点赞状态 |
| 删除评论 | /discover/comments/{commentId} | DELETE | 删除评论 |
| 获取我的帖子 | /discover/my/posts | GET | 获取当前用户的帖子 |
| 更新帖子 | /discover/posts/{postId} | PUT | 更新帖子内容 |
| 删除我的帖子 | /discover/my/posts/{postId} | DELETE | 删除当前用户的帖子 |
| 获取我的收藏 | /discover/my/collections | GET | 获取当前用户的收藏 |
| 移除收藏 | /discover/my/collections/{postId} | DELETE | 移除收藏的帖子 |
| 获取浏览历史 | /discover/my/history | GET | 获取浏览历史 |
| 清空浏览历史 | /discover/my/history | DELETE | 清空浏览历史 |
| 获取我的统计数据 | /discover/my/stats | GET | 获取用户在发现模块的统计数据 |
| 更新帖子可见性 | /discover/my/posts/{postId}/visibility | PUT | 更新帖子的可见性 |

### 2.3 消息模块接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 获取对话列表 | /messages/conversations | GET | 获取用户的对话列表 |
| 创建对话 | /messages/conversations | POST | 创建新对话 |
| 创建客服对话 | /messages/customer-service | POST | 创建与客服的对话 |
| 获取消息列表 | /messages/conversations/{conversationId} | GET | 获取对话中的消息 |
| 发送消息 | /messages/conversations/{conversationId} | POST | 发送消息 |
| 标记已读 | /messages/conversations/{conversationId}/read | POST | 标记对话为已读 |
| 获取通知 | /messages/notifications/{type} | GET | 获取指定类型的通知 |
| 标记通知已读 | /messages/notifications/{type}/read | POST | 标记通知为已读 |
| 获取未读消息数 | /messages/unread-count | GET | 获取未读消息数量 |
| 删除对话 | /messages/conversations/{conversationId} | DELETE | 删除对话 |
| 清空通知 | /messages/notifications/{type} | DELETE | 清空指定类型的通知 |
| 删除消息 | /messages/messages/{messageId} | DELETE | 删除消息 |
| 撤回消息 | /messages/messages/{messageId}/recall | POST | 撤回消息 |
| 翻译消息 | /messages/message/translate | GET | 翻译消息内容 |

### 2.4 短语模块接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 获取短语列表 | /phrases | GET | 获取用户的常用短语 |
| 添加短语 | /phrases | POST | 添加新的常用短语 |
| 删除短语 | /phrases/{phraseId} | DELETE | 删除常用短语 |

### 2.5 图片和媒体接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 分类图片 | /classification/predict | POST | 对图片进行分类 |
| 上传帖子图片 | /upload/post-image | POST | 上传帖子图片 |
| 上传图片 | /upload/image | POST | 上传通用图片 |
| 上传媒体 | /upload/media | POST | 上传媒体文件 |
| 上传头像 | /upload/avatar | POST | 上传用户头像 |
| 上传视频 | /upload/media | POST | 上传视频文件 |

### 2.6 AI 遗产路线接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 生成遗产路线 | /ai/heritage-route | POST | 生成 AI 遗产路线 |
| 保存遗产路线 | /ai/heritage-route/favorites | POST | 保存遗产路线到收藏 |
| 获取保存的路线 | /ai/heritage-route/favorites | GET | 获取用户保存的路线 |
| 获取路线详情 | /ai/heritage-route/favorites/{id} | GET | 获取保存的路线详情 |
| 删除保存的路线 | /ai/heritage-route/favorites/{id} | DELETE | 删除保存的路线 |

### 2.7 公共活动接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 获取公共活动 | /public/activities | GET | 获取公共活动列表 |
| 获取活动详情 | /public/activities/{id} | GET | 获取活动详细信息 |
| 获取官方内容 | /public/official | GET | 获取官方内容 |
| 获取首页官方内容 | /public/official/homepage | GET | 获取首页的官方内容 |
| 提交解封申请 | /public/unban-applications | POST | 提交账号解封申请 |
| 获取最新解封申请 | /public/unban-applications/latest | GET | 获取最新的解封申请 |

### 2.8 商家相关接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 申请商家 | /merchant/apply | POST | 提交商家申请 |
| 获取申请状态 | /merchant/apply/status | GET | 获取商家申请状态 |
| 获取商家活动 | /merchant/activities | GET | 获取商家的活动列表 |
| 创建商家活动 | /merchant/activities | POST | 创建新的商家活动 |
| 更新商家活动 | /merchant/activities/{id} | PUT | 更新商家活动 |
| 删除商家活动 | /merchant/activities/{id} | DELETE | 删除商家活动 |
| 获取活动预订 | /merchant/activities/{id}/bookings | GET | 获取活动的预订列表 |
| 获取商家预订 | /merchant/bookings | GET | 获取商家的所有预订 |
| 获取预订详情 | /merchant/bookings/{id} | GET | 获取预订详细信息 |
| 查找预订 | /merchant/bookings/lookup | GET | 通过代码查找预订 |
| 通过图片查找预订 | /merchant/bookings/lookup-image | POST | 通过图片查找预订 |
| 核销预订 | /merchant/bookings/{id}/checkin | POST | 核销预订 |
| 退款预订 | /merchant/bookings/{id}/refund | POST | 为预订退款 |
| 拒绝预订 | /merchant/bookings/{id}/reject | POST | 拒绝预订 |

### 2.9 管理员相关接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 获取商家列表 | /admin/merchants | GET | 获取商家列表 |
| 审核商家 | /admin/merchants/{id}/audit | POST | 审核商家申请 |
| 获取活动列表 | /admin/activities | GET | 获取活动列表 |
| 审核活动 | /admin/activities/{id}/audit | POST | 审核活动 |
| 获取帖子列表 | /admin/posts | GET | 获取帖子列表 |
| 审核帖子 | /admin/posts/{id}/audit | POST | 审核帖子 |
| 批量审核帖子 | /admin/posts/batch-audit | POST | 批量审核帖子 |
| 获取用户列表 | /admin/users | GET | 获取用户列表 |
| 封禁用户 | /admin/users/{id}/ban | POST | 封禁用户 |
| 解封用户 | /admin/users/{id}/unban | POST | 解封用户 |
| 获取解封申请 | /admin/users/unban-applications | GET | 获取解封申请 |
| 审核解封申请 | /admin/users/unban-applications/{id}/audit | POST | 审核解封申请 |
| 获取官方内容 | /admin/official | GET | 获取官方内容 |
| 创建官方内容 | /admin/official | POST | 创建官方内容 |
| 删除官方内容 | /admin/official/{id} | DELETE | 删除官方内容 |
| 获取管理员账号 | /admin/admins | GET | 获取管理员账号列表 |
| 创建管理员账号 | /admin/admins | POST | 创建管理员账号 |
| 更新管理员账号 | /admin/admins/{id} | PUT | 更新管理员账号 |
| 更新管理员密码 | /admin/admins/{id}/password | PUT | 更新管理员密码 |
| 删除管理员账号 | /admin/admins/{id} | DELETE | 删除管理员账号 |

### 2.10 官方内容管理接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 获取官方活动 | /admin/official/activities | GET | 获取官方活动列表 |
| 创建官方活动 | /admin/official/activities | POST | 创建官方活动 |
| 更新官方活动 | /admin/official/activities/{id} | PUT | 更新官方活动 |
| 删除官方活动 | /admin/official/activities/{id} | DELETE | 删除官方活动 |
| 获取官方遗产 | /admin/official/heritages | GET | 获取官方遗产列表 |
| 创建官方遗产 | /admin/official/heritages | POST | 创建官方遗产 |
| 更新官方遗产 | /admin/official/heritages/{id} | PUT | 更新官方遗产 |
| 删除官方遗产 | /admin/official/heritages/{id} | DELETE | 删除官方遗产 |
| 获取官方作品 | /admin/official/works | GET | 获取官方作品列表 |
| 获取发布状态 | /admin/official/published | GET | 获取发布状态 |
| 创建官方作品 | /admin/official/works | POST | 创建官方作品 |
| 更新官方作品 | /admin/official/works/{id} | PUT | 更新官方作品 |
| 删除官方作品 | /admin/official/works/{id} | DELETE | 删除官方作品 |
| 发布到首页 | /admin/official/publish | POST | 发布内容到首页 |

### 2.11 订单和预订接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 获取订单概览 | /orders/overview | GET | 获取订单概览信息 |
| 获取我的订单 | /orders | GET | 获取用户的订单 |
| 支付订单 | /orders/{id}/pay | POST | 支付订单 |
| 支付宝对账 | /alipay/reconcile | POST | 支付宝交易对账 |
| 取消订单 | /orders/{id}/cancel | POST | 取消订单 |
| 预订活动 | /orders/activities/{activityId}/book | POST | 预订活动 |
| 取消活动预订 | /orders/bookings/{id}/cancel | POST | 取消活动预订 |
| 删除活动预订 | /orders/bookings/{id} | DELETE | 删除活动预订 |
| 获取预订详情 | /orders/bookings/{id} | GET | 获取活动预订详情 |
| 支付活动预订 | /orders/bookings/{id}/pay | POST | 支付活动预订 |
| 获取预订二维码 | /orders/bookings/{id}/qrcode | GET | 获取预订二维码 |
| 提交活动评价 | /orders/bookings/{id}/review | POST | 提交活动评价 |

### 2.12 用户相关接口

| 接口名 | URL | 方法 | 功能描述 |
|-------|-----|------|----------|
| 获取用户成就 | /user/achievements | GET | 获取用户成就 |
| 获取用户主页 | /user/homepage/{userId} | GET | 获取用户主页信息 |
| 获取主页设置 | /user/homepage-settings | GET | 获取用户主页设置 |
| 更新主页设置 | /user/homepage-settings | PUT | 更新用户主页设置 |
| 关注用户 | /user/follow/{userId} | POST | 关注用户 |
| 取消关注 | /user/follow/{userId} | DELETE | 取消关注用户 |
| 获取关注状态 | /user/follow/{userId}/status | GET | 获取关注状态 |
| 获取访问记录 | /user/visitor-records | GET | 获取访问记录 |
| 获取用户资料 | /user/profile | GET | 获取用户资料 |
| 更新用户资料 | /user/profile | PUT | 更新用户资料 |
| 获取粉丝列表 | /user/{userId}/followers | GET | 获取用户的粉丝 |
| 获取关注列表 | /user/{userId}/following | GET | 获取用户关注的人 |
| 获取收藏者 | /user/{userId}/collected-by | GET | 获取收藏用户内容的人 |

## 3. 详细接口说明

### 3.1 认证相关接口

#### 3.1.1 登录
- **URL**: `/login`
- **方法**: `POST`
- **请求参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |-------|------|------|------|
  | username | string | 是 | 用户名/手机号/邮箱 |
  | password | string | 是 | 密码 |
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "user": {
        "id": 1,
        "username": "user1",
        "nickname": "用户1",
        "avatar": "https://example.com/avatar.jpg"
      }
    }
  }
  ```

#### 3.1.2 验证码登录
- **URL**: `/login/code`
- **方法**: `POST`
- **请求参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |-------|------|------|------|
  | phone | string | 是 | 手机号 |
  | code | string | 是 | 验证码 |
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "user": {
        "id": 1,
        "username": "user1",
        "nickname": "用户1",
        "avatar": "https://example.com/avatar.jpg"
      }
    }
  }
  ```

#### 3.1.3 注册
- **URL**: `/register`
- **方法**: `POST`
- **请求参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |-------|------|------|------|
  | username | string | 是 | 用户名 |
  | password | string | 是 | 密码 |
  | confirmPassword | string | 是 | 确认密码 |
  | phone | string | 是 | 手机号 |
  | code | string | 是 | 验证码 |
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "注册成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "user": {
        "id": 1,
        "username": "user1",
        "nickname": "用户1",
        "avatar": "https://example.com/avatar.jpg"
      }
    }
  }
  ```

#### 3.1.4 发送验证码
- **URL**: `/send-code`
- **方法**: `POST`
- **请求参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |-------|------|------|------|
  | phone | string | 是 | 手机号 |
  | type | string | 是 | 验证码类型 (register/login/reset) |
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "验证码已发送",
    "data": null
  }
  ```

#### 3.1.5 重置密码
- **URL**: `/reset-password`
- **方法**: `POST`
- **请求参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |-------|------|------|------|
  | phone | string | 是 | 手机号 |
  | code | string | 是 | 验证码 |
  | newPassword | string | 是 | 新密码 |
  | confirmPassword | string | 是 | 确认新密码 |
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "密码重置成功",
    "data": null
  }
  ```

### 3.2 发现模块接口

#### 3.2.1 获取发现帖子列表
- **URL**: `/discover/posts`
- **方法**: `GET`
- **请求参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |-------|------|------|------|
  | page | number | 否 | 页码，默认 0 |
  | size | number | 否 | 每页数量，默认 20 |
  | category | string | 否 | 分类 |
  | keyword | string | 否 | 关键词 |
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "total": 100,
      "posts": [
        {
          "id": 1,
          "title": "传统文化之旅",
          "content": "这是一篇关于传统文化的帖子...",
          "images": ["https://example.com/image1.jpg"],
          "author": {
            "id": 2,
            "nickname": "文化爱好者",
            "avatar": "https://example.com/avatar2.jpg"
          },
          "category": "文化",
          "likeCount": 10,
          "commentCount": 5,
          "collectCount": 3,
          "createdAt": "2026-03-01T12:00:00Z"
        }
      ]
    }
  }
  ```

#### 3.2.2 创建帖子
- **URL**: `/discover/posts`
- **方法**: `POST`
- **请求参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |-------|------|------|------|
  | title | string | 是 | 帖子标题 |
  | content | string | 是 | 帖子内容 |
  | images | array | 否 | 图片URL数组 |
  | category | string | 是 | 分类 |
  | tags | array | 否 | 标签数组 |
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "发布成功",
    "data": {
      "id": 1,
      "title": "传统文化之旅",
      "content": "这是一篇关于传统文化的帖子...",
      "images": ["https://example.com/image1.jpg"],
      "category": "文化",
      "createdAt": "2026-03-01T12:00:00Z"
    }
  }
  ```

### 3.3 活动预订接口

#### 3.3.1 预订活动
- **URL**: `/orders/activities/{activityId}/book`
- **方法**: `POST`
- **请求参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |-------|------|------|------|
  | participants | number | 是 | 参与人数 |
  | contactName | string | 是 | 联系人姓名 |
  | contactPhone | string | 是 | 联系人电话 |
  | notes | string | 否 | 备注 |
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "预订成功",
    "data": {
      "id": 1,
      "reserveNo": "BOOK202603010001",
      "activityId": 1,
      "activityTitle": "传统文化体验活动",
      "participants": 2,
      "status": "pending_payment",
      "totalAmount": 200,
      "createdAt": "2026-03-01T12:00:00Z"
    }
  }
  ```

#### 3.3.2 支付活动预订
- **URL**: `/orders/bookings/{id}/pay`
- **方法**: `POST`
- **请求参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |-------|------|------|------|
  | paymentType | string | 是 | 支付方式 (alipay/wechat) |
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "formHtml": "<form action='https://alipay.com/...' method='post'>...</form>",
      "paymentUrl": "https://alipay.com/..."
    }
  }
  ```

## 4. 错误处理

### 4.1 错误码

| 错误码 | 描述 |
|-------|------|
| 400 | 请求参数错误 |
| 401 | 未授权，需要登录 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 4.2 错误响应格式

```json
{
  "code": 400,
  "message": "参数错误：缺少必要字段",
  "data": null
}
```

## 5. 示例请求

### 5.1 使用 cURL 发送请求

```bash
# 登录
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "password": "password123"}'

# 获取帖子列表
curl -X GET http://localhost:8080/api/discover/posts \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 创建帖子
curl -X POST http://localhost:8080/api/discover/posts \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{"title": "传统文化之旅", "content": "这是一篇关于传统文化的帖子...", "category": "文化"}'
```

### 5.2 使用 JavaScript Fetch API

```javascript
// 登录
const loginResponse = await fetch('/api/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'user1',
    password: 'password123'
  })
});

const loginData = await loginResponse.json();
const token = loginData.data.token;

// 获取帖子列表
const postsResponse = await fetch('/api/discover/posts', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const postsData = await postsResponse.json();
console.log(postsData.data.posts);
```

## 6. 注意事项

1. **认证**: 大部分接口需要在请求头中携带有效的 JWT token
2. **文件上传**: 上传文件时需要设置 `Content-Type: multipart/form-data`
3. **参数验证**: 所有接口都会对请求参数进行验证，请确保参数格式正确
4. **速率限制**: 系统对API请求有速率限制，请勿频繁调用
5. **错误处理**: 请妥善处理接口返回的错误信息，为用户提供友好的提示

## 7. 接口版本

当前接口版本为 v1，未来可能会有版本更新，请关注接口文档的变化。