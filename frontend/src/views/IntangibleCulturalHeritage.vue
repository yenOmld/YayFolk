<template>
  <div class="heritage-page">
    <div class="heritage-header">
      <h1>中国非遗文化</h1>
      <p class="subtitle">探索千年传承，感受文化魅力</p>
    </div>

    <div class="main-container">
      <section class="intro-section">
        <h2>什么是非物质文化遗产？</h2>
        <p>
          非物质文化遗产是指各族人民世代相传并视为其文化遗产组成部分的各种传统文化表现形式，
          以及与传统文化表现形式相关的实物和场所。
        </p>
      </section>

      <section class="categories-section">
        <h2>非遗分类</h2>
        <div class="category-grid">
          <div class="category-card">
            <div class="category-icon">🎭</div>
            <h3>传统戏剧</h3>
            <p>京剧、昆曲、越剧等</p>
          </div>
          <div class="category-card">
            <div class="category-icon">🎵</div>
            <h3>传统音乐</h3>
            <p>古琴艺术、蒙古族长调等</p>
          </div>
          <div class="category-card">
            <div class="category-icon">💃</div>
            <h3>传统舞蹈</h3>
            <p>龙舞、狮舞、秧歌等</p>
          </div>
          <div class="category-card">
            <div class="category-icon">🎨</div>
            <h3>传统美术</h3>
            <p>剪纸、刺绣、年画等</p>
          </div>
          <div class="category-card">
            <div class="category-icon">✋</div>
            <h3>传统技艺</h3>
            <p>陶瓷烧制、木雕、茶艺等</p>
          </div>
          <div class="category-card">
            <div class="category-icon">📖</div>
            <h3>民间文学</h3>
            <p>神话、传说、史诗等</p>
          </div>
        </div>
      </section>

      <section class="highlights-section">
        <h2>非遗亮点</h2>
        <div class="highlight-list">
          <div class="highlight-item">
            <div class="highlight-number">10</div>
            <div class="highlight-text">
              <h3>联合国非遗名录</h3>
              <p>中国共有 43 个项目列入联合国教科文组织非遗名录</p>
            </div>
          </div>
          <div class="highlight-item">
            <div class="highlight-number">1557</div>
            <div class="highlight-text">
              <h3>国家级非遗项目</h3>
              <p>国务院公布的国家级非物质文化遗产代表性项目</p>
            </div>
          </div>
        </div>
      </section>

      <section class="protection-section">
        <h2>非遗保护</h2>
        <p>
          保护非物质文化遗产，就是守护我们的精神家园。通过记录、传承、创新，
          让古老的文化在新时代焕发出新的生机与活力。
        </p>
        <div class="protection-goals">
          <div class="goal-item">
            <i class='bx bx-check-circle'></i>
            <span>记录保存</span>
          </div>
          <div class="goal-item">
            <i class='bx bx-check-circle'></i>
            <span>传承发展</span>
          </div>
          <div class="goal-item">
            <i class='bx bx-check-circle'></i>
            <span>宣传推广</span>
          </div>
          <div class="goal-item">
            <i class='bx bx-check-circle'></i>
            <span>国际交流</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.heritage-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 75px;
}

.heritage-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 30px 20px;
  text-align: center;
  color: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.heritage-header h1 {
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

.intro-section,
.categories-section,
.highlights-section,
.protection-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.intro-section h2,
.categories-section h2,
.highlights-section h2,
.protection-section h2 {
  color: #333;
  font-size: 20px;
  margin-bottom: 15px;
  border-left: 4px solid #667eea;
  padding-left: 12px;
}

.intro-section p,
.protection-section p {
  color: #666;
  line-height: 1.8;
  font-size: 14px;
  text-align: justify;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 15px;
}

.category-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  padding: 15px 12px;
  text-align: center;
  color: white;
  transition: transform 0.3s;
}

.category-card:hover {
  transform: translateY(-3px);
}

.category-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.category-card h3 {
  font-size: 15px;
  margin-bottom: 6px;
  font-weight: 600;
}

.category-card p {
  font-size: 11px;
  opacity: 0.9;
}

.highlight-list {
  margin-top: 15px;
}

.highlight-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  border-radius: 10px;
  margin-bottom: 12px;
  color: white;
}

.highlight-item:last-child {
  margin-bottom: 0;
}

.highlight-number {
  font-size: 40px;
  font-weight: bold;
  margin-right: 15px;
  min-width: 70px;
  text-align: center;
}

.highlight-text h3 {
  font-size: 16px;
  margin-bottom: 4px;
  font-weight: 600;
}

.highlight-text p {
  font-size: 12px;
  opacity: 0.9;
}

.protection-goals {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 15px;
}

.goal-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  color: #667eea;
}

.goal-item i {
  font-size: 20px;
}

.goal-item span {
  font-size: 13px;
  font-weight: 500;
}
</style>
