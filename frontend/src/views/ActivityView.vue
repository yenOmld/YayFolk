<template>
  <div class="activity-page">
    <div class="activity-header">
      <h1>精彩活动</h1>
      <p class="subtitle">探索非遗文化活动，传承传统文化</p>
    </div>

    <div class="main-container">
      <section class="activity-section">
        <h2>热门活动</h2>
        <div class="activity-list">
          <div class="activity-card">
            <div class="activity-image">
              <div class="image-placeholder">🎭</div>
            </div>
            <div class="activity-info">
              <h3>京剧艺术体验工作坊</h3>
              <div class="activity-meta">
                <span><i class='bx bx-calendar'></i> 2026-04-15</span>
                <span><i class='bx bx-map'></i> 北京·国家大剧院</span>
              </div>
              <p class="activity-desc">近距离接触京剧艺术，学习基本唱腔和身段，体验中国传统文化魅力</p>
              <div class="activity-footer">
                <span class="status">报名中</span>
                <button class="join-btn">立即报名</button>
              </div>
            </div>
          </div>

          <div class="activity-card">
            <div class="activity-image">
              <div class="image-placeholder">✂️</div>
            </div>
            <div class="activity-info">
              <h3>非遗剪纸技艺传承课</h3>
              <div class="activity-meta">
                <span><i class='bx bx-calendar'></i> 2026-04-20</span>
                <span><i class='bx bx-map'></i> 上海·文化广场</span>
              </div>
              <p class="activity-desc">跟随非遗传承人学习传统剪纸技艺，亲手制作精美剪纸作品</p>
              <div class="activity-footer">
                <span class="status">报名中</span>
                <button class="join-btn">立即报名</button>
              </div>
            </div>
          </div>

          <div class="activity-card">
            <div class="activity-image">
              <div class="image-placeholder">🍵</div>
            </div>
            <div class="activity-info">
              <h3>中国传统茶艺表演</h3>
              <div class="activity-meta">
                <span><i class='bx bx-calendar'></i> 2026-04-25</span>
                <span><i class='bx bx-map'></i> 杭州·西湖景区</span>
              </div>
              <p class="activity-desc">品味千年茶文化，欣赏专业茶艺表演，学习茶道礼仪</p>
              <div class="activity-footer">
                <span class="status hot">热门</span>
                <button class="join-btn">立即报名</button>
              </div>
            </div>
          </div>

          <div class="activity-card">
            <div class="activity-image">
              <div class="image-placeholder">🎵</div>
            </div>
            <div class="activity-info">
              <h3>古琴音乐欣赏会</h3>
              <div class="activity-meta">
                <span><i class='bx bx-calendar'></i> 2026-05-01</span>
                <span><i class='bx bx-map'></i> 成都·锦里古街</span>
              </div>
              <p class="activity-desc">聆听千年古琴之音，感受中国传统音乐的深邃与优雅</p>
              <div class="activity-footer">
                <span class="status">报名中</span>
                <button class="join-btn">立即报名</button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="activity-section">
        <h2>往期回顾</h2>
        <div class="past-activities">
          <div class="past-card">
            <div class="past-image">
              <div class="image-placeholder">🐉</div>
            </div>
            <div class="past-info">
              <h3>龙舞表演迎新春</h3>
              <p class="past-date">2026-02-10</p>
            </div>
          </div>
          <div class="past-card">
            <div class="past-image">
              <div class="image-placeholder">🏮</div>
            </div>
            <div class="past-info">
              <h3>元宵灯会非遗展示</h3>
              <p class="past-date">2026-02-24</p>
            </div>
          </div>
          <div class="past-card">
            <div class="past-image">
              <div class="image-placeholder">🎨</div>
            </div>
            <div class="past-info">
              <h3>传统年画制作体验</h3>
              <p class="past-date">2026-01-20</p>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.activity-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 75px;
}

.activity-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 30px 20px;
  text-align: center;
  color: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.activity-header h1 {
  font-size: 28px;
  margin-bottom: 8px;
  font-weight: bold;
}

.subtitle {
  font-size: 14px;
  opacity: 0.9;
}

.main-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.activity-section {
  margin-bottom: 25px;
}

.activity-section h2 {
  color: #333;
  font-size: 20px;
  margin-bottom: 15px;
  border-left: 4px solid #667eea;
  padding-left: 12px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.activity-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s;
}

.activity-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.activity-image {
  width: 100%;
  height: 180px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-placeholder {
  font-size: 80px;
}

.activity-info {
  padding: 15px;
}

.activity-info h3 {
  color: #333;
  font-size: 17px;
  margin-bottom: 10px;
  font-weight: 600;
}

.activity-meta {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
  color: #666;
  font-size: 13px;
}

.activity-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.activity-meta i {
  font-size: 16px;
  color: #667eea;
}

.activity-desc {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.activity-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status {
  background: #e8f5e9;
  color: #4caf50;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status.hot {
  background: #ffebee;
  color: #f44336;
}

.join-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: transform 0.2s;
}

.join-btn:hover {
  transform: scale(1.05);
}

.join-btn:active {
  transform: scale(0.98);
}

.past-activities {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.past-card {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s;
}

.past-card:hover {
  transform: translateY(-2px);
}

.past-image {
  width: 100%;
  height: 100px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.past-image .image-placeholder {
  font-size: 50px;
}

.past-info {
  padding: 10px;
  text-align: center;
}

.past-info h3 {
  color: #333;
  font-size: 13px;
  margin-bottom: 5px;
  font-weight: 500;
}

.past-date {
  color: #999;
  font-size: 11px;
}
</style>
