# Travelate - 国际旅行翻译助手

基于 Vue 3 + Vite 开发的国际旅行翻译软件前端。

## 项目结构

```
travelate-vue/
├── public/                 # 静态资源
├── src/
│   ├── assets/            # 样式和资源文件
│   │   └── main.css       # 全局样式
│   ├── components/        # 公共组件
│   ├── router/            # 路由配置
│   │   └── index.js       # 路由定义
│   ├── views/             # 页面视图
│   │   ├── LoginView.vue  # 登录/注册页面
│   │   └── TranslateView.vue # 翻译主页面
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── index.html             # HTML模板
├── package.json           # 项目依赖
├── vite.config.js         # Vite配置
└── README.md              # 项目说明
```

## 功能特性

### 登录/注册页面
- 精美的滑动切换动画
- 支持用户名/邮箱/密码登录
- 社交账号登录入口（预留）
- 响应式设计，支持移动端

### 翻译主页面
- 支持 20 种语言互译
- 实时翻译（输入后自动翻译）
- 语言互换功能
- 文本朗读（Web Speech API）
- 翻译结果复制
- 收藏功能
- 快捷功能入口：
  - 拍照翻译
  - 语音翻译
  - 对话模式
  - 离线包下载
- 旅行常用语快捷选择

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Vue Router 4** - 官方路由管理器
- **Vite** - 下一代前端构建工具
- **Boxicons** - 开源图标库

## 安装和运行

### 1. 安装依赖

```bash
cd travelate-vue
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

应用将在 http://localhost:5173 启动

### 3. 构建生产版本

```bash
npm run build
```

## 后端接口预留

项目已预留以下接口调用位置：

### 登录/注册
- `POST /api/login` - 用户登录
- `POST /api/register` - 用户注册

### 翻译功能
- `POST /api/translate` - 文本翻译

### 用户功能
- `POST /api/favorites` - 添加收藏
- `GET /api/favorites` - 获取收藏列表
- `GET /api/history` - 获取翻译历史

## 设计说明

- 主色调：#7494ec（蓝紫色）
- 背景渐变：linear-gradient(90deg, #e2e2e2, #c9d6ff)
- 字体：Poppins
- 圆角风格：现代圆角设计
- 动画：流畅的过渡动画效果

## 浏览器支持

- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+

## 开发计划

- [x] 登录/注册页面
- [x] 翻译主页面
- [x] 基础路由配置
- [ ] 连接后端API
- [ ] 添加历史记录功能
- [ ] 添加用户设置页面
- [ ] 实现拍照翻译
- [ ] 实现语音翻译
- [ ] 实现对话模式
