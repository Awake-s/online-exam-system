-- ============================================================
-- 在线考试系统 · 苏科大天平学院真实场景种子数据 · P3 试卷与模板（清空版）
-- v2.2 教师后台业务数据置空
-- ============================================================
-- v2.2 行为变更（与 v2.1 相比）：
--   · 不再预置任何试卷/模板/题目关联
--   · 仅保留 DELETE 清理动作，确保从 v2.1 升级时旧数据被清空
--   · 教师可在前端「试卷管理」「模板管理」自行从 P2 真实题库（899 题）创建
--
-- 清空范围（与表的外键依赖逆序一致）：
--   exam_template_rule    模板组卷规则
--   exam_paper_template   试卷模板
--   exam_paper_question   试卷-题目关联
--   exam_paper            试卷
--
-- 前置依赖：P1（基础数据）+ P2（v2.2 真实题库 899 题）
-- ============================================================

USE online_exam_system;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- 清空教师后台「试卷模板」「试卷」业务数据
DELETE FROM exam_template_rule  WHERE template_id >= 100;
DELETE FROM exam_paper_template WHERE id >= 100;
DELETE FROM exam_paper_question WHERE paper_id >= 100;
DELETE FROM exam_paper          WHERE id >= 100;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

-- ============================================================
-- 验证断言（应全部为 0）
-- ============================================================
SELECT '试卷'          AS 实体, COUNT(*) AS 实际, 0 AS 预期 FROM exam_paper          WHERE id         >= 100
UNION ALL SELECT '试卷-题目关联', COUNT(*),  0 FROM exam_paper_question WHERE paper_id   >= 100
UNION ALL SELECT '试卷模板',     COUNT(*),  0 FROM exam_paper_template WHERE id         >= 100
UNION ALL SELECT '模板组卷规则', COUNT(*),  0 FROM exam_template_rule  WHERE template_id >= 100;
