// ====================================================================
// 站点级内容与配置（集中管理，便于一处替换）
// 注意：以下内容均取自项目真实源码与压测报告，未含任何虚构能力。
// 带 TODO 的项需替换为你的真实值后再上线。
// ====================================================================

export const SITE = {
  name: '在线考试系统',
  // 一句话定位：说“做什么 / 给谁”，不玩玄乎口号
  title: '在线考试系统 · 个人全栈技术实践项目',
  description:
    '以考试业务为载体的 Spring Boot + Vue 3 全栈工程实践：题库管理、智能组卷、防作弊在线考试、自动阅卷、成绩分析与师生实时沟通。所有数据均为演示数据。',
  // 在线体验地址（已部署的后台系统）
  // ⚠️ 域名 examplatform.online 备案完成后，把这里换成 https://examplatform.online 即可
  // （Nginx server_name 为通配 _，IP 与域名命中同一 server，前端/后端零改动）
  demoUrl: 'http://124.222.21.219',
  // GitHub 源码仓库
  githubUrl: 'https://github.com/Awake-s/online-exam-system',
  // docs-site 项目官网地址（VitePress 文档站）
  docsUrl: 'https://awake-s.github.io/online-exam-system/',
  ogImage: '/og-image.svg',
  author: '陶展',
  year: 2026
}

// Demo 测试账号（项目种子数据中的真实账号；如对外公开建议改为只读/可重置专用账号）
export const DEMO_ACCOUNTS = [
  { role: '教师', username: 'wangjianhua', password: '123456' },
  { role: '学生', username: '2430107101', password: '123456' }
]

// 顶部导航锚点
export const NAV = [
  { label: '功能', href: '#features' },
  { label: '三端', href: '#roles' },
  { label: '流程', href: '#how' },
  { label: '技术', href: '#tech' },
  { label: 'FAQ', href: '#faq' }
]

// 证明条：均为压测报告 v2.0 / 真实源码统计数字
export const STATS = [
  { num: '188', unit: '并发', label: '全场景压测', desc: '100 登录 + 43 考试全流程 + 10 教师 + 30 聊天' },
  { num: '0.00', unit: '%', label: '系统级错误率', desc: '1015 样本无 500/超时，4.24% 为业务校验拒绝' },
  { num: '321', unit: 'ms', label: 'P50 响应中位数', desc: 'APDEX 0.75（T=500ms）' },
  { num: '19', unit: '张表', label: '数据库设计', desc: '20 个接口模块 · 4 类客观题自动判分' }
]

// 痛点（传统线下/Excel 方式）
export const PAINS = [
  { icon: 'layers', title: '组卷耗时', text: '手工挑题、排版、复制多套卷子，一场考试准备半天。' },
  { icon: 'copy', title: '难防抄袭', text: '同一套卷子人手一份，前后排、同桌答案高度雷同。' },
  { icon: 'pen', title: '阅卷繁重', text: '客观题靠人工对答案，主观题反复翻卷，成绩出得慢。' },
  { icon: 'trend-down', title: '分析缺失', text: '成绩散落在 Excel，正确率、分数段、趋势无从下手。' },
  { icon: 'bell', title: '通知靠吼', text: '考试发布、成绩公布只能群里 @，学生漏看是常态。' }
]

// 功能（一个主功能 + 五个支撑功能，组件按非对称 bento 渲染）
// primary: true 的项渲染为大卡，配产品界面还原图
export const FEATURES = [
  {
    icon: 'shield',
    title: '防作弊在线考试',
    benefit: '把考场纪律交给服务端',
    primary: true,
    points: [
      '进入即强制全屏，实时记录切屏次数',
      '作答每隔数秒自动暂存，刷新断网都能续答',
      '剩余时间由服务端核算，到点强制收卷，客户端改不动'
    ]
  },
  {
    icon: 'book',
    title: '题库管理',
    benefit: '一次录入，长期复用',
    points: [
      '单选 / 多选 / 判断 / 填空 / 简答 五种题型',
      '富文本编辑，支持公式与图片',
      'EasyExcel 批量导入导出，按科目与难度归档'
    ]
  },
  {
    icon: 'file-stack',
    title: '组卷',
    benefit: '一键拉出多套平行卷',
    points: [
      '手动逐题挑选，或按模板配比抽题',
      '随机生成等难度平行卷，同场考试不撞题',
      '试卷、试卷模板独立管理可沿用'
    ]
  },
  {
    icon: 'check-check',
    title: '自动 + 人工阅卷',
    benefit: '客观题交卷即出分',
    points: [
      '四类客观题提交瞬间判分',
      '简答题教师在线评分、写批注',
      '客观分与主观分自动合并，可复核重发'
    ]
  },
  {
    icon: 'bar-chart',
    title: '成绩分析',
    benefit: '薄弱点一眼可见',
    points: [
      '最高 / 最低 / 平均分、及格率、标准差',
      '分数段直方图与逐题正确率',
      '多次考试趋势折线，ECharts 渲染'
    ]
  },
  {
    icon: 'message',
    title: '实时通讯与通知',
    benefit: '消息当场送达',
    points: [
      'WebSocket（STOMP）师生一对一聊天',
      '考试发布、交卷提醒、成绩公布等业务通知',
      '已读管理与实时推送'
    ]
  }
]

// 三端角色
export const ROLES = [
  {
    key: 'admin',
    name: '管理员',
    desc: '管住系统与教务的全部基础数据。',
    items: ['用户账号 CRUD 与 Excel 批量导入', '专业 / 班级 / 科目维护', '科目—专业—教师任课关系', '关键指标仪表盘'],
    mock: 'dashboard'
  },
  {
    key: 'teacher',
    name: '教师',
    desc: '从出题到成绩跑完一整条教学闭环。',
    items: ['题库 / 试卷 / 试卷模板管理', '发布考试，关联试卷与班级', '自动判分 + 人工批注阅卷', '成绩管理与多维分析'],
    mock: 'grading'
  },
  {
    key: 'student',
    name: '学生',
    desc: '专注作答，成绩与错题一处看清。',
    items: ['我的考试：待考 / 进行中 / 已结束', '全屏作答、倒计时、自动保存', '成绩与逐题详情回看', '错题本，与任课教师直接沟通'],
    mock: 'exam'
  }
]

// 工作流
export const STEPS = [
  { n: '01', title: '出题', text: '录入 5 种题型，富文本/公式/图片，或 Excel 批量导入。' },
  { n: '02', title: '组卷', text: '手动挑题或模板抽题，一键生成多套平行卷。' },
  { n: '03', title: '发布', text: '关联试卷与班级，设定时间、时长与防作弊策略。' },
  { n: '04', title: '考试', text: '学生全屏作答，自动保存、切屏检测、到时收卷。' },
  { n: '05', title: '阅卷', text: '客观题自动判分，主观题在线评分与批注。' },
  { n: '06', title: '成绩', text: '自动汇总、发布通知，多维度可视化分析。' }
]

// 技术栈（面向工程师/面试官，全部与 pom.xml / package.json 一致）
export const TECH = [
  { name: 'Spring Boot 2.7', role: '后端框架' },
  { name: 'Vue 3.5 + TS', role: '前端框架' },
  { name: 'MyBatis-Plus 3.5', role: 'ORM · 逻辑删除' },
  { name: 'Spring Security + JWT', role: '无状态认证' },
  { name: 'MySQL 8 + Redis', role: '存储 / 缓存' },
  { name: 'WebSocket (STOMP)', role: '实时通信' },
  { name: 'Element Plus + Pinia', role: 'UI / 状态' },
  { name: 'Vite 7 + Tailwind', role: '构建 / 样式' }
]

// 工程亮点（差异化“加分项”）
export const HIGHLIGHTS = [
  { title: 'JWT 无状态 + Redis 黑名单', text: '登出即失效；Redis 故障自动降级内存，定时清理。' },
  { title: 'Redis 6 大业务场景', text: 'JWT 黑名单 / 消息幂等 / 权限缓存 / Typing 限流 / 撤回草稿 / 通知去重。' },
  { title: 'WebSocket 三层鉴权', text: '握手 + 通道 + Origin 白名单，配心跳保活。' },
  { title: '登录限流 + 安全响应头', text: 'Guava 应用层 + Nginx 5r/m，OWASP 推荐安全头。' },
  { title: 'Actuator 健康检查', text: '含 Redis 健康检查，生产端点收敛。' },
  { title: 'JMeter 全场景压测', text: '188 并发 / 1015 样本闭环报告，系统级错误率 0%。' }
]

// FAQ
export const FAQS = [
  {
    q: '可以在线体验吗？需要账号吗？',
    a: '可以。点击「在线体验」进入已部署的系统，使用页面提供的教师 / 学生 Demo 账号即可登录试用。'
  },
  {
    q: '项目开源吗？用了什么技术栈？',
    a: '基于 MIT 协议开源。后端 Spring Boot 2.7 + Spring Security + MyBatis-Plus + MySQL + Redis + WebSocket；前端 Vue 3 + TypeScript + Vite + Element Plus + Pinia。'
  },
  {
    q: '如何本地启动？',
    a: '后端配置 MySQL/Redis 后 mvn 启动；前端 npm install && npm run dev。详细步骤见 GitHub README 与文档区。'
  },
  {
    q: '防作弊具体怎么实现？',
    a: '进入考试强制全屏并记录切屏次数；答案定时自动保存，刷新可续答；剩余时间由服务端校验，到时强制交卷，避免客户端篡改。'
  },
  {
    q: '能二次开发 / 商用吗？',
    a: 'MIT 协议允许在保留版权声明的前提下自由使用、修改与分发。'
  }
]
