---
layout: home

hero:
  name: 在线考试系统
  text: 个人全栈技术实践项目
  tagline: 以考试业务为载体的 Spring Boot + Vue 3 全栈工程实践，覆盖题库管理、智能组卷、在线考试、自动阅卷、成绩分析、即时通讯完整闭环。所有数据均为演示数据。
  image:
    src: /hero-architecture.svg
    alt: 在线考试系统架构
  actions:
    - theme: brand
      text: 在线体验
      link: http://124.222.21.219
    - theme: alt
      text: GitHub 源码
      link: https://github.com/Awake-s/online-exam-system
    - theme: alt
      text: 查看落地页 →
      link: https://awake-s.github.io/online-exam-system/landing/
    - theme: alt
      text: 查看功能特性 →
      link: /features

features:
  - icon: 📚
    title: 智能题库管理
    details: 单选 / 多选 / 判断 / 填空 / 简答 5 种题型，支持富文本编辑、公式、图片，Excel（EasyExcel）批量导入导出，按知识点与难度分类管理。
    link: /features
    linkText: 了解更多
  - icon: 📝
    title: 灵活组卷
    details: 手动组卷 + 模板自动组卷双模式。模板组卷可按题型 / 难度 / 知识点设定抽题规则，一键随机生成多套平行试卷。
    link: /features
    linkText: 了解更多
  - icon: 🎓
    title: 在线考试
    details: 全屏答题、倒计时、答案自动保存、断网续答、切屏检测防作弊、到时自动交卷。客观题即时评分，主观题教师批阅。
    link: /features
    linkText: 了解更多
  - icon: ✅
    title: 自动 + 人工阅卷
    details: 客观题（单选 / 多选 / 判断 / 填空）系统自动批改，主观题（简答）教师在线评分，支持给分批注，成绩自动汇总。
    link: /features
    linkText: 了解更多
  - icon: 📊
    title: 多维成绩分析
    details: 班级成绩统计、题目正确率分析、分数段分布、成绩趋势图（ECharts）。辅助教师精准定位教学薄弱环节。
    link: /features
    linkText: 了解更多
  - icon: 💬
    title: 即时通讯与通知
    details: 基于 WebSocket（STOMP）的实时推送：考试发布、交卷提醒、成绩公布、师生一对一即时聊天，12 类通知全覆盖。
    link: /features
    linkText: 了解更多
---

<script setup>
// 首页底部自定义区块：数据亮点 + 项目历程（VitePress Home layout 不直接支持，
// 内容通过下方 Markdown 区块渲染，此处保留插槽以备后续扩展为 Vue 组件）
</script>

<!-- ============ 数据亮点区（板块五） ============ -->

<div class="stats-section">
  <h2 style="text-align:center;margin-top:48px;">用数据说话</h2>
  <p style="text-align:center;color:var(--vp-c-text-2);">基于 JMeter 5.6.3 全场景压测（v2.0 报告）与生产级配置实测</p>

  <div class="stats-grid">
    <div class="stat-card">
      <div class="stat-number">188</div>
      <div class="stat-label">并发用户压测</div>
      <div class="stat-desc">100 登录 + 43 考试全流程 + 10 教师 + 30 聊天</div>
    </div>
    <div class="stat-card">
      <div class="stat-number">321<span class="unit">ms</span></div>
      <div class="stat-label">P50 响应中位数</div>
      <div class="stat-desc">APDEX 0.75（T=500ms），错误率 0.00%</div>
    </div>
    <div class="stat-card">
      <div class="stat-number">34.7<span class="unit">req/s</span></div>
      <div class="stat-label">吞吐量 TPS</div>
      <div class="stat-desc">4 / 4 核心业务场景 100% 覆盖</div>
    </div>
    <div class="stat-card">
      <div class="stat-number">19<span class="unit">张表</span></div>
      <div class="stat-label">数据库设计</div>
      <div class="stat-desc">20 个 Controller · 完整 E-R 模型</div>
    </div>
  </div>
</div>

<!-- ============ 项目历程区（板块六） ============ -->

<div class="journey-section">
  <h2 style="text-align:center;">项目历程</h2>
  <p style="text-align:center;color:var(--vp-c-text-2);">个人全栈技术实践项目的演进历程</p>

  <div class="timeline">
    <div class="timeline-item">
      <div class="timeline-dot done"></div>
      <div class="timeline-content">
        <div class="timeline-date">2026.03 — 2026.06</div>
        <h3>全栈开发 · 完整闭环交付</h3>
        <p>完成需求分析、概要 / 详细设计、编码实现、系统测试（接口 + 性能压测）。覆盖题库、组卷、考试、阅卷、成绩、通知、聊天全闭环。</p>
      </div>
    </div>
    <div class="timeline-item">
      <div class="timeline-dot done"></div>
      <div class="timeline-content">
        <div class="timeline-date">2026.06</div>
        <h3>开源上线 · GitHub 作品集</h3>
        <p>清洗历史、外部化密钥、补齐 README / LICENSE / CI 流水线，作为个人技术作品集对外展示。</p>
      </div>
    </div>
    <div class="timeline-item">
      <div class="timeline-dot todo"></div>
      <div class="timeline-content">
        <div class="timeline-date">后续</div>
        <h3>持续学习与演进</h3>
        <p>探索 Docker 容器化、Spring Boot 3 升级与微服务架构，持续提升工程能力。</p>
      </div>
    </div>
  </div>
</div>

<!-- ============ 技术栈速览（板块四缩影，详见 /architecture） ============ -->

<div class="tech-section">
  <h2 style="text-align:center;">技术栈</h2>
  <p style="text-align:center;color:var(--vp-c-text-2);">主流、成熟、可演进 —— <a href="./architecture.html">查看完整架构 →</a></p>

  <div class="tech-grid">
    <div class="tech-item"><span class="tech-name">Spring Boot 2.7</span><span class="tech-role">后端框架</span></div>
    <div class="tech-item"><span class="tech-name">Vue 3.5 + TypeScript</span><span class="tech-role">前端框架</span></div>
    <div class="tech-item"><span class="tech-name">MyBatis-Plus 3.5</span><span class="tech-role">ORM</span></div>
    <div class="tech-item"><span class="tech-name">Spring Security + JWT</span><span class="tech-role">认证授权</span></div>
    <div class="tech-item"><span class="tech-name">MySQL 8 + Redis</span><span class="tech-role">存储 / 缓存</span></div>
    <div class="tech-item"><span class="tech-name">WebSocket (STOMP)</span><span class="tech-role">实时通信</span></div>
    <div class="tech-item"><span class="tech-name">Element Plus + Pinia</span><span class="tech-role">UI / 状态</span></div>
    <div class="tech-item"><span class="tech-name">Vite 7 + Tailwind</span><span class="tech-role">构建 / 样式</span></div>
  </div>
</div>

<style>
/* ====== 数据亮点区 ====== */
.stats-section { max-width: 1152px; margin: 0 auto; padding: 0 24px; }
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-top: 32px;
}
.stat-card {
  text-align: center;
  padding: 28px 20px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  background: var(--vp-c-bg-soft);
  transition: transform 0.2s, border-color 0.2s;
}
.stat-card:hover { transform: translateY(-4px); border-color: var(--vp-c-brand-1); }
.stat-number {
  font-size: 40px;
  font-weight: 800;
  color: var(--vp-c-brand-1);
  line-height: 1.1;
}
.stat-number .unit { font-size: 18px; font-weight: 600; margin-left: 4px; }
.stat-label { font-size: 16px; font-weight: 600; margin-top: 8px; }
.stat-desc { font-size: 13px; color: var(--vp-c-text-2); margin-top: 6px; line-height: 1.5; }

/* ====== 项目历程区 ====== */
.journey-section { max-width: 860px; margin: 64px auto 0; padding: 0 24px; }
.timeline { position: relative; padding-left: 32px; margin-top: 32px; }
.timeline::before {
  content: '';
  position: absolute; left: 8px; top: 8px; bottom: 8px;
  width: 2px; background: var(--vp-c-divider);
}
.timeline-item { position: relative; padding-bottom: 32px; }
.timeline-item:last-child { padding-bottom: 0; }
.timeline-dot {
  position: absolute; left: -32px; top: 4px;
  width: 18px; height: 18px; border-radius: 50%;
  border: 3px solid var(--vp-c-bg);
  box-shadow: 0 0 0 2px var(--vp-c-divider);
}
.timeline-dot.done { background: #3c6fff; }
.timeline-dot.doing { background: #f0a020; }
.timeline-dot.todo { background: var(--vp-c-bg-soft); border: 3px dashed var(--vp-c-divider); }
.timeline-date { font-size: 13px; color: var(--vp-c-brand-1); font-weight: 600; }
.timeline-content h3 { margin: 6px 0 8px; font-size: 18px; }
.timeline-content p { margin: 0; color: var(--vp-c-text-2); line-height: 1.6; }

/* ====== 技术栈区 ====== */
.tech-section { max-width: 1152px; margin: 64px auto 0; padding: 0 24px; }
.tech-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-top: 32px;
}
.tech-item {
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 10px;
  background: var(--vp-c-bg-soft);
}
.tech-name { font-weight: 600; font-size: 15px; }
.tech-role { font-size: 13px; color: var(--vp-c-text-2); margin-top: 4px; }
</style>
