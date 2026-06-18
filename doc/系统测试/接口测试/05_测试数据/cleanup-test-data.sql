-- ============================================================
-- 接口测试 · 测试数据清理 SQL
-- ============================================================
-- 文件路径 : doc/接口测试/05_测试数据/cleanup-test-data.sql
-- 用途     : 清理 init-test-users.sql 创建的全部测试数据，
--           恢复数据库到初始化前状态
-- 执行     : Get-Content cleanup-test-data.sql -Raw -Encoding UTF8 | mysql -uroot -p12345678 online_exam_system
-- 安全     : 严格按 9000+ ID 段过滤，不会误删真实业务数据
-- 顺序     : 必须按外键依赖反向顺序删除（先删子表，再删主表）
-- ============================================================

USE online_exam_system;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 先删答题/考试记录（如果接口测试时学生答了题）
DELETE FROM exam_answer        WHERE record_id IN (SELECT id FROM exam_record WHERE id BETWEEN 9000 AND 9999);
DELETE FROM exam_record        WHERE id BETWEEN 9000 AND 9999;
DELETE FROM exam_record        WHERE exam_id BETWEEN 9000 AND 9999;

-- 2. 删考试
DELETE FROM exam_exam          WHERE id BETWEEN 9000 AND 9999;

-- 3. 删试卷题目关联
DELETE FROM exam_paper_question WHERE id BETWEEN 9000 AND 9999;
DELETE FROM exam_paper_question WHERE paper_id BETWEEN 9000 AND 9999;

-- 4. 删试卷
DELETE FROM exam_paper         WHERE id BETWEEN 9000 AND 9999;

-- 5. 删题目
DELETE FROM exam_question      WHERE id BETWEEN 9000 AND 9999;

-- 6. 删学科
DELETE FROM edu_subject        WHERE id BETWEEN 9000 AND 9999;

-- 7. 最后删用户
DELETE FROM sys_user           WHERE id BETWEEN 9000 AND 9999;

SET FOREIGN_KEY_CHECKS = 1;

-- 验证清理结果
SELECT '✅ 清理后 测试账号' AS step, COUNT(*) AS count FROM sys_user             WHERE id BETWEEN 9000 AND 9999
UNION ALL
SELECT '✅ 清理后 测试学科',          COUNT(*)         FROM edu_subject           WHERE id BETWEEN 9000 AND 9999
UNION ALL
SELECT '✅ 清理后 测试题目',          COUNT(*)         FROM exam_question         WHERE id BETWEEN 9000 AND 9999
UNION ALL
SELECT '✅ 清理后 测试试卷',          COUNT(*)         FROM exam_paper            WHERE id BETWEEN 9000 AND 9999
UNION ALL
SELECT '✅ 清理后 试卷题目关联',      COUNT(*)         FROM exam_paper_question   WHERE id BETWEEN 9000 AND 9999
UNION ALL
SELECT '✅ 清理后 测试考试',          COUNT(*)         FROM exam_exam             WHERE id BETWEEN 9000 AND 9999;

-- 期望全部为 0
