-- ============================================================
-- 15_精简 edu_subject (KISS 减肥 / 去教务化字段)
-- ------------------------------------------------------------
-- 背景：
--   v6 升级时 (06 号脚本) 给 edu_subject 加了 7 个教务化字段:
--     grade / major_id / course_type / credit / hours / semester / exam_type
--   其中 grade + major_id 在 OES 业务中真实被消费 (出题/考试/成绩按年级专业划分),
--   其它 5 个 (course_type / credit / hours / semester / exam_type) 都是
--   纯教务概念, OES 业务零依赖。
--
-- v7 决策 (KISS / 系统所有权 SoR 原则):
--   - 物理删除 3 列: course_type / credit / semester
--     -> 学分/学期/课程类型的权威所有者是教务系统, OES 重复维护必然漂移
--     -> 删除后管理员表单更轻, 答辩演示故事更聚焦
--   - 暂保留 hours / exam_type 两列 (前端早已不展示, 但保留方便万一回滚)
--   - grade / major_id 是 OES 业务必需, 保留
--
-- 影响面：
--   1) 数据库 schema: 删 3 列 + 重建索引 (原索引含 course_type)
--   2) 后端: EduSubject 实体 / SubjectAddRequest / Service / Controller 同步删字段
--   3) 前端: subject-manage 删表单/表格/筛选; user-manage 候选下拉降为二级分组
--   4) 种子 SQL (07 号): 同步删 INSERT 列定义 + 值
--   5) Python 生成器 (工具_生成课程数据.py): 同步删字段
--
-- 幂等性：
--   - 用 IF EXISTS / COLUMN_NAME 检查保证可反复执行
--   - 不影响 grade / major_id 业务数据
-- ============================================================

-- 1. 先删旧索引 idx_subject_grade_major (含 course_type 列, 删了字段索引会失败)
SET @idx_exists := (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'edu_subject'
                      AND INDEX_NAME = 'idx_subject_grade_major');
SET @sql := IF(@idx_exists > 0,
  'ALTER TABLE edu_subject DROP INDEX idx_subject_grade_major',
  'SELECT ''idx_subject_grade_major 不存在, 跳过 DROP'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 删 course_type 列
SET @col_exists := (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'edu_subject'
                      AND COLUMN_NAME = 'course_type');
SET @sql := IF(@col_exists > 0,
  'ALTER TABLE edu_subject DROP COLUMN course_type',
  'SELECT ''course_type 不存在, 跳过 DROP'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 删 credit 列
SET @col_exists := (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'edu_subject'
                      AND COLUMN_NAME = 'credit');
SET @sql := IF(@col_exists > 0,
  'ALTER TABLE edu_subject DROP COLUMN credit',
  'SELECT ''credit 不存在, 跳过 DROP'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 删 semester 列
SET @col_exists := (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'edu_subject'
                      AND COLUMN_NAME = 'semester');
SET @sql := IF(@col_exists > 0,
  'ALTER TABLE edu_subject DROP COLUMN semester',
  'SELECT ''semester 不存在, 跳过 DROP'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. 重建索引 (只索引 grade + major_id, 服务 listSubjects 二维过滤)
SET @idx_exists := (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'edu_subject'
                      AND INDEX_NAME = 'idx_subject_grade_major');
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE edu_subject ADD KEY idx_subject_grade_major (grade, major_id)',
  'SELECT ''idx_subject_grade_major 已存在, 跳过 CREATE'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 自检：确认 schema 干净 + 数据完整性 (业务字段不应受影响)
-- ============================================================
SELECT '== 当前 edu_subject 列定义 ==' AS msg;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_subject'
ORDER BY ORDINAL_POSITION;

SELECT '== 课程总数 (应保持不变) ==' AS msg;
SELECT COUNT(*) AS total_count FROM edu_subject;

SELECT '== 按年级分布 ==' AS msg;
SELECT grade, COUNT(*) AS cnt FROM edu_subject GROUP BY grade ORDER BY grade;

SELECT '== 关键课程定位验证 (Java EE) ==' AS msg;
SELECT id, grade, major_id, subject_name, description
FROM edu_subject
WHERE subject_name = 'Java EE 开发技术基础'
ORDER BY grade, major_id;
