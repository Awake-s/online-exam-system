-- ============================================================
-- 06_科目表升级_年级专业课程化.sql
--   苏州科技大学天平学院 v3.0：edu_subject 表升级为「具体课程」语义
--
-- 设计目标：
--   每条 edu_subject 记录从「抽象科目」升级为「具体课程实例」
--   一条记录 = (年级 + 专业 + 课程名 + 学分 + 学时 + 学期 + 类型 + 考核方式)
--   → 解决用户痛点：22级Java EE 与 24级Java EE 课程分离，不同年级不同任课教师
--
-- 升级策略（无损向后兼容）：
--   1. ALTER 加 4 个可空字段：grade/major_id/hours/exam_type (v7 KISS 已删 course_type/credit/semester)
--   2. 旧 14 条 edu_subject (id 100-113) 补全字段为「2024级·计算机/日语·具体课程」
--      —— 因为现有 14 个班级和 28 条 teacher_subject 都是 2024 级计科/日语方向
--   3. subject_major 表保留（兼容旧多对多关联，新数据不写入此表）
--   4. teacher_subject 表保留（一条 (教师, 科目id) 已能定位到具体课程）
--
-- 数据来源：
--   苏州科技大学天平学院计算机科学与技术专业 2018 版培养方案 PDF
--   https://tpxy.usts.edu.cn/__local/1/2F/5A/E3291BEAA9AF1182F8B55DC4D07_AC5EB455_4BAAA.pdf
--
-- ⚠ 这是「数据模型升级」，必须先执行此文件，再执行后续课程数据填充
-- ============================================================

USE online_exam_system;

SET NAMES utf8mb4;

-- ============================================================
-- 一、ALTER edu_subject 表增加 7 个字段
-- ============================================================
-- 幂等保护：先检查字段是否已存在，避免重复执行报错
-- MySQL 8.0+ 支持 IF NOT EXISTS（用 information_schema 校验）

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'edu_subject'
                      AND COLUMN_NAME = 'grade');
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE edu_subject
     ADD COLUMN grade VARCHAR(20) NULL COMMENT ''年级，如2022级、2024级；NULL 表示通用抽象科目（兼容旧数据）'' AFTER subject_name,
     ADD COLUMN major_id BIGINT NULL COMMENT ''所属专业ID（FK edu_major.id）；NULL 表示通用'' AFTER grade,
     ADD COLUMN hours INT NULL COMMENT ''总学时'' AFTER major_id,
     ADD COLUMN exam_type VARCHAR(10) NULL COMMENT ''考核方式：试/查'' AFTER hours',
  'SELECT ''edu_subject 字段已存在，跳过 ALTER'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 加索引：按 grade + major_id 组合查询是高频路径
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'edu_subject'
                      AND INDEX_NAME = 'idx_subject_grade_major');
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE edu_subject ADD KEY idx_subject_grade_major (grade, major_id)',
  'SELECT ''idx_subject_grade_major 索引已存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 二、升级旧 14 条 edu_subject 为「2024级·计算机科学与技术 / 日语·具体课程」
--   数据来源：天平学院计算机科学与技术专业 2018 版培养方案 PDF
--   学院日语专业培养方案
--
--   编号映射：
--     100 高等数学(一)             → 24级 计科 通识必修 5学分/80学时/第1学期/试
--     101 大学英语                 → 24级 计科 通识必修 4学分/64学时/第1学期/试
--     102 线性代数                 → 24级 计科 通识必修 3学分/48学时/第2学期/试
--     103 离散结构                 → 24级 计科 学科基础 3学分/48学时/第2学期/查
--     104 数据结构                 → 24级 计科 学科基础 4学分/64学时/第3学期/试
--     105 操作系统                 → 24级 计科 学科基础 3学分/52学时/第4学期/试
--     106 计算机组成原理           → 24级 计科 学科基础 4学分/72学时/第4学期/试
--     107 计算机网络原理及工程应用 → 24级 计科 学科基础 4学分/72学时/第5学期/试
--     108 数据库原理               → 24级 计科 学科基础 3学分/56学时/第4学期/试
--     109 Java EE 开发技术基础     → 24级 计科 专业核心 4学分/72学时/第5学期/试
--     110 软件工程                 → 24级 计科 专业核心 3学分/56学时/第5学期/查
--     111 编译原理                 → 24级 计科 专业核心 2学分/36学时/第6学期/试
--     112 人工智能基础             → 24级 计科 专业核心 3学分/56学时/第6学期/查
--     113 日语精读                 → 24级 日语 专业核心 4学分/64学时/第3学期/试
-- ============================================================

-- v8 描述统一: 14 条 description 与 07 文件 build_description() 输出完全同构
--   格式: [课程类型] · [专业简称][年级]第[N]学期 · [学分]学分/[学时]学时 · [核心内容]
--   exam_type 统一改为 '试' (与 07 一致, OES 不区分考试/考查)
UPDATE edu_subject SET grade='2024级', major_id=100, hours=80, exam_type='试',
  description='通识必修 · 计科24级第1学期 · 5学分/80学时 · 工科基础高数A' WHERE id=100;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=64, exam_type='试',
  description='通识必修 · 计科24级第1学期 · 4学分/64学时 · 公共外语全校必修' WHERE id=101;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=48, exam_type='试',
  description='通识必修 · 计科24级第2学期 · 3学分/48学时 · 矩阵与方程组' WHERE id=102;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=48, exam_type='试',
  description='学科基础必修 · 计科24级第2学期 · 3学分/48学时 · 命题逻辑/集合论/图论/组合数学' WHERE id=103;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=64, exam_type='试',
  description='学科基础必修 · 计科24级第3学期 · 4学分/64学时 · 线性表/树/图/排序查找' WHERE id=104;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=52, exam_type='试',
  description='学科基础必修 · 计科24级第4学期 · 3学分/52学时 · 进程管理/内存管理/文件系统' WHERE id=105;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=72, exam_type='试',
  description='学科基础必修 · 计科24级第4学期 · 4学分/72学时 · CPU/存储/IO/总线' WHERE id=106;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=72, exam_type='试',
  description='学科基础必修 · 计科24级第5学期 · 4学分/72学时 · OSI七层/TCP-IP/路由协议' WHERE id=107;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=56, exam_type='试',
  description='学科基础必修 · 计科24级第4学期 · 3学分/56学时 · ER模型/SQL/规范化/事务处理' WHERE id=108;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=72, exam_type='试',
  description='专业核心必修 · 计科24级第5学期 · 4学分/72学时 · SSM/Spring 框架' WHERE id=109;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=56, exam_type='试',
  description='专业核心必修 · 计科24级第5学期 · 3学分/56学时 · 软件过程/需求/设计/测试' WHERE id=110;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=36, exam_type='试',
  description='专业核心必修 · 计科24级第6学期 · 2学分/36学时 · 词法/语法/语义分析/代码生成' WHERE id=111;
UPDATE edu_subject SET grade='2024级', major_id=100, hours=56, exam_type='试',
  description='专业核心必修 · 计科24级第6学期 · 3学分/56学时 · 搜索/机器学习/神经网络入门' WHERE id=112;
UPDATE edu_subject SET grade='2024级', major_id=106, hours=64, exam_type='试',
  description='专业核心必修 · 日语24级第3学期 · 4学分/64学时 · 综合日语精读训练' WHERE id=113;

-- ============================================================
-- 三、验证 ALTER + UPDATE 结果
-- ============================================================
SELECT 'edu_subject 表结构升级' AS step, COUNT(*) AS total_rows,
       SUM(CASE WHEN grade IS NOT NULL THEN 1 ELSE 0 END) AS upgraded_rows,
       SUM(CASE WHEN grade IS NULL THEN 1 ELSE 0 END) AS legacy_rows
FROM edu_subject;

SELECT id, subject_name, grade, major_id, hours, exam_type
FROM edu_subject ORDER BY id;
