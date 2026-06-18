-- ============================================================
-- 在线考试系统数据库脚本
-- 数据库：MySQL 8.0
-- 创建时间：2026-01-15
-- 更新时间：2026-06-06
-- 【重要】导入方式（防止中文乱码 ???）：
--   命令行：mysql --default-character-set=utf8mb4 -u root -p < online_exam_system.sql
--   Navicat：连接高级选项编码选 UTF-8，执行前确认 SET NAMES utf8mb4 已生效
--   禁止：PowerShell Get-Content 管道导入（会破坏 UTF-8 中文）
--
-- 说明：在Navicat中直接运行此脚本即可创建所有数据表和初始数据
-- 同步状态：已与 entity/ 目录下全部 Entity 及本机数据库实际表结构逐字段核对一致
--   · 2026-04-18 同步 L3 消息软删与会话隐藏字段（deleted_at/deleted_by/user?_hidden）
--   · 2026-04-18 修正 exam_exam 遗漏的 last_publish_time 字段（对应 ExamExam.lastPublishTime）
--   · 2026-04-18 修正 5 处 TEXT→MEDIUMTEXT，与数据库实际 column_type 保持一致
--     （题目/答案/聊天内容的 TEXT 上限 64KB 偏小，早期已 ALTER 升级为 MEDIUMTEXT 16MB）
--   · 2026-04-25 修复 sys_notification 索引区重复 CREATE INDEX，避免 MySQL 1061 Duplicate key name
--     （建表内已声明索引，后部 4 条 CREATE INDEX 删除并改为说明性注释）
--   · 2026-04-25 补全 sys_notification.type COMMENT 为 12 种通知类型
--     （新增 EXAM_AUTO_SUBMITTED、SCORE_UPDATED；已同步本机库；生产库需在低峰期执行
--      ALTER TABLE sys_notification MODIFY COLUMN type ... COMMENT ...，详见部署文档）
-- ============================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS online_exam_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE online_exam_system;

-- 确保连接使用 utf8mb4 字符集（支持 emoji 等4字节字符）
SET NAMES utf8mb4;

-- 禁用外键检查（允许按任意顺序DROP/CREATE表）
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 角色表 (sys_role)
-- ============================================================
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码：ADMIN/TEACHER/STUDENT',
    description VARCHAR(255) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '角色表';

-- 角色初始数据
INSERT INTO sys_role (role_name, role_code, description) VALUES
('管理员', 'ADMIN', '系统管理员'),
('教师', 'TEACHER', '教师'),
('学生', 'STUDENT', '学生');

-- ============================================================
-- 2. 专业表 (edu_major)
-- ============================================================
DROP TABLE IF EXISTS edu_major;
CREATE TABLE edu_major (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '专业ID',
    major_name VARCHAR(100) NOT NULL COMMENT '专业名称',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '专业表';

-- ============================================================
-- 3. 班级表 (edu_class)
-- ============================================================
DROP TABLE IF EXISTS edu_class;
CREATE TABLE edu_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
    grade VARCHAR(50) COMMENT '年级',
    major_id BIGINT COMMENT '所属专业ID',
    description VARCHAR(255) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_major FOREIGN KEY (major_id) REFERENCES edu_major(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '班级表';

-- ============================================================
-- 4. 用户表 (sys_user)
-- ============================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    avatar VARCHAR(255) COMMENT '头像',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    gender TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    role_id BIGINT COMMENT '角色ID',
    class_id BIGINT COMMENT '班级ID（学生）',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_gender CHECK (gender IN (0, 1, 2)),
    CONSTRAINT chk_user_status CHECK (status IN (0, 1)),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE SET NULL,
    CONSTRAINT fk_user_class FOREIGN KEY (class_id) REFERENCES edu_class(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '用户表';

-- ============================================================
-- 5. 科目表 (edu_subject)
-- ============================================================
DROP TABLE IF EXISTS edu_subject;
CREATE TABLE edu_subject (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '科目ID',
    subject_name VARCHAR(100) NOT NULL COMMENT '科目名称',
    grade VARCHAR(20) NULL COMMENT '年级，如2022级、2024级；NULL 表示通用抽象科目（兼容旧数据）',
    major_id BIGINT NULL COMMENT '所属专业ID（FK edu_major.id）；NULL 表示通用',
    hours INT NULL COMMENT '总学时（历史遗留字段，前端不展示）',
    exam_type VARCHAR(10) NULL COMMENT '考核方式：试/查（历史遗留字段，前端不展示）',
    description VARCHAR(255) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_subject_grade_major (grade, major_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '科目表';

-- ============================================================
-- 5.1 科目专业关联表 (subject_major) —— 多对多
-- ============================================================
DROP TABLE IF EXISTS subject_major;
CREATE TABLE subject_major (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    subject_id BIGINT NOT NULL COMMENT '科目ID',
    major_id BIGINT NOT NULL COMMENT '专业ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_subject_major (subject_id, major_id),
    CONSTRAINT fk_sm_subject FOREIGN KEY (subject_id) REFERENCES edu_subject(id) ON DELETE CASCADE,
    CONSTRAINT fk_sm_major FOREIGN KEY (major_id) REFERENCES edu_major(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '科目专业关联表';

-- ============================================================
-- 6. 题目表 (exam_question)
-- ============================================================
DROP TABLE IF EXISTS exam_question;
CREATE TABLE exam_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    subject_id BIGINT NOT NULL COMMENT '科目ID',
    question_type TINYINT NOT NULL COMMENT '题型：1单选 2多选 3判断 4填空 5简答',
    content MEDIUMTEXT NOT NULL COMMENT '题目内容（mediumtext 支持最长 16MB，足以容纳富文本题干/图片 base64）',
    options JSON COMMENT '选项（JSON格式）',
    answer MEDIUMTEXT NOT NULL COMMENT '正确答案（mediumtext，兼容简答题长答案）',
    analysis MEDIUMTEXT COMMENT '答案解析（mediumtext，兼容长解析文本）',
    score DECIMAL(5,2) DEFAULT 0 COMMENT '默认分值',
    difficulty TINYINT DEFAULT 1 COMMENT '难度：1简单 2中等 3困难',
    creator_id BIGINT NOT NULL COMMENT '创建人ID（教师）',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除：0正常 1已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_question_subject (subject_id),
    INDEX idx_question_type (question_type),
    INDEX idx_question_difficulty (difficulty),
    INDEX idx_question_creator (creator_id),
    INDEX idx_question_deleted (deleted),
    CONSTRAINT chk_question_type CHECK (question_type IN (1, 2, 3, 4, 5)),
    CONSTRAINT chk_question_difficulty CHECK (difficulty IN (1, 2, 3)),
    CONSTRAINT chk_question_deleted CHECK (deleted IN (0, 1)),
    CONSTRAINT fk_question_subject FOREIGN KEY (subject_id) REFERENCES edu_subject(id) ON DELETE RESTRICT,
    CONSTRAINT fk_question_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '题目表';

-- ============================================================
-- 7. 试卷表 (exam_paper)
-- ============================================================
DROP TABLE IF EXISTS exam_paper;
CREATE TABLE exam_paper (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '试卷ID',
    paper_name VARCHAR(200) NOT NULL COMMENT '试卷名称',
    subject_id BIGINT NOT NULL COMMENT '科目ID',
    total_score DECIMAL(5,2) DEFAULT 100 COMMENT '总分',
    pass_score DECIMAL(5,2) DEFAULT 60 COMMENT '及格分',
    duration INT DEFAULT 120 COMMENT '考试时长（分钟）',
    creator_id BIGINT NOT NULL COMMENT '创建人ID（教师）',
    status TINYINT DEFAULT 0 COMMENT '状态：0草稿 1已发布',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_paper_subject (subject_id),
    INDEX idx_paper_creator (creator_id),
    CONSTRAINT chk_paper_status CHECK (status IN (0, 1)),
    CONSTRAINT fk_paper_subject FOREIGN KEY (subject_id) REFERENCES edu_subject(id) ON DELETE RESTRICT,
    CONSTRAINT fk_paper_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '试卷表';

-- ============================================================
-- 8. 试卷题目关联表 (exam_paper_question)
-- ============================================================
DROP TABLE IF EXISTS exam_paper_question;
CREATE TABLE exam_paper_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    score DECIMAL(5,2) NOT NULL COMMENT '本题分值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    UNIQUE KEY uk_paper_question (paper_id, question_id),
    CONSTRAINT fk_pq_paper FOREIGN KEY (paper_id) REFERENCES exam_paper(id) ON DELETE CASCADE,
    CONSTRAINT fk_pq_question FOREIGN KEY (question_id) REFERENCES exam_question(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '试卷题目关联表';

-- ============================================================
-- 9. 考试表 (exam_exam)
-- ============================================================
DROP TABLE IF EXISTS exam_exam;
CREATE TABLE exam_exam (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考试ID',
    exam_name VARCHAR(200) NOT NULL COMMENT '考试名称',
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    creator_id BIGINT NOT NULL COMMENT '创建人ID（教师）',
    status TINYINT DEFAULT 0 COMMENT '状态：0未开始 1进行中 2已结束',
    score_published TINYINT(1) NOT NULL DEFAULT 0 COMMENT '成绩是否已发布 0未发布 1已发布',
    -- 最后一次成绩发布时间（同时作为乐观锁版本号）。NULL=从未发布；非NULL=最后一次发布/重发时间。
    -- 用于：1) 通知文案区分首发/重发；2) CAS 防并发双击；对应 ExamExam.lastPublishTime
    last_publish_time DATETIME NULL DEFAULT NULL COMMENT '最后一次成绩发布时间（同时作为乐观锁版本号，支持无限次重发）',
    anti_cheat_config JSON DEFAULT NULL COMMENT '防作弊配置JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_exam_class_endtime (class_id, end_time),
    INDEX idx_exam_creator (creator_id),
    CONSTRAINT fk_exam_paper FOREIGN KEY (paper_id) REFERENCES exam_paper(id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_class FOREIGN KEY (class_id) REFERENCES edu_class(id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '考试表';

-- ============================================================
-- 10. 考试记录表 (exam_record)
-- ============================================================
DROP TABLE IF EXISTS exam_record;
CREATE TABLE exam_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    user_id BIGINT NOT NULL COMMENT '学生ID',
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    start_time DATETIME COMMENT '开始答题时间',
    submit_time DATETIME COMMENT '提交时间',
    total_score DECIMAL(5,2) COMMENT '总得分',
    objective_score DECIMAL(5,2) COMMENT '客观题得分',
    subjective_score DECIMAL(5,2) COMMENT '主观题得分',
    status TINYINT DEFAULT 0 COMMENT '状态：0未开始 1答题中 2已交卷 3已批改 4缺考',
    switch_count INT DEFAULT 0 COMMENT '切屏次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exam_user (exam_id, user_id),
    INDEX idx_record_user_exam (user_id, exam_id, status),
    CONSTRAINT fk_record_exam FOREIGN KEY (exam_id) REFERENCES exam_exam(id) ON DELETE CASCADE,
    CONSTRAINT fk_record_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_record_paper FOREIGN KEY (paper_id) REFERENCES exam_paper(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '考试记录表';

-- ============================================================
-- 11. 答题记录表 (exam_answer)
-- ============================================================
DROP TABLE IF EXISTS exam_answer;
CREATE TABLE exam_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL COMMENT '考试记录ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    answer MEDIUMTEXT COMMENT '学生答案（mediumtext，支持长简答题/代码题提交）',
    is_correct TINYINT COMMENT '是否正确：0错误 1正确 2部分正确',
    score DECIMAL(5,2) DEFAULT 0 COMMENT '得分',
    comment VARCHAR(500) COMMENT '教师评语（阅卷时填写）',
    is_removed TINYINT DEFAULT 0 COMMENT '是否从错题本移除：0否 1是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_record_question (record_id, question_id),
    INDEX idx_answer_record (record_id),
    INDEX idx_answer_question (question_id),
    CONSTRAINT chk_answer_correct CHECK (is_correct IS NULL OR is_correct IN (0, 1, 2)),
    CONSTRAINT chk_answer_removed CHECK (is_removed IN (0, 1)),
    CONSTRAINT fk_answer_record FOREIGN KEY (record_id) REFERENCES exam_record(id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES exam_question(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '答题记录表';

-- ============================================================
-- 12. 教师班级关联表 (teacher_class)
-- ============================================================
DROP TABLE IF EXISTS teacher_class;
CREATE TABLE teacher_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_teacher_class (teacher_id, class_id),
    CONSTRAINT fk_tc_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_tc_class FOREIGN KEY (class_id) REFERENCES edu_class(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '教师班级关联表';

-- ============================================================
-- 13. 教师科目关联表 (teacher_subject)
-- ============================================================
DROP TABLE IF EXISTS teacher_subject;
CREATE TABLE teacher_subject (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    subject_id BIGINT NOT NULL COMMENT '科目ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_teacher_subject (teacher_id, subject_id),
    CONSTRAINT fk_ts_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_ts_subject FOREIGN KEY (subject_id) REFERENCES edu_subject(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '教师科目关联表';

-- ============================================================
-- 14. 试卷模板表 (exam_paper_template)
-- ============================================================
DROP TABLE IF EXISTS exam_paper_template;
CREATE TABLE exam_paper_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    template_name VARCHAR(200) NOT NULL COMMENT '模板名称',
    subject_id BIGINT NOT NULL COMMENT '科目ID',
    target_score DECIMAL(5,2) DEFAULT 100 COMMENT '目标总分',
    pass_score DECIMAL(5,2) DEFAULT 60 COMMENT '及格分',
    duration INT DEFAULT 120 COMMENT '考试时长（分钟）',
    description VARCHAR(500) COMMENT '模板说明',
    creator_id BIGINT NOT NULL COMMENT '创建人ID（教师）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_template_subject (subject_id),
    INDEX idx_template_creator (creator_id),
    CONSTRAINT fk_template_subject FOREIGN KEY (subject_id) REFERENCES edu_subject(id) ON DELETE RESTRICT,
    CONSTRAINT fk_template_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '试卷模板表';

-- ============================================================
-- 15. 模板组卷规则表 (exam_template_rule)
-- ============================================================
DROP TABLE IF EXISTS exam_template_rule;
CREATE TABLE exam_template_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    question_type TINYINT NOT NULL COMMENT '题型：1单选 2多选 3判断 4填空 5简答',
    question_count INT NOT NULL COMMENT '题目数量',
    score_per_question DECIMAL(5,2) NOT NULL COMMENT '每题分值',
    difficulty TINYINT COMMENT '难度要求：1简单 2中等 3困难，NULL表示不限',
    sort_order INT DEFAULT 0 COMMENT '排序',
    CONSTRAINT chk_rule_type CHECK (question_type IN (1, 2, 3, 4, 5)),
    CONSTRAINT chk_rule_difficulty CHECK (difficulty IS NULL OR difficulty IN (1, 2, 3)),
    CONSTRAINT fk_rule_template FOREIGN KEY (template_id) REFERENCES exam_paper_template(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '模板组卷规则表';

-- ============================================================
-- 16. 系统通知表 (sys_notification)
-- ============================================================
DROP TABLE IF EXISTS sys_notification;
CREATE TABLE sys_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '接收者用户ID',
    type VARCHAR(50) NOT NULL COMMENT '通知类型: EXAM_PUBLISHED/EXAM_UPDATED/EXAM_CANCELLED/EXAM_CREATED/EXAM_SUBMITTED/EXAM_AUTO_SUBMITTED/EXAM_ABSENT/EXAM_END_SUMMARY/SCORE_PUBLISHED/SCORE_UPDATED/ACCOUNT_CREATED/USER_CREATED',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content VARCHAR(500) COMMENT '通知内容详情',
    biz_type VARCHAR(50) COMMENT '关联业务类型: exam/score/user',
    biz_id BIGINT COMMENT '关联业务ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
    priority TINYINT NOT NULL DEFAULT 2 COMMENT '优先级: 1=紧急 2=普通 3=次要',
    payload JSON NULL COMMENT '扩展载荷: {senderId,senderName,senderAvatar,actionUrl,extras}',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- 通知查询性能索引（覆盖常见访问路径）
    INDEX idx_noti_user_read (user_id, is_read, create_time DESC),      -- 用户未读通知分页（最常用）
    INDEX idx_noti_user_time (user_id, create_time DESC),               -- 用户全部通知按时间倒序
    INDEX idx_noti_user_type_biz (user_id, type, biz_id),               -- 按用户+类型+业务ID定位（幂等去重）
    INDEX idx_noti_user_type_read_time (user_id, type, is_read, create_time DESC), -- D2: 通知中心「类型+已读」组合筛选（覆盖索引，消除 filesort）
    INDEX idx_noti_read_time (is_read, create_time),                    -- 系统级未读统计/清理
    CONSTRAINT chk_noti_read CHECK (is_read IN (0, 1)),
    CONSTRAINT fk_noti_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '系统通知表';

-- ============================================================
-- 17. 聊天会话表 (chat_conversation)
-- ============================================================
DROP TABLE IF EXISTS chat_conversation;
CREATE TABLE chat_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user1_id BIGINT NOT NULL COMMENT '用户1（较小ID）',
    user2_id BIGINT NOT NULL COMMENT '用户2（较大ID）',
    last_message VARCHAR(500) COMMENT '最后一条消息摘要',
    last_message_time DATETIME COMMENT '最后消息时间',
    -- L3-M0-5：最后一条消息的发送者 ID。
    -- 对齐微信/QQ/WhatsApp 会话表设计标准：前端据此渲染"你/对方撤回了一条消息"、
    -- "你: xxx"等会话列表预览文案；未来支持群聊时可显示"张三撤回了一条消息"。
    -- 允许 NULL 以兼容 L3-M0-5 迁移前的历史会话。
    last_message_sender_id BIGINT COMMENT '最后一条消息的发送者ID（L3-M0-5：撤回文案区分"你/对方"）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- L3：会话级软隐藏，各端独立控制（收到对方新消息时自动解除隐藏）
    user1_hidden TINYINT DEFAULT 0 COMMENT 'user1 是否已隐藏该会话：0显示 1隐藏',
    user2_hidden TINYINT DEFAULT 0 COMMENT 'user2 是否已隐藏该会话：0显示 1隐藏',
    -- L3-M0-7：会话置顶 + 免打扰（对齐微信 / WhatsApp / Telegram 设计）
    user1_pinned TINYINT DEFAULT 0 COMMENT 'user1 是否已置顶该会话（上限 5 个）：0否 1是',
    user2_pinned TINYINT DEFAULT 0 COMMENT 'user2 是否已置顶该会话（上限 5 个）：0否 1是',
    user1_muted TINYINT DEFAULT 0 COMMENT 'user1 是否已对该会话免打扰（收消息但不推送通知）：0否 1是',
    user2_muted TINYINT DEFAULT 0 COMMENT 'user2 是否已对该会话免打扰（收消息但不推送通知）：0否 1是',
    UNIQUE KEY uk_users (user1_id, user2_id),
    CONSTRAINT fk_conv_user1 FOREIGN KEY (user1_id) REFERENCES sys_user(id),
    CONSTRAINT fk_conv_user2 FOREIGN KEY (user2_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '聊天会话表';

-- ============================================================
-- 18. 聊天消息表 (chat_message)
-- ============================================================
DROP TABLE IF EXISTS chat_message;
CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    content MEDIUMTEXT NOT NULL COMMENT '消息内容（mediumtext，支持长消息/图片 base64 内嵌）',
    message_type TINYINT DEFAULT 1 COMMENT '消息类型：1文字 2图片(预留)',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- L3：消息软删字段。deleted_at 非 NULL 视为已删除，查询端应过滤或渲染"已撤回"占位
    deleted_at DATETIME NULL DEFAULT NULL COMMENT '软删时间（NULL=未删除）',
    deleted_by BIGINT NULL DEFAULT NULL COMMENT '删除人用户ID（发送者撤回=自己；管理员强删=管理员ID）',
    INDEX idx_conv_time (conversation_id, create_time),
    INDEX idx_receiver_read (receiver_id, is_read),
    INDEX idx_deleted (deleted_at),
    CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id),
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT '聊天消息表';

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始化基础数据
-- ============================================================

-- 专业数据
INSERT INTO edu_major (id, major_name, description) VALUES
(1, '计算机科学与技术', '计算机科学与技术专业'),
(2, '软件工程', '软件工程专业'),
(4, '人工智能', '人工智能专业');

-- 班级数据
INSERT INTO edu_class (id, class_name, grade, major_id, description) VALUES
(1, '计算机2201', '2022级', 1, '计算机科学与技术专业'),
(2, '计算机2202', '2022级', 1, '计算机科学与技术专业'),
(3, '软件工程2201', '2022级', 2, '软件工程专业'),
(4, '软件工程2202', '2022级', 2, '软件工程专业'),
(7, '人工智能2201', '2022级', 4, '人工智能专业'),
(8, '人工智能2202', '2022级', 4, '人工智能专业');

-- 科目数据（grade/major_id 与 edu_class、subject_major 对齐，避免前端显示「通用」）
INSERT INTO edu_subject (id, subject_name, grade, major_id, hours, exam_type, description) VALUES
(1, '高等数学', '2022级', 1, NULL, '试', '公共基础课程'),
(2, '数据结构', '2022级', 1, NULL, '试', '专业核心课程'),
(3, '计算机网络', '2022级', 1, NULL, '试', '专业核心课程'),
(4, '数据库原理', '2022级', 1, NULL, '试', '专业核心课程'),
(5, 'Java程序设计', '2022级', 1, NULL, '试', '专业核心课程'),
(6, '软件工程导论', '2022级', 2, NULL, '试', '软件工程方向课程'),
(7, '计算机组成原理', '2022级', 1, NULL, '试', '专业核心课程'),
(9, '人工智能', '2022级', 4, NULL, '试', '人工智能方向课程');

-- 科目-专业关联（多对多）
INSERT INTO subject_major (subject_id, major_id) VALUES
(1, 1),           -- 高等数学 → 计算机科学与技术
(1, 2),           -- 高等数学 → 软件工程
(2, 1),           -- 数据结构 → 计算机科学与技术
(3, 1),           -- 计算机网络 → 计算机科学与技术
(4, 1),           -- 数据库原理 → 计算机科学与技术
(5, 1),           -- Java程序设计 → 计算机科学与技术
(6, 2),           -- 软件工程导论 → 软件工程
(7, 1),           -- 计算机组成原理 → 计算机科学与技术
(9, 4);           -- 人工智能 → 人工智能

-- ============================================================
-- 用户数据（密码均为BCrypt加密）
-- ============================================================

-- 管理员（密码：admin123）
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(1, 'admin', '$2a$10$Wc9zeUbSzaSrRCZjr6nCD.G3WaOfxwxKQUchjtOPoG83UvE4poPEq', '系统管理员',
 '/uploads/avatar/avatar_1_1772977536402.jpg', NULL, NULL, 0, 1, NULL, 1);

-- 教师（密码：123456）
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(13, 'lixinglaing', '$2a$10$0QEM9fOkvcZihOyYBYK6WOGMZWTcxwor9DUAPx7XmLSWw4/nD83jy', '李兴良',
 '/uploads/avatar/avatar_13_1772977565870.png', '', '', 1, 2, NULL, 1),
(14, 'luweizhong', '$2a$10$x2BK4PHI8NjVlZIxXUK/auBGKYqlLLC8wSqtx2TZJyf9mxn3.GURW', '陆卫忠',
 NULL, '', '', 1, 2, NULL, 1);

-- 学生（密码：123456）
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(8, 'taozhan', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陶展',
 '/uploads/avatar/avatar_8_1772957830070.jpg', '313141451@qq.com', '123455525265262', 1, 3, 1, 1),
(9, 'zhouxiang', '$2a$10$zhtrvuSHPjQ1nJ/2hH/yo.Bv6aRV0yTs/Xs8sFzxuWw0Rlj99tOam', '周祥',
 NULL, 'qwrqr', '14121251512', 1, 3, 2, 1),
(10, 'zhaoyu', '$2a$10$9Q/HA4ADfU7YDn1ybP7N9ubGDHu.COmbSk7/fJYjtLUkXVj21Vcdq', '赵宇',
 NULL, '13412514', '4623764377', 1, 3, 3, 1),
(11, 'gaowei', '$2a$10$zseirE7L3pLXMLUQhq26N.qYPMjUCHoCn30tNIyqPYV7KnJ5FlpSW', '高巍',
 NULL, 'gaowei@test.com', '13800138000', 1, 3, 4, 1),
(28, 'ruanjian01', '$2a$10$3yw.IZuveF0unm8hzDgQiOKmmLpoLX5IJp2AGxCYybGULFHjDgSGG', 'ruanjian01',
 NULL, '', '', 1, 3, 3, 1),
(29, 'ruanjian02', '$2a$10$/ogXmzPAyVQwhesXeO/V0.u/LMIXk2cxzEkSYj0Q9eRr0/ztWKHK2', 'ruanjian02',
 NULL, '', '', 1, 3, 4, 1);

-- ============================================================
-- 教师关联数据
-- ============================================================

-- 教师-班级关联
INSERT INTO teacher_class (teacher_id, class_id) VALUES
(13, 3),   -- 李兴良 → 软件工程2201
(13, 4),   -- 李兴良 → 软件工程2202
(14, 1),   -- 陆卫忠 → 计算机2201
(14, 2);   -- 陆卫忠 → 计算机2202

-- 教师-科目关联
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(13, 7),   -- 李兴良 → 计算机组成原理
(14, 5);   -- 陆卫忠 → Java程序设计

-- ============================================================
-- 试卷模板数据
-- ============================================================

INSERT INTO exam_paper_template (id, template_name, subject_id, target_score, pass_score, duration, creator_id) VALUES
(5, 'Java程序设计期中考试（A卷）', 5, 100.00, 60.00, 120, 14),
(6, '计算机组成原理期末（A卷）', 7, 100.00, 60.00, 120, 13);

INSERT INTO exam_template_rule (id, template_id, question_type, question_count, score_per_question, difficulty, sort_order) VALUES
(24, 5, 1, 6, 5.00, NULL, 1),   -- Java模板：6道单选×5分
(25, 5, 2, 2, 5.00, NULL, 2),   -- Java模板：2道多选×5分
(26, 5, 3, 4, 5.00, NULL, 3),   -- Java模板：4道判断×5分
(27, 5, 4, 4, 5.00, NULL, 4),   -- Java模板：4道填空×5分
(28, 5, 5, 2, 10.00, NULL, 5),  -- Java模板：2道简答×10分
(29, 6, 1, 6, 5.00, NULL, 1),   -- 组成原理模板：6道单选×5分
(30, 6, 2, 2, 5.00, NULL, 2),   -- 组成原理模板：2道多选×5分
(31, 6, 3, 2, 5.00, NULL, 3),   -- 组成原理模板：2道判断×5分
(32, 6, 4, 2, 5.00, NULL, 4),   -- 组成原理模板：2道填空×5分
(33, 6, 5, 2, 20.00, NULL, 5);  -- 组成原理模板：2道简答×20分

-- ============================================================
-- 性能优化索引（通知系统+查询优化）
-- ============================================================
-- 注：sys_notification 的全部索引已在第 16 节建表语句的 INDEX 子句内声明，
--     此处不再 CREATE INDEX，避免 MySQL 8.0 触发 1061 (ER_DUP_KEYNAME) 重复键名错误。
--     旧版本曾在表外重复 CREATE 4 个索引（idx_noti_user_time / idx_noti_user_type_biz /
--     idx_noti_user_type_read_time / idx_noti_read_time），导致全量初始化脚本中断。
--     当前所有性能索引均在建表 INDEX 子句内一次性声明，覆盖：
--       - idx_noti_user_read           (user_id, is_read, create_time DESC)
--       - idx_noti_user_time           (user_id, create_time DESC)
--       - idx_noti_user_type_biz       (user_id, type, biz_id)
--       - idx_noti_user_type_read_time (user_id, type, is_read, create_time DESC)
--       - idx_noti_read_time           (is_read, create_time)
-- 注：exam_record 和 exam_exam 的优化索引已包含在建表语句中

-- ============================================================
-- 注意事项
-- ============================================================
-- 1. 题库数据（exam_question）较大，请通过应用程序导入或单独的SQL文件导入
-- 2. 所有用户默认密码为 123456（管理员为 admin123），首次登录后建议修改
-- 3. 考试数据（exam_exam/exam_record/exam_answer）、通知（sys_notification）、
--    聊天（chat_conversation/chat_message）均为运行时数据，无需初始化

-- ============================================================
-- 完成提示
-- ============================================================
SELECT '数据库初始化完成！共创建19个数据表，含3个专业、6个班级、8个科目、9个用户账号。' AS '提示信息';
