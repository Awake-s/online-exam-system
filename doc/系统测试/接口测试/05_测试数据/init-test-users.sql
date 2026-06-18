-- ============================================================
-- 接口测试 · 专用测试账号与业务数据初始化 SQL
-- ============================================================
-- 文件路径 : doc/接口测试/05_测试数据/init-test-users.sql
-- 用途     : 为接口自动化测试创建独立的测试账号 + 测试业务数据
-- 执行方式 : Get-Content init-test-users.sql -Raw -Encoding UTF8 | mysql -uroot -p12345678 online_exam_system
-- 幂等     : 使用 INSERT ... ON DUPLICATE KEY UPDATE，可重复执行
-- 隔离     : 使用 9000+ ID 段，与真实业务数据完全隔离
-- 清理     : 见 cleanup-test-data.sql
--
-- ⚠️ 字段定义全部基于 exam-system/sql/online_exam_system.sql 实际表结构
--    sys_user           : 第 77-95 行
--    edu_subject        : 第 101-106 行（无 status 字段）
--    exam_question      : 第 126-150 行（用 deleted 字段，无 status）
--    exam_paper         : 第 156-172 行（无 question_count 字段）
--    exam_paper_question: 第 178-187 行
--    exam_exam          : 第 193-214 行（creator_id 不是 teacher_id；class_id 单值；无 duration）
--    sys_role           : 第 43-46 行（1=ADMIN, 2=TEACHER, 3=STUDENT）
--    edu_class          : 第 423-426 行（class_id=1 是计算机2201）
-- ============================================================

USE online_exam_system;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 测试账号（3 个，密码统一为 123456）
-- ============================================================
-- BCrypt 哈希说明：
--   $2a$10$x2BK4PHI8NjVlZIxXUK/auBGKYqlLLC8wSqtx2TZJyf9mxn3.GURW = BCrypt('123456')
--   该哈希值复用自 sys_user 表 luweizhong (id=14) 的密码字段（线上验证通过）
--   引用源码: @AuthServiceImpl.java:90 passwordEncoder.matches(password, user.getPassword())

INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status)
VALUES
    (9001, 'it_admin',   '$2a$10$x2BK4PHI8NjVlZIxXUK/auBGKYqlLLC8wSqtx2TZJyf9mxn3.GURW', '【接口测试】管理员', NULL, 'it_admin@test.local',   NULL, 0, 1, NULL, 1),
    (9002, 'it_teacher', '$2a$10$x2BK4PHI8NjVlZIxXUK/auBGKYqlLLC8wSqtx2TZJyf9mxn3.GURW', '【接口测试】教师',   NULL, 'it_teacher@test.local', NULL, 1, 2, NULL, 1),
    (9003, 'it_student', '$2a$10$x2BK4PHI8NjVlZIxXUK/auBGKYqlLLC8wSqtx2TZJyf9mxn3.GURW', '【接口测试】学生',   NULL, 'it_student@test.local', NULL, 1, 3, 1,    1)
ON DUPLICATE KEY UPDATE
    password    = VALUES(password),
    real_name   = VALUES(real_name),
    role_id     = VALUES(role_id),
    class_id    = VALUES(class_id),
    status      = 1,
    update_time = CURRENT_TIMESTAMP;

-- ============================================================
-- 2. 测试学科 (edu_subject 字段: id, subject_name, description, create_time)
-- ============================================================
INSERT INTO edu_subject (id, subject_name, description)
VALUES
    (9001, '【接口测试】Java 基础', '接口自动化测试专用学科，请勿删除')
ON DUPLICATE KEY UPDATE
    subject_name = VALUES(subject_name),
    description  = VALUES(description);

-- ============================================================
-- 3. 测试题目 (exam_question 字段: id, subject_id, question_type, content, options, answer, analysis, score, difficulty, creator_id, deleted)
-- ============================================================
-- 字段顺序严格按表定义: subject_id 在 question_type 之前；analysis 在 score 之前；用 deleted (=0) 而非 status

INSERT INTO exam_question (id, subject_id, question_type, content, options, answer, analysis, score, difficulty, creator_id, deleted)
VALUES
    (9001, 9001, 1, '【接口测试】Java 中以下哪个关键字用于继承？',
     '[{"key":"A","value":"extends"},{"key":"B","value":"implements"},{"key":"C","value":"inherit"},{"key":"D","value":"super"}]',
     'A', 'extends 关键字用于类继承，implements 用于接口实现', 5.00, 1, 9002, 0),
    (9002, 9001, 2, '【接口测试】Java 中哪些是基本数据类型？',
     '[{"key":"A","value":"int"},{"key":"B","value":"String"},{"key":"C","value":"boolean"},{"key":"D","value":"double"}]',
     'A,C,D', 'String 是引用类型，不属于基本数据类型', 10.00, 2, 9002, 0),
    (9003, 9001, 3, '【接口测试】Java 是面向对象的编程语言。',
     '[{"key":"A","value":"正确"},{"key":"B","value":"错误"}]',
     'A', 'Java 是经典的面向对象编程语言', 5.00, 1, 9002, 0)
ON DUPLICATE KEY UPDATE
    content    = VALUES(content),
    answer     = VALUES(answer),
    analysis   = VALUES(analysis),
    creator_id = VALUES(creator_id),
    deleted    = 0;

-- ============================================================
-- 4. 测试试卷 (exam_paper 字段: id, paper_name, subject_id, total_score, pass_score, duration, creator_id, status)
-- ============================================================
-- 注：exam_paper 表无 question_count 字段，题目数从 exam_paper_question 关联表 COUNT 而来

INSERT INTO exam_paper (id, paper_name, subject_id, total_score, pass_score, duration, creator_id, status)
VALUES
    (9001, '【接口测试】Java 基础测试卷', 9001, 20.00, 12.00, 60, 9002, 1)
ON DUPLICATE KEY UPDATE
    paper_name  = VALUES(paper_name),
    total_score = VALUES(total_score),
    pass_score  = VALUES(pass_score),
    duration    = VALUES(duration),
    creator_id  = VALUES(creator_id),
    status      = 1,
    update_time = CURRENT_TIMESTAMP;

-- ============================================================
-- 5. 试卷题目关联 (exam_paper_question 字段: id, paper_id, question_id, score, sort_order)
-- ============================================================
INSERT INTO exam_paper_question (id, paper_id, question_id, score, sort_order)
VALUES
    (9001, 9001, 9001, 5.00,  1),
    (9002, 9001, 9002, 10.00, 2),
    (9003, 9001, 9003, 5.00,  3)
ON DUPLICATE KEY UPDATE
    score      = VALUES(score),
    sort_order = VALUES(sort_order);

-- ============================================================
-- 6. 测试考试 (exam_exam 字段: id, exam_name, paper_id, class_id, start_time, end_time, creator_id, status)
-- ============================================================
-- 重要修正：
--   ❌ 错: teacher_id, class_ids, duration   ✅ 对: creator_id, class_id（单值）, 无 duration
--   class_id = 1 (计算机2201班，即 it_student 所在班级)
--   时间窗：1天前 ~ 7天后，确保考试期内学生可访问
-- status: 0=未开始 1=进行中 2=已结束

INSERT INTO exam_exam (id, exam_name, paper_id, class_id, start_time, end_time, creator_id, status, score_published)
VALUES
    (9001, '【接口测试】Java 期中考试', 9001, 1,
     DATE_SUB(NOW(), INTERVAL 1 DAY),
     DATE_ADD(NOW(), INTERVAL 7 DAY),
     9002, 1, 0)
ON DUPLICATE KEY UPDATE
    exam_name        = VALUES(exam_name),
    paper_id         = VALUES(paper_id),
    class_id         = VALUES(class_id),
    start_time       = VALUES(start_time),
    end_time         = VALUES(end_time),
    creator_id       = VALUES(creator_id),
    status           = 1,
    score_published  = 0,
    update_time      = CURRENT_TIMESTAMP;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 验证结果
-- ============================================================
SELECT '✅ 测试账号' AS step, COUNT(*) AS count FROM sys_user             WHERE id BETWEEN 9000 AND 9999
UNION ALL
SELECT '✅ 测试学科',         COUNT(*)         FROM edu_subject           WHERE id BETWEEN 9000 AND 9999
UNION ALL
SELECT '✅ 测试题目',         COUNT(*)         FROM exam_question         WHERE id BETWEEN 9000 AND 9999
UNION ALL
SELECT '✅ 测试试卷',         COUNT(*)         FROM exam_paper            WHERE id BETWEEN 9000 AND 9999
UNION ALL
SELECT '✅ 试卷题目关联',     COUNT(*)         FROM exam_paper_question   WHERE id BETWEEN 9000 AND 9999
UNION ALL
SELECT '✅ 测试考试',         COUNT(*)         FROM exam_exam             WHERE id BETWEEN 9000 AND 9999;

-- ============================================================
-- 测试账号速查表（与 environment.json 对照）
-- ============================================================
-- | username    | password | role    | role_id | class_id | 业务用途                       |
-- |-------------|----------|---------|---------|----------|--------------------------------|
-- | it_admin    | 123456   | ADMIN   | 1       | NULL     | 管理类接口测试                 |
-- | it_teacher  | 123456   | TEACHER | 2       | NULL     | 题库/试卷/考试/阅卷接口测试    |
-- | it_student  | 123456   | STUDENT | 3       | 1        | 学生考试/成绩/错题接口测试     |

-- ============================================================
-- 测试业务数据 ID 速查表（用于 environment 中的固定 ID）
-- ============================================================
-- | 资源              | ID   | 描述                                          |
-- |-------------------|------|-----------------------------------------------|
-- | subjectId         | 9001 | 【接口测试】Java 基础学科                     |
-- | questionId        | 9001 | 【接口测试】Java 单选题（继承关键字）         |
-- | paperId           | 9001 | 【接口测试】Java 基础测试卷（含 3 题，20 分） |
-- | examId            | 9001 | 【接口测试】Java 期中考试（已发布，7天有效）   |
