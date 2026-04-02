<template>
  <div class="personal-center">
    <!-- 顶部设置图标 -->
    <div class="page-header">
      <div class="page-heading">
        <p class="page-eyebrow">PERSONAL CENTER</p>
        <h1>个人中心</h1>
        <p class="page-subtitle">这里负责账号管理与常用入口，个人主页单独承担对外展示。</p>
      </div>
      <button class="settings-trigger" @click="showSettingsDrawer = true">
        <i class='bx bx-cog'></i>
        设置
      </button>
    </div>
    
    <!-- 客服消息模态框 -->
    <CustomerServiceView 
      :visible="showCustomerServiceModal" 
      @close="showCustomerServiceModal = false"
    />


    <!-- 个人信息概览 -->
    <div class="overview-card">
      <div class="overview-main">
        <div class="overview-profile">
          <div class="avatar-wrapper">
            <div class="avatar-container">
              <img :src="userInfo.avatar" alt="Avatar" class="avatar">
              <div class="edit-avatar-btn" @click="navigateToEditProfile">
                <i class='bx bxs-camera'></i>
              </div>
            </div>
          </div>
          <div class="profile-copy">
            <div class="profile-title-row">
              <h2>{{ userInfo.nickname }}</h2>
              <div class="role-badge" v-if="isMerchantRole">
                <i :class="userInfo.role === 'admin' ? 'bx bxs-shield' : 'bx bxs-store'"></i>
                {{ userInfo.role === 'admin' ? '管理员' : '商家' }}
              </div>
            </div>
            <p class="username">@{{ userInfo.username }}</p>
            <p class="profile-summary">{{ profileSummary }}</p>
          </div>
        </div>
        <div class="primary-actions">
          <button class="my-homepage-btn" @click="openHomepage">
            <i class='bx bx-user-circle'></i>
            个人主页
          </button>
          <button class="secondary-action-btn" :class="{ 'has-alert': hasWorkbenchAlert }" @click="handleSecondaryAction">
            <i :class="secondaryActionIcon"></i>
            {{ secondaryActionLabel }}
            <span v-if="hasWorkbenchAlert" class="workbench-alert-pill">{{ workbenchAlertText }}</span>
          </button>
        </div>
      </div>

      <div class="metric-strip">
        <template v-if="isMerchantRole">
          <button class="metric-card" @click="navigateToMerchantReservations">
            <span class="metric-label">活动报名</span>
            <strong class="metric-value">{{ stats.activityBookingCount }}</strong>
            <span class="metric-footnote">已核销 {{ stats.checkedInCount }}</span>
          </button>
          <button class="metric-card" @click="openFollowers">
            <span class="metric-label">粉丝</span>
            <strong class="metric-value">{{ userInfo.followerCount || 0 }}</strong>
            <span class="metric-footnote">查看关注你的人</span>
          </button>
          <button class="metric-card" @click="openVisitors">
            <span class="metric-label">访客</span>
            <strong class="metric-value">{{ receivedVisitorCount }}</strong>
            <span class="metric-footnote">查看 {{ viewedVisitorCount }} 人 · 被看 {{ receivedVisitorCount }} 人</span>
          </button>
          <button class="metric-card" @click="navigateToMyPosts">
            <span class="metric-label">内容发布</span>
            <strong class="metric-value">{{ stats.posts }}</strong>
            <span class="metric-footnote">查看已发布内容</span>
          </button>
        </template>
        <template v-else>
          <button class="metric-card" @click="navigateToMyPosts">
            <span class="metric-label">我的发布</span>
            <strong class="metric-value">{{ stats.posts }}</strong>
            <span class="metric-footnote">继续管理内容</span>
          </button>
          <button class="metric-card" @click="navigateToMyCollections">
            <span class="metric-label">我的收藏</span>
            <strong class="metric-value">{{ stats.collections }}</strong>
            <span class="metric-footnote">回看喜欢的内容</span>
          </button>
          <button class="metric-card" @click="navigateToHistory">
            <span class="metric-label">浏览历史</span>
            <strong class="metric-value">{{ stats.history }}</strong>
            <span class="metric-footnote">继续上次浏览</span>
          </button>

        </template>
      </div>
    </div>

    <div class="dashboard-layout">
      <div class="section-card main-panel">
        <div v-if="isMerchantRole" class="merchant-dashboard">
          <div class="section-header">
            <h3>常用工作台</h3>
            <span class="section-caption">个人中心只保留管理入口，主页展示请通过上方"个人主页"进入。</span>
          </div>

          <div class="dashboard-grid">
            <div class="content-card" @click="navigateToMerchantActivities">
              <div class="card-icon" style="background: #e6f7ff; color: #1890ff;">
                <i class='bx bxs-calendar-event'></i>
              </div>
              <div class="card-info">
                <h4>活动管理</h4>
                <p>发布、编辑和下架活动</p>
              </div>
            </div>
            <div class="content-card" @click="navigateToMerchantReservations">
              <div class="card-icon" style="background: #fff0f6; color: #ff6b81;">
                <i class='bx bxs-calendar-check'></i>
              </div>
              <div class="card-info">
                <h4>预约订单</h4>
                <p>处理用户报名与到店核销</p>
              </div>
            </div>
            <div class="content-card" @click="navigateToMerchantApply">
              <div class="card-icon" style="background: #eef2ff; color: #4f46e5;">
                <i class='bx bxs-store-alt'></i>
              </div>
              <div class="card-info">
                <h4>商家信息</h4>
                <p>管理商家申请资料和审核状态</p>
              </div>
            </div>
            <div class="content-card" @click="navigateToMyPosts">
              <div class="card-icon" style="background: #ecfeff; color: #0891b2;">
                <i class='bx bxs-edit-alt'></i>
              </div>
              <div class="card-info">
                <h4>我的发布</h4>
                <p>查看公开展示的内容</p>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="user-dashboard">
          <div class="section-header">
            <h3>常用功能</h3>
          </div>

          <div class="dashboard-grid">
            <div class="content-card" @click="navigateToMyReservations">
              <div class="card-icon" style="background: #e6f7ff; color: #1890ff;">
                <i class='bx bxs-calendar-check'></i>
              </div>
              <div class="card-info">
                <h4>我的预约</h4>
                <p>查看活动报名与当前状态</p>
              </div>
            </div>
            <div class="content-card" @click="navigateToMyCollections">
              <div class="card-icon" style="background: #fff0f6; color: #ff5f95;">
                <i class='bx bxs-star'></i>
              </div>
              <div class="card-info">
                <h4>我的收藏</h4>
                <p>回看收藏过的内容</p>
              </div>
            </div>
            <div class="content-card" @click="navigateToMyPosts">
              <div class="card-icon" style="background: #eef2ff; color: #4facfe;">
                <i class='bx bxs-edit-alt'></i>
              </div>
              <div class="card-info">
                <h4>我的发布</h4>
                <p>管理自己公开发布的内容</p>
              </div>
            </div>
            <div class="content-card" @click="navigateToHistory">
              <div class="card-icon" style="background: #fff7e6; color: #fa8c16;">
                <i class='bx bxs-history'></i>
              </div>
              <div class="card-info">
                <h4>浏览历史</h4>
                <p>继续之前浏览过的内容</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="section-card side-panel">
        <div class="section-header">
          <h3>{{ isMerchantRole ? '关系与互动' : '社交' }}</h3>
        </div>

        <div class="social-list">
          <button class="social-item" @click="openFollowing">
            <div class="social-icon" style="background: rgba(31, 111, 235, 0.12); color: #1f6feb;">
              <i class='bx bx-user-plus'></i>
            </div>
            <div class="social-copy">
              <strong>关注</strong>
              <span>我关注了 {{ userInfo.followingCount || 0 }} 人</span>
            </div>
            <i class='bx bx-chevron-right social-arrow'></i>
          </button>
          <button class="social-item" @click="openFollowers">
            <div class="social-icon" style="background: rgba(34, 197, 94, 0.14); color: #16a34a;">
              <i class='bx bx-group'></i>
            </div>
            <div class="social-copy">
              <strong>粉丝</strong>
              <span>{{ userInfo.followerCount || 0 }} 人关注了你</span>
            </div>
            <i class='bx bx-chevron-right social-arrow'></i>
          </button>
          <button class="social-item" @click="openVisitors">
            <div class="social-icon" style="background: rgba(250, 140, 22, 0.16); color: #ea580c;">
              <i class='bx bx-walk'></i>
            </div>
            <div class="social-copy">
              <strong>访客</strong>
              <span>查看 {{ viewedVisitorCount }} 人 · 被看 {{ receivedVisitorCount }} 人</span>
            </div>
            <i class='bx bx-chevron-right social-arrow'></i>
          </button>
        </div>
      </div>
    </div>

    <div v-if="false" class="profile-section">
      <div class="profile-header">
        <div class="avatar-wrapper">
          <div class="avatar-container">
            <img :src="userInfo.avatar" alt="Avatar" class="avatar">
            <div class="edit-avatar-btn" @click="navigateToEditProfile">
              <i class='bx bxs-camera'></i>
            </div>
          </div>
        </div>
        <div class="profile-info">
          <h2>{{ userInfo.nickname }}</h2>
          <p class="username">{{ userInfo.username }}</p>
          <div class="role-badge" v-if="isMerchantRole">
            <i :class="userInfo.role === 'admin' ? 'bx bxs-shield' : 'bx bxs-store'"></i>
            {{ userInfo.role === 'admin' ? '管理员' : '商家' }}
          </div>
          <button class="my-homepage-btn" @click="openPrimaryPanel">
            <i class='bx bx-user-circle'></i> {{ userInfo.role === 'admin' ? primaryPanelLabel : '个人主页' }} <i class='bx bx-chevron-right'></i>
          </button>
        </div>
      </div>
      
      <!-- 数据统计框（中间框） -->
      <div class="stats-box">
        <template v-if="isMerchantRole">
          <div class="stat-item" @click="navigateToMerchantOrders">
            <span class="stat-value">{{ merchantStats.weeklySales }}</span>
            <span class="stat-label">近七日销量</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item" @click="openFollowers">
            <span class="stat-value">{{ userInfo.followerCount || 0 }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item" @click="openVisitors">
            <span class="stat-value">{{ receivedVisitorCount }}</span>
            <span class="stat-label">访客</span>
          </div>
        </template>
        <template v-else>
          <div class="stat-item" @click="openFollowing">
            <span class="stat-value">{{ userInfo.followingCount || 0 }}</span>
            <span class="stat-label">关注</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item" @click="openFollowers">
            <span class="stat-value">{{ userInfo.followerCount || 0 }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item" @click="openVisitors">
            <span class="stat-value">{{ receivedVisitorCount }}</span>
            <span class="stat-label">访客</span>
          </div>
        </template>
      </div>
    </div>

    <!-- 下半部分内容展示（不强制分类，美观为主） -->
    <div v-if="false" class="content-section">
      
      <!-- 商家专属布局 -->
      <div v-if="isMerchantRole" class="merchant-dashboard">
        <div class="section-header">
          <h3>店铺运营中心</h3>
        </div>
        
        <!-- 销量统计图表占位-->
        <div class="chart-card" @click="navigateToMerchantOrders">
          <div class="chart-header">
            <h4>近七日销量趋势</h4>
            <span class="trend up"><i class='bx bx-trending-up'></i> {{ merchantStats.growthRate }}</span>
          </div>
          <div class="chart-placeholder">
            <div class="bar" style="height: 40%"></div>
            <div class="bar" style="height: 60%"></div>
            <div class="bar" style="height: 35%"></div>
            <div class="bar" style="height: 80%"></div>
            <div class="bar" style="height: 50%"></div>
            <div class="bar" style="height: 90%"></div>
            <div class="bar" style="height: 100%"></div>
          </div>
        </div>

        <div class="content-grid">
          <div class="content-card" @click="navigateToMerchantActivities">
            <div class="card-icon" style="background: #e6f7ff; color: #1890ff;">
              <i class='bx bxs-calendar-event'></i>
            </div>
            <div class="card-info">
              <h4>活动管理</h4>
              <p>发布与编辑活动</p>
            </div>
          </div>
          <div class="content-card" @click="navigateToMerchantReservations">
            <div class="card-icon" style="background: #fff0f6; color: #ff9a9e;">
              <i class='bx bxs-calendar-check'></i>
            </div>
            <div class="card-info">
              <h4>预约订单</h4>
              <p>处理用户预约</p>
            </div>
          </div>
          <div class="content-card" @click="navigateToMerchantProducts">
            <div class="card-icon" style="background: #f6ffed; color: #52c41a;">
              <i class='bx bxs-box'></i>
            </div>
            <div class="card-info">
              <h4>商品管理</h4>
              <p>维护活动商品</p>
            </div>
          </div>
          <div class="content-card" @click="navigateToMerchantOrders">
            <div class="card-icon" style="background: #fff7e6; color: #fa8c16;">
              <i class='bx bxs-chart'></i>
            </div>
            <div class="card-info">
              <h4>订单统计</h4>
              <p>查看订单与履约情况</p>
            </div>
          </div>
          <div class="content-card" @click="navigateToMyPosts">
            <div class="card-icon" style="background: #eef2ff; color: #4facfe;">
              <i class='bx bxs-edit-alt'></i>
            </div>
            <div class="card-info">
              <h4>我的发布</h4>
              <p>查看已发布内容</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 普通用户专属布局 -->
      <div v-else class="user-dashboard">
        <div class="section-header">
          <h3>活动预约</h3>
        </div>
        
        <div class="content-grid">
          <div class="content-card" @click="navigateToMyReservations">
            <div class="card-icon" style="background: #e6f7ff; color: #1890ff;">
              <i class='bx bxs-calendar-check'></i>
            </div>
            <div class="card-info">
              <h4>我的预约</h4>
              <p>查看活动报名与状态</p>
            </div>
          </div>
          <div class="content-card" @click="navigateToOrders">
            <div class="card-icon" style="background: #f9f0ff; color: #a18cd1;">
              <i class='bx bxs-package'></i>
            </div>
            <div class="card-info">
              <h4>商品订单</h4>
              <p>管理商品订单记录</p>
            </div>
          </div>
          <div class="content-card" @click="navigateToAchievements">
            <div class="card-icon" style="background: #f6ffed; color: #52c41a;">
              <i class='bx bxs-medal'></i>
            </div>
            <div class="card-info">
              <h4>打卡成就</h4>
              <p>查看线下体验成长</p>
            </div>
          </div>
          <div class="content-card" @click="navigateToMyCollections">
            <div class="card-icon" style="background: #fff0f6; color: #ff9a9e;">
              <i class='bx bxs-star'></i>
            </div>
            <div class="card-info">
              <h4>我的收藏</h4>
              <p>回看收藏的内容</p>
            </div>
          </div>
        </div>

        <!-- 底部列表菜单 -->
        <div class="menu-list">
          <div class="menu-item" @click="navigateToHistory">
            <i class='bx bxs-history'></i>
            <span>浏览历史</span>
            <i class='bx bx-chevron-right'></i>
          </div>
          <div class="menu-item" @click="navigateToMyPosts">
            <i class='bx bxs-edit-alt'></i>
            <span>我的发布</span>
            <i class='bx bx-chevron-right'></i>
          </div>
        </div>
      </div>

    </div>

    <!-- 设置侧边栏弹窗 -->
    <div class="modal settings-drawer" v-if="showSettingsDrawer" @click.self="showSettingsDrawer = false">
      <div class="drawer-content">
        <div class="drawer-header">
          <h3>设置</h3>
          <i class='bx bx-x close-btn' @click="showSettingsDrawer = false"></i>
        </div>
        
        <div class="menu-group">
          <div class="menu-item" @click="openAccountManager">
            <i class='bx bx-id-card'></i>
            <span>账号管理</span>
            <i class='bx bx-chevron-right'></i>
          </div>
          <div class="menu-item" @click="navigateToEditProfile">
            <i class='bx bxs-user'></i>
            <span>编辑个人资料</span>
            <i class='bx bx-chevron-right'></i>
          </div>
          <div class="menu-item" @click="showChangePassword">
            <i class='bx bxs-lock-alt'></i>
            <span>修改密码</span>
            <i class='bx bx-chevron-right'></i>
          </div>
          <div class="menu-item" @click="openCustomerService">
            <i class='bx bxs-info-circle'></i>
            <span>联系客服</span>
            <i class='bx bx-chevron-right'></i>
          </div>
          
          <!-- 商家相关设置 -->
          <div class="menu-item" v-if="!isMerchantRole" @click="navigateToMerchantApply">
            <i class='bx bxs-store'></i>
            <span>注册为商家</span>
            <i class='bx bx-chevron-right'></i>
          </div>
          <div class="menu-item menu-item--with-alert" v-else @click="openPrimaryPanel" :data-alert="hasWorkbenchAlert ? workbenchAlertText : ''">
            <i class='bx bxs-dashboard'></i>
            <span>{{ userInfo.role === 'admin' ? '进入管理后台' : '进入商家工作台' }}</span>
            <i class='bx bx-chevron-right'></i>
          </div>
        </div>
        
        <div class="logout-section">
          <button class="logout-btn" @click="showLogoutConfirm">
            <i class='bx bx-log-out'></i> 退出登录
          </button>
          <button class="delete-account-btn" @click="showDeleteConfirm">
            <i class='bx bx-user-x'></i> 注销账号
          </button>
        </div>
      </div>
    </div>

    <div class="modal" v-if="showAccountManagerModal" @click.self="closeAccountManager">
      <div class="modal-content account-manager-modal">
        <div class="modal-header account-manager-header">
          <div>
            <h3>账号管理</h3>
            <p>添加常用账号后，点击即可快速切换</p>
          </div>
          <i class='bx bx-x close-btn' @click="closeAccountManager"></i>
        </div>

        <div class="account-manager-body">
          <div class="saved-account-section">
            <div class="saved-account-head">
              <strong>已保存账号</strong>
              <span>{{ savedAccounts.length }} 个</span>
            </div>

            <div v-if="savedAccounts.length > 0" class="saved-account-list">
              <div
                v-for="account in savedAccounts"
                :key="account.account"
                class="saved-account-item"
                :class="{
                  active: currentAccountId && account.userId === currentAccountId,
                  switching: switchingAccount === account.account
                }"
                @click="switchManagedAccount(account)"
                @keydown.enter.prevent="switchManagedAccount(account)"
                tabindex="0"
              >
                <img :src="account.avatar || userInfo.avatar" alt="avatar" class="saved-account-avatar" />
                <div class="saved-account-copy">
                  <div class="saved-account-row">
                    <strong>{{ account.nickname || account.username || account.account }}</strong>
                    <span v-if="currentAccountId && account.userId === currentAccountId" class="account-badge">当前账号</span>
                  </div>
                  <span class="saved-account-meta">{{ account.account }}</span>
                </div>
                <span v-if="switchingAccount === account.account" class="account-switching-text">切换中...</span>
                <button
                  class="remove-account-btn"
                  type="button"
                  @click.stop="removeManagedAccount(account.account)"
                >
                  删除
                </button>
              </div>
            </div>
            <div v-else class="account-empty-state">
              <i class='bx bx-user-plus'></i>
              <p>还没有保存账号</p>
            </div>
          </div>

          <div class="account-form-section">
            <div class="saved-account-head">
              <strong>添加账号</strong>
              <span>会先校验账号密码</span>
            </div>

            <div class="form-group">
              <label>账号</label>
              <input
                v-model.trim="accountForm.username"
                type="text"
                placeholder="请输入用户名 / 手机号 / 邮箱"
              />
            </div>
            <div class="form-group">
              <label>密码</label>
              <input
                v-model="accountForm.password"
                type="password"
                placeholder="请输入密码"
              />
            </div>
            <p class="account-note">仅保存在当前设备浏览器，用于一键切换账号。</p>

            <div class="modal-buttons account-manager-actions">
              <button class="btn secondary" @click="closeAccountManager">取消</button>
              <button class="btn primary" :disabled="addingAccount" @click="addManagedAccount">
                {{ addingAccount ? '添加中...' : '添加账号' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 退出登录确认弹窗-->
    <div class="modal" v-if="showLogoutModal">
      <div class="modal-content">
        <h3>退出登录</h3>
        <p>确定要退出登录吗？</p>
        <div class="modal-buttons">
          <button class="btn secondary" @click="showLogoutModal = false">取消</button>
          <button class="btn primary" @click="handleLogout">确定</button>
        </div>
      </div>
    </div>
    <div class="modal" v-if="showDeleteModal">
      <div class="modal-content">
        <h3>注销账号</h3>
        <p>注销后你的账号与数据将被删除，确定继续吗？</p>
        <div class="modal-buttons">
          <button class="btn secondary" @click="showDeleteModal = false">取消</button>
          <button class="btn danger" @click="handleDeleteAccount">确定</button>
        </div>
      </div>
    </div>

    <!-- 注销商家确认弹窗 -->
    <div class="modal" v-if="showUnregisterMerchantModal">
      <div class="modal-content">
        <h3>注销商家身份</h3>
        <p>注销商家将删除您的所有商品并退回普通用户身份。是否继续？</p>
        <div class="modal-buttons">
          <button class="btn secondary" @click="showUnregisterMerchantModal = false">取消</button>
          <button class="btn danger" @click="handleUnregisterMerchant">确定</button>
        </div>
      </div>
    </div>

    <!-- 修改瀵嗙爜弹窗 -->
    <div class="modal" v-if="showPasswordModal">
      <div class="modal-content">
        <h3>修改密码</h3>
        <div class="form-group">
          <label>原密码</label>
          <input 
            type="password" 
            v-model="passwordForm.oldPassword" 
            placeholder="原密码"
          />
        </div>
        <div class="form-group">
          <label>新密码</label>
          <input 
            type="password" 
            v-model="passwordForm.newPassword" 
            placeholder="8-20"
          />
          <span v-if="passwordErrors.newPassword" class="error-text">{{ passwordErrors.newPassword }}</span>
        </div>
        <div class="form-group">
          <label>确认新密码</label>
          <input 
            type="password" 
            v-model="passwordForm.confirmPassword" 
            placeholder="确认新密码"
          />
          <span v-if="passwordErrors.confirmPassword" class="error-text">{{ passwordErrors.confirmPassword }}</span>
        </div>
        <div class="modal-buttons">
          <button class="btn secondary" @click="closePasswordModal">取消</button>
          <button class="btn primary" @click="submitPasswordChange">确定</button>
        </div>
      </div>
    </div>

    <!-- 商家认证弹窗 -->
    <div class="modal" v-if="showMerchantCertModal">
      <div class="modal-content">
        <h3>申请成为商家</h3>
        <p style="margin-bottom: 15px; color: #666; font-size: 14px;">提交以下信息以进行商家认证</p>
        <div class="form-group">
          <label>真实姓名/企业名称</label>
          <input 
            type="text" 
            v-model="merchantForm.realName" 
            placeholder="请输入真实姓名或企业名称"
          />
        </div>
        <div class="form-group">
          <label>联系电话</label>
          <input 
            type="text" 
            v-model="merchantForm.phone" 
            placeholder="请输入联系电话"
          />
        </div>
        <div class="form-group">
          <label>主营业务</label>
          <input 
            type="text" 
            v-model="merchantForm.businessType" 
            placeholder="例如：酒店、餐饮、旅游纪念品等"
          />
        </div>
        <div class="modal-buttons">
          <button class="btn secondary" @click="showMerchantCertModal = false">取消</button>
          <button class="btn primary" @click="submitMerchantCert">提交申请</button>
        </div>
      </div>
    </div>

    <!-- 粉丝列表弹窗 -->
    <div class="modal" v-if="showFollowersModal">
      <div class="modal-content user-list-modal">
        <div class="modal-header">
          <h3>粉丝列表</h3>
          <i class='bx bx-x close-btn' @click="showFollowersModal = false"></i>
        </div>
        <div class="user-list" v-if="!loadingFollowers">
          <div class="user-item" v-for="user in followersList" :key="user.id">
            <img :src="user.avatar" class="user-avatar" />
            <div class="user-info">
              <span class="user-name">{{ user.nickname }}</span>
              <span class="user-bio">{{ user.bio || '暂无简介' }}</span>
            </div>
          </div>
          <div class="empty-state" v-if="followersList.length === 0">
            <i class='bx bx-user-x'></i>
            <p>暂无粉丝</p>
          </div>
        </div>
        <div class="loading-state" v-else>
          <i class='bx bx-loader-alt bx-spin'></i>
          <p>加载中...</p>
        </div>
      </div>
    </div>

    <!-- 关注列表弹窗 -->
    <div class="modal" v-if="showFollowingModal">
      <div class="modal-content user-list-modal">
        <div class="modal-header">
          <h3>关注列表</h3>
          <i class='bx bx-x close-btn' @click="showFollowingModal = false"></i>
        </div>
        <div class="user-list" v-if="!loadingFollowing">
          <div class="user-item" v-for="user in followingList" :key="user.id">
            <img :src="user.avatar" class="user-avatar" />
            <div class="user-info">
              <span class="user-name">{{ user.nickname }}</span>
              <span class="user-bio">{{ user.bio || '暂无简介' }}</span>
            </div>
          </div>
          <div class="empty-state" v-if="followingList.length === 0">
            <i class='bx bx-user-plus'></i>
            <p>暂无关注</p>
          </div>
        </div>
        <div class="loading-state" v-else>
          <i class='bx bx-loader-alt bx-spin'></i>
          <p>加载中...</p>
        </div>
      </div>
    </div>

    <!-- 访客列表弹窗 -->
    <div class="modal" v-if="showVisitorsModal">
      <div class="modal-content user-list-modal">
        <div class="modal-header">
          <h3>访客记录</h3>
          <i class='bx bx-x close-btn' @click="showVisitorsModal = false"></i>
        </div>

        <div class="visitor-sections" v-if="!loadingVisitors">
          <div class="visitor-section">
            <div class="visitor-section-head">
              <strong>我查看了谁</strong>
              <span>{{ viewedVisitorCount }} 人</span>
            </div>

            <div class="user-list visitor-list" v-if="viewedUsersList.length > 0">
              <div class="user-item" v-for="visitor in viewedUsersList" :key="`viewed-${visitor.id}`">
                <img :src="visitor.avatar" class="user-avatar" />
                <div class="user-info">
                  <span class="user-name">{{ visitor.nickname }}</span>
                  <span class="user-time">{{ visitor.viewTime || visitor.visitTime || '最近查看' }}</span>
                </div>
              </div>
            </div>
            <div class="empty-state visitor-empty" v-else>
              <i class='bx bx-compass'></i>
              <p>暂无查看记录</p>
            </div>
          </div>

          <div class="visitor-section">
            <div class="visitor-section-head">
              <strong>谁查看了我</strong>
              <span>{{ receivedVisitorCount }} 人</span>
            </div>

            <div class="user-list visitor-list" v-if="visitorsList.length > 0">
              <div class="user-item" v-for="visitor in visitorsList" :key="`received-${visitor.id}`">
                <img :src="visitor.avatar" class="user-avatar" />
                <div class="user-info">
                  <span class="user-name">{{ visitor.nickname }}</span>
                  <span class="user-time">{{ visitor.visitTime || visitor.viewTime || '最近访问' }}</span>
                </div>
              </div>
            </div>
            <div class="empty-state visitor-empty" v-else>
              <i class='bx bx-walk'></i>
              <p>暂无被访问记录</p>
            </div>
          </div>
        </div>
        <div class="loading-state" v-else>
          <i class='bx bx-loader-alt bx-spin'></i>
          <p>加载中...</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onBeforeUnmount, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { createCustomerServiceConversation, getMyDiscoverStats, getMyOrderOverview, getVisitorRecords, login } from '../api/app'
import CustomerServiceView from './CustomerServiceView.vue'
import { refreshWorkbenchBadges, workbenchBadgeState } from '@/utils/workbenchBadge.js'

const { appContext } = getCurrentInstance()
const notify = appContext.config.globalProperties.$notify

const router = useRouter()
const showLogoutModal = ref(false)
const showSettingsDrawer = ref(false)
const showAccountManagerModal = ref(false)
const showCustomerServiceModal = ref(false)
let badgeTimer = null

const addingAccount = ref(false)
const switchingAccount = ref('')
const savedAccounts = ref([])
const accountForm = ref({
  username: '',
  password: ''
})

const showPasswordModal = ref(false)
const showUnregisterMerchantModal = ref(false)

const showFollowersModal = ref(false)
const showFollowingModal = ref(false)
const followersList = ref([])
const followingList = ref([])
const loadingFollowers = ref(false)
const loadingFollowing = ref(false)

const openFollowers = async () => {
  showFollowersModal.value = true
}

const openFollowing = async () => {
  showFollowingModal.value = true
}

const showVisitorsModal = ref(false)
const visitorsList = ref([])
const viewedUsersList = ref([])
const loadingVisitors = ref(false)

const openVisitors = async () => {
  showVisitorsModal.value = true
  await loadVisitorRecords()
}

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordErrors = ref({
  newPassword: '',
  confirmPassword: ''
})

const userInfo = ref({
  id: null,
  nickname: '用户',
  username: '',
  avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=user',
  role: 'user',
  followerCount: 0,
  followingCount: 0
})
const stats = ref({
  posts: 0,
  collections: 0,
  history: 0,
  activityBookingCount: 0,
  checkedInCount: 0
})

const merchantStats = ref({
  weeklySales: '0',
  growthRate: '+0.0%'
})

const showMerchantCertModal = ref(false)
const merchantForm = ref({
  realName: '',
  phone: '',
  businessType: ''
})

const isMerchantRole = computed(() => ['merchant', 'admin'].includes(userInfo.value.role))
const hasWorkbenchAlert = computed(() => {
  if (userInfo.value.role === 'admin') {
    return workbenchBadgeState.admin.totalCount > 0
  }
  if (userInfo.value.role === 'merchant') {
    return workbenchBadgeState.merchant.totalCount > 0
  }
  return false
})
const workbenchAlertCount = computed(() => {
  if (userInfo.value.role === 'admin') {
    return Number(workbenchBadgeState.admin.totalCount || 0)
  }
  if (userInfo.value.role === 'merchant') {
    return Number(workbenchBadgeState.merchant.totalCount || 0)
  }
  return 0
})
const workbenchAlertText = computed(() => (workbenchAlertCount.value > 99 ? '99+' : String(workbenchAlertCount.value || 0)))
const receivedVisitorCount = computed(() => visitorsList.value.length || 0)
const viewedVisitorCount = computed(() => viewedUsersList.value.length || 0)

const primaryPanelLabel = computed(() => {
  if (userInfo.value.role === 'admin') {
    return '管理后台'
  }
  if (isMerchantRole.value) {
    return '商家工作台'
  }
  return '我的内容'
})

const secondaryActionLabel = computed(() => {
  if (userInfo.value.role === 'admin') {
    return '管理后台'
  }
  if (isMerchantRole.value) {
    return '商家工作台'
  }
  return '编辑资料'
})

const secondaryActionIcon = computed(() => {
  if (userInfo.value.role === 'admin') {
    return 'bx bxs-dashboard'
  }
  if (isMerchantRole.value) {
    return 'bx bxs-store-alt'
  }
  return 'bx bx-edit'
})

const profileSummary = computed(() => {
  if (userInfo.value.bio) {
    return userInfo.value.bio
  }
  if (userInfo.value.role === 'admin') {
    return '这里集中处理后台入口、资料设置和账号操作，个人主页单独作为对外展示页'
  }
  if (isMerchantRole.value) {
    return '这里集中处理商家工作台和账号设置，个人主页单独承担品牌与内容展示'
  }
  return '这里集中处理资料、订单和我的内容入口，个人主页单独展示给其他用户。'
})

const submitMerchantCert = async () => {
  if (!merchantForm.value.realName || !merchantForm.value.phone) {
    notify.warning('请填写必填信息')
    return
  }
  
  try {
    const response = await fetch('/api/user/certify-merchant', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify(merchantForm.value)
    })
    const data = await response.json()
    if (data.code === 200) {
      notify.success('商家认证成功！')
      showMerchantCertModal.value = false
      showSettingsDrawer.value = false
      // 更新本地用户信息
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        user.role = 'merchant'
        localStorage.setItem('user', JSON.stringify(user))
        userInfo.value.role = 'merchant'
      }
    } else {
      notify.error(data.message || '认证失败')
    }
  } catch (error) {
    console.error('认证失败:', error)
    notify.error('网络错误，请稍后再试')
  }
}

// 注销商家身份
const handleUnregisterMerchant = async () => {
  try {
    const response = await fetch('/api/user/unregister-merchant', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const data = await response.json()
    if (data.code === 200) {
      notify.success('已成功注销商家身份')
      showUnregisterMerchantModal.value = false
      showSettingsDrawer.value = false
      
      const userStr = localStorage.getItem('user')
      if (userStr) {
        const user = JSON.parse(userStr)
        user.role = 'user'
        localStorage.setItem('user', JSON.stringify(user))
        userInfo.value.role = 'user'
      }
    } else {
      notify.error(data.message || '注销失败')
    }
  } catch (error) {
    console.error('注销商家失败:', error)
    notify.error('网络错误，请稍后再试')
  }
}

// 本地默认头像（用于网络不可用时的回退）
const localDefaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjUwIiByPSI0MCIgZmlsbD0iIzc0OTRlYyIvPgogIDxjaXJjbGUgY3g9IjUwIiBjeT0iMzAiIHI9IjIwIiBmaWxsPSIjNzQ5NGVjIi8+CiAgPGNpcmNsZSBjeD0iNTUiIGN5PSI0NSIgcj0iNSIgZmlsbD0iI2ZmZiIvPgogIDxjaXJjbGUgY3g9IjQ1IiBjeT0iNDUiIHI9IjUiIGZpbGw9IiNmZmYiLz4KICA8Y2lyY2xlIGN4PSI1MCIgY3k9IjYwIiByPSIyIiBmaWxsPSIjNzQ5NGVjIi8+CiAgPHRleHQgeD0iNTAiIHk9Ijc1IiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTIiIGZpbGw9IiMzMzMiIG5hbWU9ImNvbnRlbnQiPldyb3lhbC4uLjwvdGV4dD4KPC9zdmc+'

const SAVED_ACCOUNT_STORAGE_KEY = 'yayfolk_saved_accounts'

const parseStoredUser = () => {
  const raw = localStorage.getItem('user') || localStorage.getItem('userInfo')
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch (error) {
    console.error('解析用户信息失败:', error)
    return null
  }
}

const persistStoredUser = (user) => {
  localStorage.setItem('user', JSON.stringify(user))
  localStorage.setItem('userInfo', JSON.stringify(user))
}

const normalizeUserInfo = (user = {}) => ({
  ...user,
  nickname: user.nickname || user.username || user.phone || '用户',
  username: user.username || user.phone || user.email || 'YayFolk 用户',
  avatar: user.avatar || localDefaultAvatar,
  followerCount: Number(user.followerCount || 0),
  followingCount: Number(user.followingCount || 0)
})

const currentAccountId = computed(() => Number(userInfo.value.id || 0))

const parseSavedAccounts = () => {
  const raw = localStorage.getItem(SAVED_ACCOUNT_STORAGE_KEY)
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    console.error('解析已保存账号失败', error)
    return []
  }
}

const writeSavedAccounts = (accounts) => {
  localStorage.setItem(SAVED_ACCOUNT_STORAGE_KEY, JSON.stringify(accounts))
}

const sortSavedAccounts = (accounts) => [...accounts].sort((left, right) => Number(right.updatedAt || 0) - Number(left.updatedAt || 0))

const loadSavedAccounts = () => {
  savedAccounts.value = sortSavedAccounts(parseSavedAccounts())
}

const resetAccountForm = () => {
  accountForm.value = {
    username: '',
    password: ''
  }
}

const buildSavedAccount = ({ account, password, user, previousAccount }) => ({
  account,
  password,
  userId: Number(user?.id || previousAccount?.userId || 0),
  nickname: user?.nickname || user?.username || previousAccount?.nickname || account,
  username: user?.username || previousAccount?.username || account,
  avatar: user?.avatar || previousAccount?.avatar || localDefaultAvatar,
  role: user?.role || previousAccount?.role || 'user',
  addedAt: previousAccount?.addedAt || Date.now(),
  updatedAt: Date.now()
})

const upsertSavedAccount = (entry) => {
  const accounts = parseSavedAccounts()
  const index = accounts.findIndex(item => item.account === entry.account)

  if (index >= 0) {
    accounts[index] = {
      ...accounts[index],
      ...entry
    }
  } else {
    accounts.unshift(entry)
  }

  writeSavedAccounts(accounts)
  loadSavedAccounts()
}

const openAccountManager = () => {
  closeSettingsDrawer()
  loadSavedAccounts()
  resetAccountForm()
  showAccountManagerModal.value = true
}

const closeAccountManager = () => {
  showAccountManagerModal.value = false
  resetAccountForm()
  addingAccount.value = false
  switchingAccount.value = ''
}

const applyLoginSession = (payload) => {
  localStorage.setItem('token', payload.token)
  persistStoredUser(payload.user)
  if (window.$axios) {
    window.$axios.defaults.headers.common.Authorization = `Bearer ${payload.token}`
  }
}

const clearLoginSession = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('userInfo')
  if (window.$axios) {
    delete window.$axios.defaults.headers.common.Authorization
  }
}

const addManagedAccount = async () => {
  const account = accountForm.value.username.trim()
  const password = accountForm.value.password

  if (!account || !password) {
    notify.warning('请输入账号和密码')
    return
  }

  addingAccount.value = true

  try {
    const result = await login({
      username: account,
      password
    })

    if (result.code !== 200 || !result.data?.user || !result.data?.token) {
      notify.error(result.message || '添加账号失败')
      return
    }

    const previousAccount = parseSavedAccounts().find(item => item.account === account)
    upsertSavedAccount(buildSavedAccount({
      account,
      password,
      user: result.data.user,
      previousAccount
    }))
    resetAccountForm()
    notify.success('账号已保存，可直接点击切换')
  } catch (error) {
    console.error('添加账号失败:', error)
    notify.error('账号或密码错误，添加失败')
  } finally {
    addingAccount.value = false
  }
}

const removeManagedAccount = (account) => {
  const nextAccounts = parseSavedAccounts().filter(item => item.account !== account)
  writeSavedAccounts(nextAccounts)
  loadSavedAccounts()
  notify.success('已移除该账号')
}

const switchManagedAccount = async (account) => {
  if (!account?.account || switchingAccount.value) {
    return
  }

  switchingAccount.value = account.account

  try {
    const result = await login({
      username: account.account,
      password: account.password
    })

    if (result.code !== 200 || !result.data?.user || !result.data?.token) {
      notify.error(result.message || '切换账号失败')
      return
    }

    applyLoginSession(result.data)
    upsertSavedAccount(buildSavedAccount({
      account: account.account,
      password: account.password,
      user: result.data.user,
      previousAccount: account
    }))
    await loadPersonalCenterData()
    showAccountManagerModal.value = false
    notify.success(`已切换到 ${result.data.user.nickname || result.data.user.username || account.account}`)
  } catch (error) {
    console.error('切换账号失败:', error)
    notify.error('切换账号失败，请重新添加该账号')
  } finally {
    switchingAccount.value = ''
  }
}

const closeSettingsDrawer = () => {
  showSettingsDrawer.value = false
}

// 导航到编辑个人资料
const navigateToEditProfile = () => {
  closeSettingsDrawer()
  router.push('/personal/edit-profile')
}

const navigateToEditHomepage = () => {
  closeSettingsDrawer()
  if (userInfo.value.id) {
    router.push(`/user-homepage/${userInfo.value.id}`)
    return
  }
  router.push('/personal/edit-homepage')
}

// 导航到我的发布
const navigateToMyPosts = () => {
  closeSettingsDrawer()
  router.push('/personal/my-posts')
}

// 导航到我的收藏
const navigateToMyCollections = () => {
  closeSettingsDrawer()
  router.push('/personal/my-collections')
}

// 导航到浏览历史
const navigateToHistory = () => {
  closeSettingsDrawer()
  router.push('/personal/history')
}

const navigateToMyReservations = () => {
  closeSettingsDrawer()
  router.push('/personal/activities')
}

const navigateToOrders = () => {
  closeSettingsDrawer()
  router.push('/personal/activities')
}

const navigateToAchievements = () => {
  closeSettingsDrawer()
  router.push('/personal/achievements')
}

const navigateToMerchantApply = () => {
  closeSettingsDrawer()
  router.push('/merchant/apply')
}

const navigateToMerchantActivities = () => {
  closeSettingsDrawer()
  router.push('/merchant/activities')
}

const navigateToMerchantReservations = () => {
  closeSettingsDrawer()
  router.push('/merchant/bookings')
}

const navigateToMerchantProducts = () => {
  closeSettingsDrawer()
  router.push('/merchant/activities')
}

const navigateToMerchantOrders = () => {
  closeSettingsDrawer()
  router.push('/merchant/bookings')
}

const openHomepage = () => {
  closeSettingsDrawer()
  if (userInfo.value.id) {
    router.push(`/user-homepage/${userInfo.value.id}`)
    return
  }
  router.push('/personal/edit-profile')
}

const handleSecondaryAction = () => {
  if (isMerchantRole.value) {
    openPrimaryPanel()
    return
  }
  navigateToEditProfile()
}

const openPrimaryPanel = () => {
  closeSettingsDrawer()
  if (userInfo.value.role === 'admin') {
    const storedUser = parseStoredUser()
    const isSuperAdmin = Number(storedUser?.isSuperAdmin || 0) === 1
    router.push(isSuperAdmin ? '/admin/admins' : '/admin/merchants')
    return
  }
  if (userInfo.value.role === 'merchant') {
    router.push('/merchant/activities')
    return
  }
  openHomepage()
}

// 显示修改密码
const showChangePassword = () => {
  closeSettingsDrawer()
  showPasswordModal.value = true
  resetPasswordForm()
}

// 关闭修改密码弹窗
const closePasswordModal = () => {
  showPasswordModal.value = false
  resetPasswordForm()
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  passwordErrors.value = {
    newPassword: '',
    confirmPassword: ''
  }
}

// 验证新密码格式
const validatePassword = (password) => {
  if (!password) {
    return '请输入新密码'
  }
  if (password.length < 8 || password.length > 20) {
    return '密码长度应为8-20位'
  }
  if (!/[a-zA-Z]/.test(password)) {
    return '密码必须包含字母'
  }
  if (!/[0-9]/.test(password)) {
    return '密码必须包含数字'
  }
  return ''
}

// 验证确认密码
const validateConfirmPassword = (password, confirmPassword) => {
  if (!confirmPassword) {
    return '请再次输入新密码'
  }
  if (password !== confirmPassword) {
    return '两次输入的密码不一致'
  }
  return ''
}

// 提交修改密码
const submitPasswordChange = async () => {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value

  if (!oldPassword) {
    notify.warning('请输入旧密码')
    return
  }

  const newPasswordError = validatePassword(newPassword)
  if (newPasswordError) {
    passwordErrors.value.newPassword = newPasswordError
    return
  }
  passwordErrors.value.newPassword = ''

  const confirmPasswordError = validateConfirmPassword(newPassword, confirmPassword)
  if (confirmPasswordError) {
    passwordErrors.value.confirmPassword = confirmPasswordError
    return
  }
  passwordErrors.value.confirmPassword = ''

  try {
    const response = await fetch('/api/user/change-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        oldPassword,
        newPassword,
        confirmPassword
      })
    })

    const data = await response.json()

    if (data.code === 200) {
      notify.success('密码修改成功')
      closePasswordModal()
    } else {
      notify.error(data.message || '密码修改失败')
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    notify.error('网络错误')
  }
}

// 显示关于我们
const openCustomerService = async () => {
  closeSettingsDrawer()
  showCustomerServiceModal.value = true
}

// 显示退出登录确认
const showLogoutConfirm = () => {
  closeSettingsDrawer()
  showLogoutModal.value = true
}

const showDeleteModal = ref(false)
const showDeleteConfirm = () => {
  closeSettingsDrawer()
  showDeleteModal.value = true
}

// 处理退出登录
const handleLogout = () => {
  clearLoginSession()
  router.push('/login')
}

const handleDeleteAccount = async () => {
  try {
    showDeleteModal.value = false
    const response = await fetch('/api/user/account', {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const data = await response.json()
    if (data.code === 200) {
      clearLoginSession()
      notify.success('账号已删除')
      router.push('/login')
    } else {
      notify.error(data.message || '删除账号失败')
    }
  } catch (error) {
    notify.error('网络错误')
  }
}

// 检查头像URL是否可访问
const checkAvatarAccessibility = (url) => {
  return new Promise((resolve) => {
    if (!url || url === localDefaultAvatar) {
      resolve(true)
      return
    }
    
    const img = new Image()
    img.onload = () => resolve(true)
    img.onerror = () => resolve(false)
    img.src = url
  })
}

const formatCompactNumber = (value) => {
  const amount = Number(value || 0)
  if (!Number.isFinite(amount)) {
    return '0'
  }
  if (amount >= 10000) {
    return `${(amount / 10000).toFixed(1)}w`
  }
  if (amount >= 1000) {
    return `${(amount / 1000).toFixed(1)}k`
  }
  return String(Math.round(amount))
}

const formatGrowthRate = (value) => {
  const amount = Number(value || 0)
  return `${amount >= 0 ? '+' : ''}${amount.toFixed(1)}%`
}

const resetVisitorRecords = () => {
  visitorsList.value = []
  viewedUsersList.value = []
}

const applyVisitorRecords = (payload) => {
  visitorsList.value = Array.isArray(payload?.receivedVisitors) ? payload.receivedVisitors : []
  viewedUsersList.value = Array.isArray(payload?.viewedUsers) ? payload.viewedUsers : []
}

const loadVisitorRecords = async () => {
  loadingVisitors.value = true
  try {
    const response = await getVisitorRecords()
    if (response?.code === 200 && response.data) {
      applyVisitorRecords(response.data)
      return
    }
    resetVisitorRecords()
  } catch (error) {
    console.error('获取访客记录失败:', error)
    resetVisitorRecords()
  } finally {
    loadingVisitors.value = false
  }
}

const resolveMerchantStats = (orders = []) => {
  const now = new Date()
  const sevenDaysAgo = new Date(now)
  sevenDaysAgo.setDate(now.getDate() - 7)
  const fourteenDaysAgo = new Date(now)
  fourteenDaysAgo.setDate(now.getDate() - 14)

  let current = 0
  let previous = 0

  orders.forEach((order) => {
    const createdAt = order?.createTime ? new Date(order.createTime) : null
    if (!createdAt || Number.isNaN(createdAt.getTime())) {
      return
    }

    const amount = Number(order.payAmount ?? order.totalAmount ?? 0) / 100
    if (createdAt >= sevenDaysAgo) {
      current += amount
      return
    }
    if (createdAt >= fourteenDaysAgo && createdAt < sevenDaysAgo) {
      previous += amount
    }
  })

  const growth = previous <= 0 ? (current > 0 ? 100 : 0) : ((current - previous) / previous) * 100
  merchantStats.value = {
    weeklySales: formatCompactNumber(current),
    growthRate: formatGrowthRate(growth)
  }
}

const resetStats = () => {
  stats.value = {
    ...stats.value,
    posts: 0,
    collections: 0,
    history: 0
  }
  merchantStats.value = {
    weeklySales: '0',
    growthRate: '+0.0%'
  }
}

const loadPersonalCenterData = async () => {
  resetStats()
  resetVisitorRecords()
  try {
    const user = parseStoredUser()
    if (user) {
      try {
        userInfo.value = normalizeUserInfo(user)

        try {
          const isAccessible = await checkAvatarAccessibility(userInfo.value.avatar)
          if (!isAccessible) {
            userInfo.value.avatar = localDefaultAvatar
          }
        } catch (error) {
          console.error('检查头像可访问性失败', error)
          userInfo.value.avatar = localDefaultAvatar
        }
      } catch (error) {
        console.error('解析用户信息失败:', error)
        userInfo.value = normalizeUserInfo({
          avatar: localDefaultAvatar
        })
      }
    } else {
      userInfo.value = normalizeUserInfo({
        avatar: localDefaultAvatar
      })
    }

    try {
      const [discoverRes, overviewRes, visitorRes] = await Promise.all([
        getMyDiscoverStats().catch(() => null),
        getMyOrderOverview().catch(() => null),
        getVisitorRecords().catch(() => null)
      ])

      if (discoverRes?.code === 200 && discoverRes.data) {
        stats.value = {
          ...stats.value,
          posts: discoverRes.data.posts || 0,
          collections: discoverRes.data.collections || 0,
          history: discoverRes.data.history || 0
        }
      }

      if (overviewRes?.data?.summary) {
        stats.value = {
          ...stats.value,
          activityBookingCount: overviewRes.data.summary.activityBookingCount || 0,
          checkedInCount: overviewRes.data.summary.checkedInCount || 0
        }
      }

      if (visitorRes?.code === 200 && visitorRes.data) {
        applyVisitorRecords(visitorRes.data)
      }

      persistStoredUser({
        ...userInfo.value
      })
    } catch (error) {
      console.error('获取统计数据失败:', error)
      resetStats()
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    userInfo.value = normalizeUserInfo({
      avatar: localDefaultAvatar
    })
    resetStats()
  } finally {
    loadSavedAccounts()
    await refreshWorkbenchBadges()
  }
}

onMounted(async () => {
  await loadPersonalCenterData()
  badgeTimer = window.setInterval(refreshWorkbenchBadges, 60000)
})

onBeforeUnmount(() => {
  if (badgeTimer) {
    window.clearInterval(badgeTimer)
    badgeTimer = null
  }
})
</script>

<style scoped>
.personal-center {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(31, 111, 235, 0.08), transparent 26%),
    radial-gradient(circle at top right, rgba(34, 197, 94, 0.08), transparent 24%),
    #f5f7fa;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  padding: 24px 20px 40px;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-heading h1 {
  margin: 6px 0 8px;
  font-size: 32px;
  line-height: 1.1;
  color: #16324f;
}

.page-eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: #7494ec;
  font-weight: 700;
}

.page-subtitle {
  margin: 0;
  color: #6b7a90;
  font-size: 14px;
}

.settings-trigger {
  border: none;
  border-radius: 999px;
  background: #16324f;
  color: #fff;
  padding: 10px 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 14px 30px rgba(22, 50, 79, 0.14);
}

.overview-card,
.section-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
  border-radius: 28px;
}

.overview-card {
  padding: 28px;
  margin-bottom: 20px;
}

.overview-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.overview-profile {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.profile-copy {
  min-width: 0;
}

.profile-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.profile-copy h2 {
  margin: 0;
  font-size: 28px;
  color: #16324f;
}

.profile-summary {
  margin: 10px 0 0;
  color: #526277;
  font-size: 14px;
  line-height: 1.7;
  max-width: 680px;
}

.primary-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 172px;
}

.secondary-action-btn {
  border: none;
  border-radius: 999px;
  background: #edf3fb;
  color: #16324f;
  padding: 11px 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
}

.secondary-action-btn.has-alert {
  box-shadow: 0 0 0 1px rgba(201, 145, 63, 0.18), 0 10px 22px rgba(163, 112, 38, 0.08);
}

.workbench-alert-pill {
  min-width: 28px;
  height: 22px;
  padding: 0 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid rgba(201, 145, 63, 0.28);
  background: linear-gradient(180deg, #fff8ea, #f7edd7);
  color: #8a5a16;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.01em;
}

.workbench-alert-pill--drawer {
  margin-left: 12px;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.metric-card {
  border: none;
  border-radius: 20px;
  padding: 18px;
  background: linear-gradient(135deg, rgba(22, 50, 79, 0.05), rgba(31, 111, 235, 0.08));
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.metric-card:hover,
.social-item:hover,
.content-card:hover {
  transform: translateY(-2px);
}

.metric-label {
  font-size: 13px;
  color: #6b7a90;
}

.metric-value {
  font-size: 28px;
  color: #16324f;
  line-height: 1;
}

.metric-footnote {
  font-size: 12px;
  color: #7a8aa0;
}

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 0.95fr);
  gap: 20px;
}

.section-card {
  padding: 24px;
}

.section-caption {
  color: #7b8aa0;
  font-size: 13px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.social-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.social-item {
  border: none;
  border-radius: 20px;
  background: #f8fbff;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.social-icon {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.social-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.social-copy strong {
  color: #16324f;
  font-size: 15px;
}

.social-copy span {
  color: #6b7a90;
  font-size: 13px;
  line-height: 1.5;
}

.social-arrow {
  color: #a2afc2;
  font-size: 20px;
}

.top-bar {
  display: flex;
  justify-content: flex-end;
  padding: 15px 20px 0;
}

.settings-icon {
  font-size: 28px;
  color: #666;
  cursor: pointer;
  transition: color 0.3s;
}

.settings-icon:hover {
  color: #7494ec;
}

.profile-section {
  padding: 10px 20px 20px;
  display: flex;
  flex-direction: column;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 25px;
}

.avatar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.avatar-container {
  position: relative;
  display: inline-block;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.edit-avatar-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #7494ec;
  color: white;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px solid white;
  font-size: 12px;
}

.follow-btn {
  background: #ff4757;
  color: white;
  border: none;
  border-radius: 15px;
  padding: 4px 15px;
  font-size: 12px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: background 0.3s;
}

.follow-btn:hover {
  background: #ff2442;
}

.follow-btn.following {
  background: #f0f0f0;
  color: #666;
}

.follow-btn.following:hover {
  background: #e4e4e4;
}

.profile-info {
  flex: 1;
}

.profile-info h2 {
  margin: 0 0 5px 0;
  font-size: 22px;
  font-weight: 600;
  color: #333;
}

.username {
  color: #888;
  font-size: 14px;
  margin: 0 0 8px 0;
}

.role-badge {
  display: inline-block;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  color: white;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.my-homepage-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #7494ec;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 18px;
  font-size: 13px;
  cursor: pointer;
  margin-top: 8px;
  transition: all 0.3s;
}

.my-homepage-btn:hover {
  background: #4facfe;
  transform: translateY(-2px);
}

.stats-box {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
  margin-bottom: 10px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  flex: 1;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #888;
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 30px;
  background-color: #eee;
}

/* 标签页*/
.content-section {
  padding: 0 20px 80px;
}

.section-block {
  margin-bottom: 25px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
  font-weight: 600;
}

.view-all {
  font-size: 13px;
  color: #888;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.chart-card {
  background: white;
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.chart-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.chart-header h4 {
  margin: 0;
  font-size: 15px;
  color: #333;
}

.trend {
  font-size: 12px;
  font-weight: bold;
}

.trend.up {
  color: #ff4757;
}

.chart-placeholder {
  height: 80px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
  padding-top: 10px;
}

.bar {
  flex: 1;
  background: linear-gradient(to top, #ff9a9e 0%, #fecfef 100%);
  border-radius: 4px 4px 0 0;
  opacity: 0.8;
  transition: opacity 0.3s;
}

.bar:hover {
  opacity: 1;
}

.content-card {
  background: white;
  border-radius: 12px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.content-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.card-info h4 {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #333;
}

.card-info p {
  margin: 0;
  font-size: 12px;
  color: #999;
}

/* 图标颜色 */
.icon-blue { color: #4facfe; }
.icon-yellow { color: #f6d365; }
.icon-purple { color: #a18cd1; }
.icon-pink { color: #ff9a9e; }
.icon-green { color: #84fab0; }
.icon-orange { color: #fa709a; }
.icon-red { color: #ff0844; }

/* 侧边抽屉 */
.settings-drawer .drawer-content {
  position: absolute;
  right: 0;
  top: 0;
  height: 100%;
  width: 280px;
  max-width: 80%;
  background: white;
  box-shadow: -2px 0 10px rgba(0,0,0,0.1);
  border-radius: 16px 0 0 16px;
  display: flex;
  flex-direction: column;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.drawer-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  font-size: 24px;
  color: #999;
  cursor: pointer;
}

.settings-drawer .menu-group {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.menu-item:hover {
  background-color: #f5f5f5;
}

.menu-item--with-alert {
  position: relative;
}

.menu-item--with-alert[data-alert]:not([data-alert=''])::after {
  content: attr(data-alert);
  margin-left: 12px;
  padding: 0 8px;
  min-width: 28px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid rgba(201, 145, 63, 0.28);
  background: linear-gradient(180deg, #fff8ea, #f7edd7);
  color: #8a5a16;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.menu-item i:first-child {
  font-size: 20px;
  color: #7494ec;
  margin-right: 15px;
  width: 24px;
  text-align: center;
}

.menu-item span {
  flex: 1;
  color: #333;
  font-size: 15px;
}

.menu-item .bx-chevron-right {
  color: #ccc;
  font-size: 20px;
}

.menu-item.text-danger span,
.menu-item.text-danger i:first-child {
  color: #ff4757;
}

.logout-section {
  padding: 20px;
  border-top: 1px solid #eee;
}

.logout-btn {
  width: 100%;
  padding: 12px;
  background: #f0f0f0;
  color: #333;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  margin-bottom: 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.delete-account-btn {
  width: 100%;
  padding: 12px;
  background: white;
  color: #ff4757;
  border: 1px solid #ff4757;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 30px;
  width: 90%;
  max-width: 400px;
  text-align: center;
}

.modal-content h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  font-weight: 600;
}

.modal-content p {
  margin: 0 0 20px 0;
  color: #666;
  font-size: 14px;
}

.account-manager-modal {
  max-width: 720px;
  padding: 0;
  text-align: left;
  overflow: hidden;
}

.account-manager-header {
  padding: 22px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

.account-manager-header h3 {
  margin: 0 0 6px;
  color: #16324f;
}

.account-manager-header p {
  margin: 0;
  color: #6b7a90;
}

.account-manager-body {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.9fr);
  gap: 0;
}

.saved-account-section,
.account-form-section {
  padding: 22px 24px 24px;
}

.account-form-section {
  border-left: 1px solid #eef2f7;
  background: #f8fbff;
}

.saved-account-head {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.saved-account-head strong {
  color: #16324f;
  font-size: 16px;
}

.saved-account-head span {
  color: #7b8aa0;
  font-size: 12px;
}

.saved-account-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.saved-account-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid #e5edf7;
  background: #fff;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.saved-account-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
  border-color: #cfe0f7;
}

.saved-account-item.active {
  border-color: #7494ec;
  box-shadow: 0 12px 24px rgba(116, 148, 236, 0.16);
}

.saved-account-item.switching {
  opacity: 0.72;
  cursor: progress;
}

.saved-account-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background: #edf3fb;
  flex-shrink: 0;
}

.saved-account-copy {
  flex: 1;
  min-width: 0;
}

.saved-account-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.saved-account-row strong {
  color: #16324f;
  font-size: 15px;
}

.saved-account-meta {
  display: block;
  margin-top: 4px;
  color: #7b8aa0;
  font-size: 13px;
  word-break: break-all;
}

.account-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(116, 148, 236, 0.14);
  color: #4b6fd6;
  font-size: 12px;
  font-weight: 600;
}

.account-switching-text {
  color: #7b8aa0;
  font-size: 12px;
  white-space: nowrap;
}

.remove-account-btn {
  border: none;
  border-radius: 999px;
  padding: 8px 12px;
  background: #fff2f2;
  color: #e05252;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
}

.account-empty-state {
  min-height: 220px;
  border: 1px dashed #d5e1ef;
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #8a97ab;
  background: #fbfdff;
}

.account-empty-state i {
  font-size: 36px;
}

.account-note {
  margin: -6px 0 0;
  color: #8a97ab;
  font-size: 12px;
  line-height: 1.6;
}

.account-manager-actions {
  justify-content: flex-end;
}

.modal-buttons {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn.secondary {
  background: #f0f0f0;
  color: #333;
}

.btn.primary {
  background: #7494ec;
  color: white;
}

.btn.danger {
  background: #ff4757;
  color: white;
}

.btn:hover {
  opacity: 0.9;
}

/* 表单样式 */
.form-group {
  margin-bottom: 20px;
  text-align: left;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-group input:focus {
  border-color: #7494ec;
}

.form-group input::placeholder {
  color: #999;
}

.error-text {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #ff4d4f;
}

@media (min-width: 768px) {
  .personal-center {
    width: 85%;
  }
}

@media (min-width: 1024px) {
  .personal-center {
    width: 70%;
  }
}

@media (max-width: 1024px) {
  .dashboard-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .personal-center {
    padding: 18px 14px 32px;
  }

  .page-header,
  .overview-main {
    flex-direction: column;
  }

  .primary-actions {
    width: 100%;
    min-width: 0;
  }

  .metric-strip,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .section-card,
  .overview-card {
    padding: 18px;
    border-radius: 22px;
  }

  .account-manager-body {
    grid-template-columns: 1fr;
  }

  .account-form-section {
    border-left: none;
    border-top: 1px solid #eef2f7;
  }

  .page-heading h1 {
    font-size: 28px;
  }

  .profile-copy h2 {
    font-size: 24px;
  }
}

/* 用户列表弹窗样式 */
.user-list-modal {
  max-width: 520px;
  max-height: 70vh;
  padding: 0;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
}

.modal-header .close-btn {
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.user-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 10px 0;
}

.visitor-sections {
  max-height: calc(70vh - 72px);
  overflow-y: auto;
  padding: 8px 0 12px;
}

.visitor-section + .visitor-section {
  border-top: 1px solid #eef2f7;
}

.visitor-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px 6px;
}

.visitor-section-head strong {
  color: #16324f;
  font-size: 15px;
}

.visitor-section-head span {
  color: #7b8aa0;
  font-size: 12px;
}

.visitor-list {
  max-height: 240px;
  padding-top: 4px;
}

.visitor-empty {
  min-height: 120px;
  padding: 24px 20px 30px;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  gap: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-item:hover {
  background-color: #f5f5f5;
}

.user-avatar {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.user-bio, .user-time {
  font-size: 12px;
  color: #999;
}

.empty-state, .loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
}

.empty-state i, .loading-state i {
  font-size: 40px;
  margin-bottom: 10px;
}

.empty-state p, .loading-state p {
  margin: 0;
  font-size: 14px;
}

.menu-list {
  margin-top: 20px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.menu-list .menu-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f5f5f5;
}

.menu-list .menu-item:last-child {
  border-bottom: none;
}

.menu-list .menu-item:hover {
  background-color: #f9f9f9;
}

.menu-list .menu-item i:first-child {
  font-size: 20px;
  color: #7494ec;
  margin-right: 15px;
  width: 24px;
  text-align: center;
}

.menu-list .menu-item span {
  flex: 1;
  color: #333;
  font-size: 15px;
}

.menu-list .menu-item .bx-chevron-right {
  color: #ccc;
  font-size: 20px;
}
</style>





