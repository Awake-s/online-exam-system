-- ============================================================================
-- 10_课程名数字汉化.sql
--
-- 背景:
--   学校实际课程名使用中文数字 "大学体育(一)" 而非阿拉伯数字 "大学体育(1)".
--   现有数据中以下 8 种命名共 432 条记录需要汉化:
--     大学体育(1)-(4)  各 56 条 (28 专业 × 2 年级)
--     大学英语(1)-(4)  各 52 条 (26 非外语专业 × 2 年级)
--
-- 注: 课程名 "高等数学A(一)" 等本身就用中文数字, 已经合规, 不动.
-- ============================================================================

USE online_exam_system;

START TRANSACTION;

-- 大学体育 (1)-(4) → (一)-(四)
UPDATE edu_subject SET subject_name = REPLACE(subject_name, '(1)', '(一)')
 WHERE subject_name = '大学体育(1)';
UPDATE edu_subject SET subject_name = REPLACE(subject_name, '(2)', '(二)')
 WHERE subject_name = '大学体育(2)';
UPDATE edu_subject SET subject_name = REPLACE(subject_name, '(3)', '(三)')
 WHERE subject_name = '大学体育(3)';
UPDATE edu_subject SET subject_name = REPLACE(subject_name, '(4)', '(四)')
 WHERE subject_name = '大学体育(4)';

-- 大学英语 (1)-(4) → (一)-(四)
UPDATE edu_subject SET subject_name = REPLACE(subject_name, '(1)', '(一)')
 WHERE subject_name = '大学英语(1)';
UPDATE edu_subject SET subject_name = REPLACE(subject_name, '(2)', '(二)')
 WHERE subject_name = '大学英语(2)';
UPDATE edu_subject SET subject_name = REPLACE(subject_name, '(3)', '(三)')
 WHERE subject_name = '大学英语(3)';
UPDATE edu_subject SET subject_name = REPLACE(subject_name, '(4)', '(四)')
 WHERE subject_name = '大学英语(4)';

COMMIT;

-- ============================================================================
-- 验证
-- ============================================================================

-- 应当为 0: 不应再有任何课程名含半角数字括号
SELECT COUNT(*) AS '残留半角数字课程数'
  FROM edu_subject
 WHERE subject_name REGEXP '\\([0-9]+\\)';

-- 抽样: 全部新风格命名
SELECT DISTINCT subject_name
  FROM edu_subject
 WHERE subject_name LIKE '大学体育%' OR subject_name LIKE '大学英语%'
 ORDER BY subject_name;

-- (grade, major_id, subject_name) 联合唯一性检查
SELECT grade, major_id, subject_name, COUNT(*) AS cnt
  FROM edu_subject
 WHERE grade IS NOT NULL
 GROUP BY grade, major_id, subject_name
HAVING cnt > 1;
