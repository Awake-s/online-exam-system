# 在线考试系统 — 产品需求文档（PRD）

| 属性 | 内容 |
|------|------|
| 产品名称 | 在线考试系统 |
| 文档版本 | V1.0 |
| 文档状态 | 已定稿 |
| 作者 | taozhan |
| 创建日期 | 2026-03 |
| 最后更新 | 2026-03-23 |

---

## 目录

1. [项目概述](#一项目概述)
2. [项目背景与目标](#二项目背景与目标)
3. [用户角色定义](#三用户角色定义)
4. [功能需求总览](#四功能需求总览)
5. [功能需求详细描述](#五功能需求详细描述)
6. [数据模型设计](#六数据模型设计)
7. [接口规格](#七接口规格)
8. [非功能需求](#八非功能需求)
9. [技术架构](#九技术架构)
10. [页面清单与路由设计](#十页面清单与路由设计)
11. [优先级定义](#十一优先级定义)
12. [验收标准](#十二验收标准)

---

## 一、项目概述

### 1.1 一句话描述

基于B/S架构的在线考试管理平台，支持管理员、教师、学生三种角色，覆盖题库管理、智能组卷、在线考试、自动阅卷、成绩分析、实时通知的完整考试业务闭环。

### 1.2 产品定位

以高校考试场景为业务原型的**Web端在线考试系统**，旨在将传统纸质考试流程数字化，提升出题、组卷、考试、阅卷、成绩管理的效率，降低人工成本。本项目为个人全栈技术实践项目，所有数据均为演示数据。

### 1.3 核心价值

| 价值维度 | 描述 |
|---------|------|
| 效率提升 | 客观题自动批改，成绩实时统计，减少教师批改工作量 |
| 流程标准化 | 统一的出题→组卷→发布→考试→阅卷→成绩发布流程 |
| 数据驱动 | 成绩分析、正确率统计、成绩趋势图，辅助教学决策 |
| 实时协作 | WebSocket实时通知，考试发布/交卷/成绩发布即时推送 |
| 公平防弊 | 切屏检测、考试时间控制、缺考自动标记 |

---

## 二、项目背景与目标

### 2.1 项目背景

传统纸质考试存在以下痛点：

1. **出卷效率低**：教师手工出卷、排版、印刷，耗时耗力
2. **批改成本高**：大量试卷人工批改，容易出错且耗时
3. **数据分析难**：成绩统计依赖Excel手工汇总，难以深度分析
4. **通知不及时**：考试安排、成绩发布依赖线下通知，信息滞后
5. **管理分散**：题库、试卷、成绩分散管理，缺乏统一平台

### 2.2 项目目标

| 目标编号 | 目标描述 | 可量化指标 |
|---------|---------|-----------|
| G-01 | 实现题库数字化管理 | 支持单选、多选、判断、填空、简答5种题型，支持Excel批量导入 |
| G-02 | 实现智能组卷 | 支持手动组卷和模板自动组卷两种方式 |
| G-03 | 实现在线考试 | 支持倒计时、自动保存、切屏检测、到时自动交卷 |
| G-04 | 实现自动阅卷 | 客观题（单选/多选/判断/填空）自动批改，主观题教师手动批改 |
| G-05 | 实现成绩分析 | 提供班级成绩统计、题目正确率、分数分布图、成绩趋势图 |
| G-06 | 实现实时通知 | 基于WebSocket的考试/交卷/成绩发布实时推送 |
| G-07 | 实现三角色权限隔离 | 管理员/教师/学生严格权限隔离，路由+API双重校验 |

### 2.3 不做的事情（Out of Scope）

| 编号 | 排除项 | 原因 |
|------|--------|------|
| OS-01 | 移动端APP | 当前版本仅支持Web端，响应式设计兼容笔记本和桌面 |
| OS-02 | 视频监考 | 需要额外硬件和带宽支持，不在本期范围 |
| OS-03 | 题目图片OCR识别 | 技术复杂度高，非核心需求 |
| OS-04 | 多租户/多学校 | 当前为单机构部署 |
| OS-05 | IE浏览器兼容 | Vue3不支持IE，符合行业趋势 |

---

## 三、用户角色定义

### 3.1 角色概览

| 角色 | 角色编码 | 角色ID | 描述 | 核心场景 |
|------|---------|--------|------|---------|
| 管理员 | ADMIN | 1 | 系统管理者，负责用户、教务基础数据管理 | 账号管理、班级/学科/专业维护、系统仪表盘 |
| 教师 | TEACHER | 2 | 教学执行者，负责考试全流程管理 | 出题、组卷、发布考试、阅卷、成绩管理 |
| 学生 | STUDENT | 3 | 考试参与者，参加考试并查看成绩 | 参加考试、查看成绩、错题复习 |

### 3.2 用户故事

#### 管理员用户故事

| 编号 | 用户故事 |
|------|---------|
| US-A01 | 作为管理员，我希望查看系统仪表盘（用户总数、教师数、学生数、考试数、试卷数、题目数），以便掌握系统整体运行状态 |
| US-A02 | 作为管理员，我希望管理用户账号（新增/编辑/删除/启用/禁用/重置密码），以便维护系统用户 |
| US-A03 | 作为管理员，我希望管理专业信息（新增/编辑/删除），以便组织教务基础数据 |
| US-A04 | 作为管理员，我希望管理班级信息（新增/编辑/删除/查看学生列表），以便组织学生分班 |
| US-A05 | 作为管理员，我希望管理学科信息（新增/编辑/删除/关联专业），以便建立学科体系 |

#### 教师用户故事

| 编号 | 用户故事 |
|------|---------|
| US-T01 | 作为教师，我希望查看教师仪表盘（我的题目数、试卷数、考试数、待阅卷数、待考试列表、快捷操作），以便快速掌握工作概况 |
| US-T02 | 作为教师，我希望管理题库（新增/编辑/删除/批量删除/Excel导入），支持5种题型（单选/多选/判断/填空/简答），以便积累和维护题目资源 |
| US-T03 | 作为教师，我希望手动组卷（创建试卷/选择题目/设置分值/排序），以便精确控制试卷内容 |
| US-T04 | 作为教师，我希望使用模板自动组卷（设置题型/数量/难度/分值规则，系统随机抽题），以便快速生成试卷 |
| US-T05 | 作为教师，我希望发布考试（选择试卷/班级/设置时间/防作弊配置），以便安排学生参加考试 |
| US-T06 | 作为教师，我希望阅卷批改（查看学生答卷/为主观题评分/添加批注），以便完成成绩评定 |
| US-T07 | 作为教师，我希望发布成绩并通知学生，以便学生及时查看考试结果 |
| US-T08 | 作为教师，我希望查看成绩分析（班级成绩统计/题目正确率/分数分布），以便了解教学效果 |
| US-T09 | 作为教师，我希望导出成绩为Excel文件，以便存档和汇报 |

#### 学生用户故事

| 编号 | 用户故事 |
|------|---------|
| US-S01 | 作为学生，我希望查看学生仪表盘（待考试数、已完成数、平均分、最高分、待考试列表、最近成绩、成绩趋势图），以便了解学习状况 |
| US-S02 | 作为学生，我希望查看"我的考试"列表，了解哪些考试待参加、进行中或已结束 |
| US-S03 | 作为学生，我希望进入考试页面答题（查看题目/选择答案/自动保存/倒计时/交卷），以便完成考试 |
| US-S04 | 作为学生，我希望查看"我的成绩"列表和成绩详情（每题得分/正确答案/教师批注），以便了解考试表现 |
| US-S05 | 作为学生，我希望使用"错题本"（按科目筛选/查看错题详情/移除已掌握题目），以便针对性复习 |
| US-S06 | 作为学生，我希望实时收到考试发布和成绩发布的通知，以便及时参加考试和查看成绩 |

#### 公共用户故事

| 编号 | 用户故事 |
|------|---------|
| US-C01 | 作为用户，我希望通过账号密码+滑块验证码登录系统，以便安全访问 |
| US-C02 | 作为用户，我希望修改个人信息（头像/姓名/邮箱/电话/密码），以便维护个人资料 |
| US-C03 | 作为用户，我希望查看通知消息列表（已读/未读/全部标记已读），以便获取系统通知 |
| US-C04 | 作为用户，我希望系统支持深色/浅色主题切换和多语言（中/英），以便获得舒适的使用体验 |

---

## 四、功能需求总览

### 4.1 功能模块矩阵

| 功能模块 | 管理员 | 教师 | 学生 | API前缀 |
|---------|--------|------|------|---------|
| 系统仪表盘 | ✅ | ✅ | ✅ | /api/dashboard |
| 用户管理 | ✅ | — | — | /api/user |
| 专业管理 | ✅ | — | — | /api/major |
| 班级管理 | ✅ | — | — | /api/class |
| 学科管理 | ✅ | — | — | /api/subject |
| 题库管理 | — | ✅ | — | /api/question |
| 试卷管理 | — | ✅ | — | /api/paper |
| 试卷模板 | — | ✅ | — | /api/template |
| 考试管理 | — | ✅ | — | /api/exam |
| 阅卷管理 | — | ✅ | — | /api/marking |
| 成绩管理 | — | ✅ | — | /api/score |
| 在线考试 | — | — | ✅ | /api/student/exam |
| 我的成绩 | — | — | ✅ | /api/score |
| 错题本 | — | — | ✅ | /api/wrong |
| 通知管理 | ✅ | ✅ | ✅ | /api/notification |
| 个人中心 | ✅ | ✅ | ✅ | /api/profile |
| 即时聊天 | ✅ | ✅ | ✅ | /api/chat |
| 登录认证 | ✅ | ✅ | ✅ | /api/auth |

### 4.2 功能统计

| 维度 | 数量 |
|------|------|
| 功能模块 | 18个 |
| API接口 | 68个 |
| 前端页面 | 21个业务页面 + 4个公共页面 |
| 数据表 | 19张 |
| 用户角色 | 3个 |

---

## 五、功能需求详细描述

### 5.1 登录认证模块

**模块描述**：系统入口，实现用户身份认证和会话管理。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-AUTH-01 | 用户登录 | 选择角色（管理员/教师/学生），输入账号密码，完成滑块验证码后登录 | P0 |
| F-AUTH-02 | 获取用户信息 | 根据JWT Token获取当前登录用户的基本信息和角色 | P0 |
| F-AUTH-03 | 退出登录 | 注销Token，清除前端状态，跳转到登录页 | P0 |
| F-AUTH-04 | 滑块验证码 | 登录时展示滑块验证码，拖拽完成验证，防止机器人暴力破解 | P0 |

#### 业务规则

- 登录密码使用BCrypt加密存储
- 认证采用JWT Token，有效期24小时
- 退出登录后Token加入黑名单，不可重复使用
- 连续登录失败5次，锁定账户15分钟（基于`LoginRateLimiter`实现）
- 用户名和姓名输入进行XSS检测拦截（`XssUtils`）

---

### 5.2 管理员 — 系统仪表盘

**模块描述**：管理员首页，展示系统全局运营数据。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-ADMIN-01 | 统计概览 | 展示用户总数、教师数、学生数、考试数、试卷数、题目数 | P0 |

#### 数据来源

调用 `GET /api/dashboard/admin` 获取统计数据。

---

### 5.3 管理员 — 用户管理

**模块描述**：管理系统中所有用户账号。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-USER-01 | 用户列表 | 分页展示用户，支持按用户名/姓名/角色/班级/状态筛选 | P0 |
| F-USER-02 | 新增用户 | 填写用户名/密码/姓名/角色/班级/性别/邮箱/电话，教师需关联班级和科目 | P0 |
| F-USER-03 | 编辑用户 | 修改用户基本信息，教师可更新班级和科目关联 | P0 |
| F-USER-04 | 删除用户 | 删除用户，教师有题目/试卷/考试关联数据时禁止删除，学生有考试记录时禁止删除 | P0 |
| F-USER-05 | 启用/禁用 | 切换用户状态，系统至少保留一个启用状态的管理员 | P1 |
| F-USER-06 | 重置密码 | 将用户密码重置为默认值`123456` | P1 |

#### 业务规则

- 用户名全局唯一
- 不能删除自己
- 教师角色需要关联班级列表（`teacher_class`）和科目列表（`teacher_subject`）
- 学生角色需要关联所属班级（`classId`）
- 新建用户自动发送欢迎通知
- 用户名和姓名输入进行XSS非法字符检测

---

### 5.4 管理员 — 教务管理

**模块描述**：管理专业、班级、学科等教务基础数据。

#### 5.4.1 专业管理

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-MAJOR-01 | 专业列表 | 分页展示专业，支持按名称搜索 | P0 |
| F-MAJOR-02 | 新增专业 | 输入专业名称和描述 | P0 |
| F-MAJOR-03 | 编辑专业 | 修改专业名称和描述 | P0 |
| F-MAJOR-04 | 删除专业 | 有关联班级时禁止删除 | P0 |

#### 5.4.2 班级管理

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-CLASS-01 | 班级列表 | 分页展示班级，支持按名称/年级/专业筛选 | P0 |
| F-CLASS-02 | 新增班级 | 输入班级名称、年级、所属专业、描述 | P0 |
| F-CLASS-03 | 编辑班级 | 修改班级信息 | P0 |
| F-CLASS-04 | 删除班级 | 有学生关联时禁止删除 | P0 |
| F-CLASS-05 | 查看学生 | 查看班级下的学生列表 | P1 |

#### 5.4.3 学科管理

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-SUBJ-01 | 学科列表 | 分页展示学科，支持按名称搜索 | P0 |
| F-SUBJ-02 | 新增学科 | 输入学科名称和描述，可关联多个专业 | P0 |
| F-SUBJ-03 | 编辑学科 | 修改学科信息和专业关联 | P0 |
| F-SUBJ-04 | 删除学科 | 有关联题目或试卷时禁止删除 | P0 |

#### 业务规则

- 专业-班级：一个专业下有多个班级（一对多）
- 学科-专业：多对多关系，通过`subject_major`中间表关联
- 级联保护：删除操作自动检查下游数据关联，有关联则阻止删除并提示原因

---

### 5.5 教师 — 题库管理

**模块描述**：教师维护题目资源库，支持5种题型。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-QUES-01 | 题目列表 | 分页展示题目，支持按学科/题型/难度/关键字筛选 | P0 |
| F-QUES-02 | 新增题目 | 选择学科、题型、填写题目内容、选项、答案、解析、分值、难度 | P0 |
| F-QUES-03 | 编辑题目 | 修改题目信息 | P0 |
| F-QUES-04 | 删除题目 | 逻辑删除（`deleted`字段置为1） | P0 |
| F-QUES-05 | 批量删除 | 批量逻辑删除选中题目 | P1 |
| F-QUES-06 | Excel导入 | 上传Excel文件批量导入题目 | P1 |
| F-QUES-07 | 查看详情 | 查看题目完整信息（含选项、答案、解析） | P0 |

#### 题型定义

| 题型编码 | 题型名称 | 选项格式 | 答案格式 | 自动批改 |
|---------|---------|---------|---------|---------|
| 1 | 单选题 | JSON数组 `[{label:"A",content:"..."}]` | 单个字母，如"A" | ✅ 精确匹配 |
| 2 | 多选题 | JSON数组 `[{label:"A",content:"..."}]` | 多个字母逗号分隔，如"A,B,C" | ✅ 精确匹配 |
| 3 | 判断题 | 无（固定"正确/错误"） | "正确"或"错误" | ✅ 精确匹配 |
| 4 | 填空题 | 无 | 标准答案文本 | ✅ 忽略大小写匹配 |
| 5 | 简答题 | 无 | 参考答案文本 | ❌ 教师手动批改 |

#### 难度定义

| 难度值 | 含义 |
|--------|------|
| 1 | 简单 |
| 2 | 中等 |
| 3 | 困难 |

#### 业务规则

- 教师只能查看和管理自己创建的题目（`creatorId`隔离）
- 删除为逻辑删除，不影响已组卷的试卷
- 选项字段以JSON字符串存储

---

### 5.6 教师 — 试卷管理

**模块描述**：教师组建试卷，支持手动和模板自动两种组卷方式。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-PAPER-01 | 试卷列表 | 分页展示试卷，支持按名称/学科/状态筛选 | P0 |
| F-PAPER-02 | 创建试卷 | 手动组卷：输入名称/学科/及格分/时长，进入编辑页选择题目和设置分值 | P0 |
| F-PAPER-03 | 编辑试卷 | 进入试卷编辑页面，增删题目、调整分值和排序 | P0 |
| F-PAPER-04 | 模板组卷 | 选择模板，系统按规则随机抽题生成试卷 | P1 |
| F-PAPER-05 | 发布/取消发布 | 切换试卷发布状态，发布后方可用于考试 | P0 |
| F-PAPER-06 | 删除试卷 | 已被考试引用的试卷禁止删除 | P0 |
| F-PAPER-07 | 查看详情 | 查看试卷所有题目、分值、总分 | P0 |

#### 试卷模板功能

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-TMPL-01 | 模板列表 | 展示教师创建的组卷模板 | P1 |
| F-TMPL-02 | 创建模板 | 设置模板名称/学科/目标总分/及格分/时长/抽题规则 | P1 |
| F-TMPL-03 | 模板规则 | 每条规则定义：题型+数量+每题分值+难度 | P1 |
| F-TMPL-04 | 编辑模板 | 修改模板信息和规则 | P1 |
| F-TMPL-05 | 删除模板 | 删除不再使用的模板 | P1 |

#### 业务规则

- 试卷-题目关联通过`exam_paper_question`中间表，包含`questionId`、`score`（分值）、`sortOrder`（排序）
- 试卷总分自动计算为所有题目分值之和
- 教师只能管理自己创建的试卷（`creatorId`隔离）
- 模板组卷时从教师题库中按条件随机抽题，题库不足时提示

---

### 5.7 教师 — 考试管理

**模块描述**：教师发布和管理考试。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-EXAM-01 | 考试列表 | 分页展示教师创建的考试，显示状态（未开始/进行中/已结束） | P0 |
| F-EXAM-02 | 发布考试 | 选择已发布试卷、指定班级、设置考试开始/结束时间 | P0 |
| F-EXAM-03 | 编辑考试 | 修改考试信息（未开始的考试可修改） | P1 |
| F-EXAM-04 | 删除考试 | 删除考试及关联考试记录 | P1 |
| F-EXAM-05 | 查看考情 | 查看某场考试的所有学生答卷记录（参考/缺考/分数） | P0 |

#### 考试状态定义

| 状态值 | 含义 | 判断逻辑 |
|--------|------|---------|
| — | 未开始 | 当前时间 < startTime |
| — | 进行中 | startTime ≤ 当前时间 ≤ endTime |
| — | 已结束 | 当前时间 > endTime |

#### 业务规则

- 发布考试自动向班级内所有学生推送WebSocket通知
- 考试结束后，定时任务自动为未交卷学生创建缺考记录（`status=4`）
- 教师只能管理自己创建的考试（`creatorId`隔离）
- 教师只能选择自己管理的班级发布考试

---

### 5.8 教师 — 阅卷管理

**模块描述**：教师对学生答卷进行批改评分。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-MARK-01 | 待阅卷列表 | 展示某场考试的所有答卷记录 | P0 |
| F-MARK-02 | 阅卷详情 | 查看学生的每道题答案，与正确答案对比，为主观题评分和添加批注 | P0 |
| F-MARK-03 | 提交评分 | 批量提交各题评分和批注，系统自动计算总分 | P0 |
| F-MARK-04 | 发布成绩 | 整场考试批改完毕后，一键发布成绩并通知学生 | P0 |

#### 考试记录状态

| status值 | 含义 |
|----------|------|
| 0 | 考试中（未交卷） |
| 1 | 已交卷（待批改） |
| 2 | 已交卷（客观题已自动批改） |
| 3 | 已批改（教师已评分） |
| 4 | 缺考 |

#### 业务规则

- 学生交卷后，客观题（单选/多选/判断/填空）自动批改评分
- 简答题需教师手动评分和添加批注（`comment`字段）
- 发布成绩后通过WebSocket推送成绩通知给学生
- 发布成绩时自动将错题写入学生的错题本

---

### 5.9 教师 — 成绩管理

**模块描述**：教师查看和分析考试成绩。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-SCORE-01 | 班级成绩 | 查看某场考试所有学生的成绩列表 | P0 |
| F-SCORE-02 | 成绩分析 | 查看班级平均分、最高分、最低分、通过率、题目正确率、分数分布图 | P1 |
| F-SCORE-03 | 导出成绩 | 将某场考试的成绩导出为Excel文件（EasyExcel生成） | P1 |

---

### 5.10 学生 — 系统仪表盘

**模块描述**：学生首页，展示学习概况和待办。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-STU-HOME-01 | 统计卡片 | 展示待考试数、已完成数、平均分、最高分 | P0 |
| F-STU-HOME-02 | 待考试列表 | 展示即将开始和进行中的考试 | P0 |
| F-STU-HOME-03 | 最近成绩 | 展示最近考试的成绩记录 | P1 |
| F-STU-HOME-04 | 成绩趋势图 | ECharts折线图展示历次考试成绩趋势 | P1 |

---

### 5.11 学生 — 在线考试

**模块描述**：学生参加在线考试的核心流程。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-EXAM-S01 | 我的考试 | 展示学生所属班级的所有考试，区分待参加/进行中/已结束/已交卷 | P0 |
| F-EXAM-S02 | 开始考试 | 进入全屏答题页面，加载试卷题目 | P0 |
| F-EXAM-S03 | 答题 | 逐题或跳题作答，支持题目导航面板 | P0 |
| F-EXAM-S04 | 自动保存 | 每60秒自动保存当前答案到服务器 | P0 |
| F-EXAM-S05 | 倒计时 | 页面顶部显示考试剩余时间，到时自动交卷 | P0 |
| F-EXAM-S06 | 手动交卷 | 学生主动提交试卷，二次确认后提交 | P0 |
| F-EXAM-S07 | 切屏检测 | 检测浏览器标签页切换，记录切屏次数（`switchCount`） | P1 |
| F-EXAM-S08 | 查看结果 | 交卷后查看考试结果（成绩发布后可查看详细评分） | P0 |

#### 业务规则

- 考试页面为全屏模式（`isFullPage: true`），隐藏侧边栏和Tab栏
- 已交卷的考试不可重复进入，按钮变为"查看成绩"
- 自动保存使用upsert逻辑（有则更新，无则插入），附带时间戳避免旧数据覆盖新数据
- 交卷后客观题立即自动批改

---

### 5.12 学生 — 成绩与错题本

**模块描述**：学生查看考试成绩和错题复习。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-SCORE-S01 | 我的成绩 | 分页展示个人所有考试成绩记录 | P0 |
| F-SCORE-S02 | 成绩详情 | 查看某次考试的每道题答案、得分、正确答案、教师批注 | P0 |
| F-WRONG-01 | 科目筛选 | 按科目筛选错题列表 | P1 |
| F-WRONG-02 | 错题列表 | 展示答错的题目，包含题目内容、我的答案、正确答案 | P0 |
| F-WRONG-03 | 错题详情 | 弹窗展示题目详细信息和解析 | P0 |
| F-WRONG-04 | 移除错题 | 将已掌握的题目从错题本中移除（`isRemoved`置为1） | P1 |

#### 业务规则

- 错题来源于`exam_answer`表中`isCorrect=0`且`isRemoved=0`的记录
- 学生只能查看自己的成绩（水平越权防护）

---

### 5.13 通知管理

**模块描述**：系统消息通知，支持WebSocket实时推送和轮询。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-NOTI-01 | 通知列表 | 分页展示当前用户的通知消息 | P0 |
| F-NOTI-02 | 未读数量 | 顶部导航栏Badge显示未读通知数量 | P0 |
| F-NOTI-03 | 标记已读 | 点击通知标记为已读 | P0 |
| F-NOTI-04 | 全部已读 | 一键标记所有通知为已读 | P1 |
| F-NOTI-05 | 待办事项 | 展示教师的待阅卷数和待处理事项 | P1 |

#### 通知类型

| 类型编码 | 触发场景 | 接收者 |
|---------|---------|--------|
| EXAM_PUBLISHED | 教师发布考试 | 班级内所有学生 |
| EXAM_SUBMITTED | 学生交卷 | 出卷教师 |
| SCORE_PUBLISHED | 教师发布成绩 | 参加考试的学生 |
| ACCOUNT_CREATED | 管理员创建用户 | 新用户 |
| USER_CREATED | 管理员创建用户 | 其他管理员 |

#### WebSocket机制

- 连接端点：`/ws/notification`（SockJS + STOMP协议）
- 用户订阅：`/user/queue/notification`
- 心跳保活：STOMP内置心跳（10s/10s）
- 断线重连：指数退避策略（1s → 2s → 4s → 8s → 16s → 30s），最多10次
- 降级方案：WebSocket不可用时自动切换为HTTP轮询（每30秒）

---

### 5.14 个人中心

**模块描述**：用户管理个人信息。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-PROF-01 | 查看个人信息 | 展示用户名、姓名、角色、邮箱、电话、头像 | P0 |
| F-PROF-02 | 修改个人信息 | 修改姓名、邮箱、电话、性别 | P0 |
| F-PROF-03 | 修改密码 | 输入旧密码和新密码，旧密码验证通过后修改 | P0 |
| F-PROF-04 | 上传头像 | 上传图片作为个人头像 | P1 |

#### 业务规则

- 修改密码时新密码不能与旧密码相同
- 头像上传支持jpg/jpeg/png/gif格式，文件扩展名校验

---

### 5.15 即时聊天

**模块描述**：系统内用户间的即时消息通信。

#### 功能清单

| 功能ID | 功能名称 | 描述 | 优先级 |
|--------|---------|------|--------|
| F-CHAT-01 | 联系人列表 | 展示可发消息的用户列表 | P2 |
| F-CHAT-02 | 会话列表 | 展示历史聊天会话，显示最后一条消息 | P2 |
| F-CHAT-03 | 聊天消息 | 查看会话中的历史消息，支持分页加载 | P2 |
| F-CHAT-04 | 发送消息 | 发送文本消息，通过WebSocket实时推送 | P2 |
| F-CHAT-05 | 已读标记 | 标记会话消息为已读 | P2 |
| F-CHAT-06 | 未读计数 | 显示未读消息总数 | P2 |
| F-CHAT-07 | 在线状态 | 查看用户是否在线 | P2 |

---

## 六、数据模型设计

### 6.1 数据表清单

| 序号 | 表名 | 中文名 | 所属模块 | 记录数量级 |
|------|------|--------|---------|-----------|
| 1 | sys_user | 用户表 | 系统管理 | 百级 |
| 2 | sys_role | 角色表 | 系统管理 | 固定3条 |
| 3 | sys_notification | 通知表 | 通知管理 | 千级 |
| 4 | edu_major | 专业表 | 教务管理 | 十级 |
| 5 | edu_class | 班级表 | 教务管理 | 十级 |
| 6 | edu_subject | 学科表 | 教务管理 | 十级 |
| 7 | subject_major | 学科-专业关联表 | 教务管理 | 十级 |
| 8 | teacher_class | 教师-班级关联表 | 系统管理 | 十级 |
| 9 | teacher_subject | 教师-科目关联表 | 系统管理 | 十级 |
| 10 | exam_question | 题目表 | 题库管理 | 百~千级 |
| 11 | exam_paper | 试卷表 | 试卷管理 | 十~百级 |
| 12 | exam_paper_question | 试卷-题目关联表 | 试卷管理 | 百~千级 |
| 13 | exam_paper_template | 试卷模板表 | 试卷管理 | 十级 |
| 14 | exam_template_rule | 模板规则表 | 试卷管理 | 十级 |
| 15 | exam_exam | 考试表 | 考试管理 | 十~百级 |
| 16 | exam_record | 考试记录表 | 考试管理 | 百~千级 |
| 17 | exam_answer | 答题记录表 | 考试管理 | 千~万级 |
| 18 | chat_conversation | 聊天会话表 | 即时聊天 | 十级 |
| 19 | chat_message | 聊天消息表 | 即时聊天 | 百级 |

### 6.2 核心表结构

#### sys_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| username | VARCHAR | 用户名（唯一） |
| password | VARCHAR | 密码（BCrypt加密） |
| real_name | VARCHAR | 真实姓名 |
| avatar | VARCHAR | 头像路径 |
| email | VARCHAR | 邮箱 |
| phone | VARCHAR | 手机号 |
| gender | INT | 性别（0女/1男） |
| role_id | BIGINT | 角色ID（1管理员/2教师/3学生） |
| class_id | BIGINT | 所属班级ID（学生专用） |
| status | INT | 状态（0禁用/1启用） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### exam_question（题目表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| subject_id | BIGINT | 所属学科ID |
| question_type | INT | 题型（1单选/2多选/3判断/4填空/5简答） |
| content | TEXT | 题目内容 |
| options | TEXT | 选项（JSON字符串） |
| answer | TEXT | 正确答案 |
| analysis | TEXT | 解析 |
| score | DECIMAL | 默认分值 |
| difficulty | INT | 难度（1简单/2中等/3困难） |
| creator_id | BIGINT | 创建教师ID |
| deleted | INT | 逻辑删除（0正常/1已删除） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### exam_exam（考试表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| exam_name | VARCHAR | 考试名称 |
| paper_id | BIGINT | 试卷ID |
| class_id | BIGINT | 班级ID |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| creator_id | BIGINT | 创建教师ID |
| status | INT | 状态 |
| score_published | INT | 成绩是否已发布（0未发布/1已发布） |
| anti_cheat_config | TEXT | 防作弊配置（JSON） |
| create_time | DATETIME | 创建时间 |

#### exam_record（考试记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| exam_id | BIGINT | 考试ID |
| user_id | BIGINT | 学生ID |
| paper_id | BIGINT | 试卷ID |
| start_time | DATETIME | 开始作答时间 |
| submit_time | DATETIME | 交卷时间 |
| total_score | DECIMAL | 总分 |
| objective_score | DECIMAL | 客观题得分 |
| subjective_score | DECIMAL | 主观题得分 |
| status | INT | 状态（0考试中/1已交卷/2自动批改完/3已批改/4缺考） |
| switch_count | INT | 切屏次数 |
| create_time | DATETIME | 创建时间 |

#### exam_answer（答题记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| record_id | BIGINT | 考试记录ID |
| question_id | BIGINT | 题目ID |
| answer | TEXT | 学生答案 |
| is_correct | INT | 是否正确（0错/1对/NULL未批改） |
| score | DECIMAL | 得分 |
| comment | TEXT | 教师批注 |
| is_removed | INT | 是否从错题本移除（0未移除/1已移除） |
| create_time | DATETIME | 创建时间 |

### 6.3 核心实体关系

```
sys_role 1──N sys_user
edu_major 1──N edu_class
edu_subject N──M edu_major (通过 subject_major)
sys_user(教师) N──M edu_class (通过 teacher_class)
sys_user(教师) N──M edu_subject (通过 teacher_subject)
sys_user(学生) N──1 edu_class
edu_subject 1──N exam_question
sys_user(教师) 1──N exam_question
exam_paper 1──N exam_paper_question N──1 exam_question
exam_paper_template 1──N exam_template_rule
exam_exam N──1 exam_paper
exam_exam N──1 edu_class
exam_exam 1──N exam_record N──1 sys_user(学生)
exam_record 1──N exam_answer N──1 exam_question
sys_user 1──N sys_notification
chat_conversation ──── sys_user(user1) + sys_user(user2)
chat_conversation 1──N chat_message
```

---

## 七、接口规格

### 7.1 接口总览

系统采用RESTful API设计规范，所有接口以 `/api` 为前缀，返回统一JSON格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

### 7.2 认证接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | /api/auth/login | 用户登录 | 否 |
| GET | /api/auth/info | 获取当前用户信息 | 是 |
| POST | /api/auth/logout | 退出登录 | 是 |
| GET | /api/auth/captcha | 获取滑块验证码 | 否 |

### 7.3 用户管理接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/user/list | 用户列表（分页） | ADMIN |
| POST | /api/user/add | 新增用户 | ADMIN |
| PUT | /api/user/update | 修改用户 | ADMIN |
| DELETE | /api/user/{id} | 删除用户 | ADMIN |
| PUT | /api/user/status/{id} | 启用/禁用用户 | ADMIN |
| PUT | /api/user/reset-password/{id} | 重置密码 | ADMIN |

### 7.4 教务管理接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/major/list | 专业列表 | ADMIN |
| POST | /api/major/add | 新增专业 | ADMIN |
| PUT | /api/major/update/{id} | 修改专业 | ADMIN |
| DELETE | /api/major/{id} | 删除专业 | ADMIN |
| GET | /api/major/all | 所有专业（下拉） | ADMIN |
| GET | /api/class/list | 班级列表 | ADMIN |
| POST | /api/class/add | 新增班级 | ADMIN |
| PUT | /api/class/update/{id} | 修改班级 | ADMIN |
| DELETE | /api/class/{id} | 删除班级 | ADMIN |
| GET | /api/class/students/{classId} | 班级学生列表 | ADMIN |
| GET | /api/class/all | 所有班级 | ADMIN |
| GET | /api/class/my | 我的班级（教师） | TEACHER |
| GET | /api/subject/list | 学科列表 | ADMIN |
| POST | /api/subject/add | 新增学科 | ADMIN |
| PUT | /api/subject/update/{id} | 修改学科 | ADMIN |
| DELETE | /api/subject/{id} | 删除学科 | ADMIN |
| GET | /api/subject/all | 所有学科（下拉） | ALL |

### 7.5 题库管理接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/question/list | 题目列表（分页） | TEACHER |
| GET | /api/question/{id} | 题目详情 | TEACHER |
| POST | /api/question/add | 新增题目 | TEACHER |
| PUT | /api/question/update/{id} | 修改题目 | TEACHER |
| DELETE | /api/question/{id} | 删除题目 | TEACHER |
| DELETE | /api/question/batch | 批量删除 | TEACHER |
| POST | /api/question/import | Excel导入 | TEACHER |

### 7.6 试卷管理接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/paper/list | 试卷列表 | TEACHER |
| GET | /api/paper/{id} | 试卷详情 | TEACHER |
| POST | /api/paper/add | 创建试卷 | TEACHER |
| PUT | /api/paper/update/{id} | 修改试卷 | TEACHER |
| POST | /api/paper/random | 模板组卷 | TEACHER |
| DELETE | /api/paper/{id} | 删除试卷 | TEACHER |
| PUT | /api/paper/togglePublish/{id} | 发布/取消发布 | TEACHER |
| GET | /api/template/list | 模板列表 | TEACHER |
| GET | /api/template/{id} | 模板详情 | TEACHER |
| POST | /api/template/add | 创建模板 | TEACHER |
| PUT | /api/template/update/{id} | 修改模板 | TEACHER |
| DELETE | /api/template/{id} | 删除模板 | TEACHER |

### 7.7 考试管理接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/exam/list | 考试列表 | TEACHER |
| POST | /api/exam/add | 发布考试 | TEACHER |
| PUT | /api/exam/update/{id} | 修改考试 | TEACHER |
| DELETE | /api/exam/{id} | 删除考试 | TEACHER |
| GET | /api/exam/records/{examId} | 考试记录 | TEACHER |

### 7.8 在线考试接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/student/exam/my-exams | 我的考试列表 | STUDENT |
| GET | /api/student/exam/start/{examId} | 开始考试 | STUDENT |
| POST | /api/student/exam/submit | 交卷 | STUDENT |
| GET | /api/student/exam/result/{recordId} | 考试结果 | STUDENT |
| POST | /api/student/exam/auto-save | 自动保存答案 | STUDENT |
| POST | /api/student/exam/switch-screen/{recordId} | 记录切屏 | STUDENT |

### 7.9 阅卷与成绩接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/marking/list/{examId} | 待阅卷列表 | TEACHER |
| GET | /api/marking/detail/{recordId} | 阅卷详情 | TEACHER |
| POST | /api/marking/score | 提交评分 | TEACHER |
| POST | /api/marking/publish/{examId} | 发布成绩 | TEACHER |
| GET | /api/score/my-scores | 我的成绩 | STUDENT |
| GET | /api/score/class/{examId} | 班级成绩 | TEACHER |
| GET | /api/score/analysis/{examId} | 成绩分析 | TEACHER |
| GET | /api/score/export/{examId} | 导出Excel | TEACHER |

### 7.10 错题本接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/wrong/subjects | 错题科目列表 | STUDENT |
| GET | /api/wrong/list | 错题列表 | STUDENT |
| GET | /api/wrong/detail/{answerId} | 错题详情 | STUDENT |
| DELETE | /api/wrong/{answerId} | 移除错题 | STUDENT |

### 7.11 通知与仪表盘接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/notification/list | 通知列表 | ALL |
| GET | /api/notification/unread-count | 未读数量 | ALL |
| PUT | /api/notification/read/{id} | 标记已读 | ALL |
| PUT | /api/notification/read-all | 全部已读 | ALL |
| GET | /api/notification/pending | 待办事项 | ALL |
| GET | /api/dashboard/admin | 管理员仪表盘 | ADMIN |
| GET | /api/dashboard/teacher | 教师仪表盘 | TEACHER |
| GET | /api/dashboard/student | 学生仪表盘 | STUDENT |
| GET | /api/dashboard/student-trend | 成绩趋势 | STUDENT |

### 7.12 个人中心接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/profile/info | 个人信息 | ALL |
| PUT | /api/profile/update | 修改信息 | ALL |
| PUT | /api/profile/password | 修改密码 | ALL |
| POST | /api/profile/avatar | 上传头像 | ALL |

### 7.13 即时聊天接口

| 方法 | 路径 | 描述 | 角色 |
|------|------|------|------|
| GET | /api/chat/contacts | 联系人列表 | ALL |
| GET | /api/chat/conversations | 会话列表 | ALL |
| GET | /api/chat/conversations/{id}/messages | 历史消息 | ALL |
| POST | /api/chat/messages | 发送消息 | ALL |
| PUT | /api/chat/conversations/{id}/read | 标记已读 | ALL |
| GET | /api/chat/unread-count | 未读数量 | ALL |
| POST | /api/upload/image | 上传图片 | ALL |
| GET | /api/chat/online-status/{userId} | 在线状态 | ALL |

---

## 八、非功能需求

### 8.1 性能需求

| 编号 | 需求 | 指标 |
|------|------|------|
| NFR-P01 | API响应时间 | 所有接口 ≤500ms（实测平均54ms） |
| NFR-P02 | 并发支持 | 支持20+并发请求，100%成功率 |
| NFR-P03 | 分页查询 | 支持大数据量分页，配置maxLimit=500防止超大查询 |
| NFR-P04 | 页面加载 | 首页加载 ≤3s（实测 ≤2s） |

### 8.2 安全需求

| 编号 | 需求 | 实现方案 |
|------|------|---------|
| NFR-S01 | 身份认证 | JWT Token + Spring Security |
| NFR-S02 | 密码存储 | BCrypt加密 |
| NFR-S03 | SQL注入防护 | MyBatis-Plus参数化查询 |
| NFR-S04 | XSS防护 | 后端XssUtils输入检测 + 前端Vue3自动转义 |
| NFR-S05 | 暴力破解防护 | 5次失败锁定15分钟 |
| NFR-S06 | 越权防护 | 路由角色校验 + API @PreAuthorize注解 + 数据creatorId隔离 |
| NFR-S07 | Token安全 | 退出登录Token加入黑名单 |
| NFR-S08 | 点击劫持防护 | X-Frame-Options: SAMEORIGIN |
| NFR-S09 | 敏感信息 | API返回不包含密码字段，JWT不含敏感信息 |
| NFR-S10 | 文件上传安全 | 文件扩展名白名单校验 |

### 8.3 兼容性需求

| 编号 | 需求 | 说明 |
|------|------|------|
| NFR-C01 | 浏览器兼容 | Chrome 90+、Firefox 90+、Edge 90+、Safari 14+ |
| NFR-C02 | 分辨率适配 | 1024×768、1366×768、1920×1080 |
| NFR-C03 | 响应式布局 | 侧边栏折叠/展开、内容区自适应缩放 |
| NFR-C04 | 不兼容 | IE 11（Vue3不支持） |

### 8.4 可用性需求

| 编号 | 需求 | 说明 |
|------|------|------|
| NFR-U01 | 全中文界面 | 所有提示、标签、错误信息使用中文 |
| NFR-U02 | 表单验证 | 必填字段实时校验，错误提示明确 |
| NFR-U03 | 操作确认 | 删除等危险操作弹出二次确认对话框 |
| NFR-U04 | 空状态提示 | 无数据时显示友好的空状态提示 |
| NFR-U05 | 加载反馈 | 数据加载时显示loading状态 |
| NFR-U06 | 主题切换 | 支持深色/浅色主题切换 |
| NFR-U07 | 国际化 | 支持中文/英文界面切换 |

---

## 九、技术架构

### 9.1 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **前端框架** | Vue 3 | 3.5.21 |
| **前端语言** | TypeScript | 5.6.3 |
| **UI框架** | Element Plus | 2.11.2 |
| **CSS框架** | Tailwind CSS | 4.1.14 |
| **前端模板** | Art Design Pro | — |
| **构建工具** | Vite | 7.1.5 |
| **状态管理** | Pinia | 3.0.3 |
| **路由** | Vue Router | 4.5.1 |
| **图表** | ECharts | 6.0.0 |
| **国际化** | Vue I18n | 9.14.0 |
| **WebSocket客户端** | @stomp/stompjs + sockjs-client | 7.0.0 / 1.6.1 |
| **后端框架** | Spring Boot | 2.7.18 |
| **后端语言** | Java | 11 |
| **安全框架** | Spring Security | — |
| **ORM框架** | MyBatis-Plus | 3.5.5 |
| **数据库** | MySQL | 8.x |
| **缓存** | Redis | — |
| **JWT** | jjwt | 0.11.5 |
| **Excel处理** | EasyExcel | 3.3.3 |
| **工具库** | Hutool | 5.8.25 |
| **WebSocket服务端** | Spring WebSocket (STOMP) | — |
| **限流** | Google Guava RateLimiter | 33.0.0 |

### 9.2 系统架构概览

```
┌────────────────────────────────────────────────────────────┐
│                      前端（Browser）                        │
│  Vue3 + TypeScript + Element Plus + Tailwind + ECharts     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ 管理员端  │  │  教师端   │  │  学生端   │  │  公共组件 │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│              ↕ HTTP(Axios) + WebSocket(STOMP)              │
├────────────────────────────────────────────────────────────┤
│                      后端（Server）                         │
│  Spring Boot + Spring Security + MyBatis-Plus              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │Controller│→ │ Service  │→ │  Mapper  │→ │  MySQL   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                 │
│  │JWT Filter│  │WebSocket │  │  Redis   │                 │
│  └──────────┘  └──────────┘  └──────────┘                 │
├────────────────────────────────────────────────────────────┤
│                    数据存储层                               │
│  MySQL 8.x（19张业务表）+ Redis（通知去重/Token黑名单）     │
└────────────────────────────────────────────────────────────┘
```

### 9.3 后端分层架构

```
com.exam
├── common/          # 公共组件（异常、结果封装、常量、工具类）
├── config/          # 配置类（WebSocket、CORS、MyBatis-Plus等）
├── controller/      # 控制层（19个Controller）
├── dto/             # 数据传输对象（Request/Response）
├── entity/          # 实体类（19个，对应19张表）
├── mapper/          # 数据访问层（19个Mapper接口）
├── security/        # 安全模块（JWT、Security配置、XSS防护）
├── service/         # 业务逻辑层
│   └── impl/        # 业务实现类
└── task/            # 定时任务（缺考检测）
```

---

## 十、页面清单与路由设计

### 10.1 管理员页面

| 页面 | 路由路径 | 组件路径 | 角色 |
|------|---------|---------|------|
| 管理员首页 | /admin-home | /admin/home | R_ADMIN |
| 用户管理 | /admin/user | /admin/user-manage | R_ADMIN |
| 专业管理 | /academic/major | /admin/major-manage | R_ADMIN |
| 班级管理 | /academic/class | /admin/class-manage | R_ADMIN |
| 学科管理 | /academic/subject | /admin/subject-manage | R_ADMIN |
| 个人中心 | /admin/profile | /profile/index | R_ADMIN |

### 10.2 教师页面

| 页面 | 路由路径 | 组件路径 | 角色 |
|------|---------|---------|------|
| 教师首页 | /teacher-home | /teacher/home | R_TEACHER |
| 题库管理 | /teacher/question | /teacher/question-manage | R_TEACHER |
| 试卷管理 | /teacher/paper | /teacher/paper-manage | R_TEACHER |
| 试卷模板 | /teacher/paper/template | /teacher/paper-template | R_TEACHER |
| 试卷编辑 | /teacher/paper/edit/:id? | /teacher/paper-edit | R_TEACHER |
| 考试管理 | /exam-center/exam | /teacher/exam-manage | R_TEACHER |
| 阅卷列表 | /exam-center/marking | /teacher/marking-list | R_TEACHER |
| 阅卷详情 | /exam-center/marking/:recordId | /teacher/marking-detail | R_TEACHER |
| 成绩管理 | /exam-center/score | /teacher/score-manage | R_TEACHER |
| 成绩分析 | /exam-center/score/analysis/:examId | /teacher/score-analysis | R_TEACHER |
| 个人中心 | /teacher/profile | /profile/index | R_TEACHER |

### 10.3 学生页面

| 页面 | 路由路径 | 组件路径 | 角色 |
|------|---------|---------|------|
| 学生首页 | /student-home | /student/home | R_STUDENT |
| 我的考试 | /student/exam | /student/my-exam | R_STUDENT |
| 答题页面 | /student/exam/do/:examId | /student/exam-page | R_STUDENT |
| 我的成绩 | /my-study/score | /student/my-score | R_STUDENT |
| 成绩详情 | /my-study/score/:recordId | /student/score-detail | R_STUDENT |
| 错题本 | /my-study/wrong | /student/wrong-book | R_STUDENT |
| 个人中心 | /student/profile | /profile/index | R_STUDENT |

### 10.4 公共页面

| 页面 | 路由路径 | 说明 |
|------|---------|------|
| 登录页 | /auth/login | 所有用户共用 |
| 403页 | /exception/403 | 无权限 |
| 404页 | /exception/404 | 页面不存在 |
| 500页 | /exception/500 | 服务器错误 |

---

## 十一、优先级定义

### 11.1 优先级说明

| 优先级 | 含义 | 说明 |
|--------|------|------|
| P0 | MVP必须 | 核心功能，没有则系统无法运行 |
| P1 | 重要功能 | 增强体验和效率的功能 |
| P2 | 锦上添花 | 非核心但有附加价值的功能 |

### 11.2 P0功能清单（MVP）

- 登录认证（JWT + 滑块验证码）
- 三角色权限隔离
- 用户管理（CRUD + 状态切换）
- 班级/学科管理
- 题库管理（5种题型 + CRUD）
- 试卷管理（手动组卷）
- 考试管理（发布考试 + 查看考情）
- 在线考试（答题 + 自动保存 + 倒计时 + 交卷）
- 自动阅卷（客观题自动批改）
- 阅卷（主观题教师手动批改 + 发布成绩）
- 成绩查看（学生查看成绩详情）
- 错题本（查看错题 + 详情）
- 通知推送（WebSocket实时通知）
- 仪表盘（三角色首页统计）

### 11.3 P1功能清单

- Excel批量导入题目
- 试卷模板自动组卷
- 成绩分析（统计图表）
- 成绩导出Excel
- 切屏检测
- 缺考自动标记
- 个人中心（头像上传、密码修改）
- 深色/浅色主题切换

### 11.4 P2功能清单

- 即时聊天（WebSocket文本通信）
- 在线状态检测
- 国际化（中英文切换）

---

## 十二、验收标准

### 12.1 功能验收

| 验收项 | 标准 |
|--------|------|
| 登录认证 | 三种角色均可正确登录，错误密码有明确提示，5次失败锁定 |
| 权限隔离 | 角色A不能访问角色B的路由和API |
| 考试闭环 | 教师出题→组卷→发布考试→学生答题交卷→自动批改→教师阅卷→发布成绩→学生查看→错题本 |
| 数据完整性 | 有关联的数据禁止删除并给出明确提示 |
| 实时通知 | 考试发布/交卷/成绩发布触发WebSocket实时推送 |

### 12.2 非功能验收

| 验收项 | 标准 |
|--------|------|
| 性能 | 所有API响应时间 ≤500ms |
| 安全 | SQL注入、XSS、暴力破解、越权访问全部防护通过 |
| 兼容性 | 1024×768 / 1366×768 / 1920×1080 三种分辨率正常显示 |
| 可用性 | 中文提示、表单校验、操作确认、空状态处理完备 |

### 12.3 测试覆盖

本系统已完成 **109项测试用例**，覆盖功能测试、安全测试、性能测试、兼容性测试、可用性测试、WebSocket稳定性测试六个维度，**通过率100%（109/109）**。

详细测试结果参见：`doc/项目完整测试/测试执行记录.md`

---

## 附录A：术语表

| 术语 | 说明 |
|------|------|
| JWT | JSON Web Token，无状态的身份认证令牌 |
| RBAC | Role-Based Access Control，基于角色的访问控制 |
| SPA | Single Page Application，单页面应用 |
| WebSocket | 全双工通信协议，用于实时消息推送 |
| STOMP | Simple Text Oriented Messaging Protocol，WebSocket上的消息协议 |
| SockJS | WebSocket的降级兼容方案 |
| ORM | Object-Relational Mapping，对象关系映射 |
| DTO | Data Transfer Object，数据传输对象 |
| BCrypt | 密码哈希算法，自带盐值 |
| MyBatis-Plus | MyBatis的增强版ORM框架 |

## 附录B：参考标准

| 标准 | 说明 |
|------|------|
| ISO/IEC/IEEE 12207:2017 | 软件生命周期过程国际标准 |
| ISO/IEC/IEEE 29148 | 需求工程国际标准 |
| IEEE Std 1074-1997 | 软件生命周期过程开发标准 |

---

> 本文档基于项目实际代码分析生成，所有功能描述、API接口、数据表结构、技术栈版本均与代码仓库完全一致，无虚构内容。
