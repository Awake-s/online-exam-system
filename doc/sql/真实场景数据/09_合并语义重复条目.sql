-- ============================================================================
-- 09_合并语义重复条目.sql
--
-- 背景:
--   去后缀后, 旧 14 条遗留科目 (id 100-113) 与新模板生成数据存在语义重叠:
--   - id=100 "高等数学（一）"  ≡  新 id=186 "高等数学A(一)"     (24级·计算机·5/80/学期1)
--   - id=101 "大学英语"          ≡  新 id=182 "大学英语(1)"       (24级·计算机·4/64/学期1)
--
--   旧 id 上挂载有题库数据 (如 id=100 已有 300 题), 直接删除会丢失题库.
--   方案: 旧 id 改名为模板标准命名, 删除新插入的语义重复条目.
-- ============================================================================

USE online_exam_system;

START TRANSACTION;

-- 1. 旧 100 高等数学（一） → 模板标准名 "高等数学A(一)" (匹配工科基础)
UPDATE edu_subject
   SET subject_name = '高等数学A(一)',
       description  = '工科基础·5学分高数A'
 WHERE id = 100;

-- 删除新模板对 24 级·计算机·高等数学A(一) 的重复插入
DELETE FROM edu_subject
 WHERE id != 100
   AND grade = '2024级'
   AND major_id = 100
   AND subject_name = '高等数学A(一)';

-- 2. 旧 101 大学英语 → 模板标准名 "大学英语(1)" (24级第1学期 4学分)
UPDATE edu_subject
   SET subject_name = '大学英语(1)',
       description  = '公共外语·第1学期'
 WHERE id = 101;

-- 删除新模板对 24 级·计算机·大学英语(1) 的重复插入
DELETE FROM edu_subject
 WHERE id != 101
   AND grade = '2024级'
   AND major_id = 100
   AND subject_name = '大学英语(1)';

-- 3. 旧 113 日语精读 → 24 级·日语·专业核心, 与日语模板的"高级日语"不同, 保持原名称
--    (日语精读是某一学期的单独课程, 与高级日语区分; 无需合并)

COMMIT;

-- ============================================================================
-- 验证
-- ============================================================================

-- 应当为 0
SELECT grade, major_id, subject_name, COUNT(*) AS cnt
  FROM edu_subject
 WHERE grade IS NOT NULL
 GROUP BY grade, major_id, subject_name
HAVING cnt > 1;

-- 抽样: 24 级·计算机 第 1 学期通识必修
-- v7 KISS 减肥: course_type / credit / semester 字段已删, 抽样降为按 (grade, major_id, subject_name LIKE)
SELECT id, subject_name, hours
  FROM edu_subject
 WHERE grade = '2024级' AND major_id = 100
   AND (subject_name LIKE '高等数学A(一)%' OR subject_name LIKE '大学英语(1)%')
 ORDER BY id;

-- 整体计数
SELECT COUNT(*) AS '科目总数' FROM edu_subject;
