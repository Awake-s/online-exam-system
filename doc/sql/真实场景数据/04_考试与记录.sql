-- ============================================================
-- 在线考试系统 · 苏科大天平学院真实场景种子数据 · P4 考试与考试记录（清空版）
-- v2.2 教师后台 + 学生后台业务数据置空
-- ============================================================
-- v2.2 行为变更（与 v2.1 相比）：
--   · 不再预置任何考试/考试记录/考试答题
--   · 仅保留 DELETE 清理动作
--   · 教师从前端「考试管理」自行创建考试；学生在「我的考试」中作答
--
-- 清空范围（与表的外键依赖逆序一致）：
--   exam_answer   考试答题（学生作答）
--   exam_record   考试记录（学生参考记录）
--   exam_exam     考试主表
--
-- 前置依赖：P1（基础数据）+ P2（题库）+ P3（清空模板/试卷）
-- ============================================================

USE online_exam_system;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- 清空教师后台「考试」+ 学生后台「我的考试」「我的成绩」业务数据
DELETE FROM exam_answer WHERE record_id >= 100;
DELETE FROM exam_record WHERE id        >= 100;
DELETE FROM exam_exam   WHERE id        >= 100;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

-- ============================================================
-- 验证断言（应全部为 0）
-- ============================================================
SELECT '考试'     AS 实体, COUNT(*) AS 实际, 0 AS 预期 FROM exam_exam   WHERE id        >= 100
UNION ALL SELECT '考试记录', COUNT(*), 0 FROM exam_record WHERE id        >= 100
UNION ALL SELECT '考试答题', COUNT(*), 0 FROM exam_answer WHERE record_id >= 100;
