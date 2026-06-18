-- ============================================================================
-- 08_课程名简化_去除冗余后缀.sql
--
-- 背景:
--   07 脚本初版给所有课程名加了 "_年级_专业简称" 后缀以保证 subject_name 全局唯一
--   (例: "形势与政策_2024级_计科"). 这种命名冗余, 因为 grade + major_id 已经承担
--   区分职责. 课程名本身就是 "形势与政策"——视觉、模型双双更优.
--
-- 影响:
--   1820 条课程数据的 subject_name 去后缀.
--   旧 14 条数据 (id 100-113) 本身就是无后缀的简化名 (如"高等数学(一)"), 不动.
--   现有 SQL 文件 07_课程数据_22级与24级真实课表.sql 已同步更新为不带后缀.
--
-- 关联代码:
--   工具_生成课程数据.py 中 build_full_subject_name() 已改为返回原课程名.
-- ============================================================================

USE online_exam_system;

START TRANSACTION;

-- 用正则一次性剥离 "_(2022|2024)级_<专业简称>" 后缀
-- 专业简称为非下划线连续字符, 行尾锚定确保只截最后一段
UPDATE edu_subject
   SET subject_name = REGEXP_REPLACE(subject_name, '_20[0-9]{2}级_[^_]+$', '')
 WHERE subject_name REGEXP '_20[0-9]{2}级_[^_]+$';

COMMIT;

-- ============================================================================
-- 验证
-- ============================================================================

-- 应当为 0: 不应再有任何课程名带 _年级_专业 后缀
SELECT COUNT(*) AS '残留后缀数据数'
  FROM edu_subject
 WHERE subject_name REGEXP '_20[0-9]{2}级_[^_]+$';

-- 抽样: 计算机科学与技术 2022 级前 10 门课
-- v7 KISS 减肥: course_type / semester 字段已删, 抽样字段同步精简
SELECT id, subject_name, grade, exam_type
  FROM edu_subject
 WHERE major_id = 100 AND grade = '2022级'
 ORDER BY id
 LIMIT 10;

-- 跨专业重名验证: 同一门"形势与政策"应在 28 专业 × 2 年级 = 56 条出现
SELECT subject_name, COUNT(*) AS '出现次数'
  FROM edu_subject
 WHERE subject_name = '形势与政策'
 GROUP BY subject_name;

-- (grade, major_id, subject_name) 联合唯一性验证: 不应有重复
SELECT grade, major_id, subject_name, COUNT(*) AS cnt
  FROM edu_subject
 WHERE grade IS NOT NULL
 GROUP BY grade, major_id, subject_name
HAVING cnt > 1;
