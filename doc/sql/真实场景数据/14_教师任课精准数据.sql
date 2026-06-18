-- ============================================================
-- 14_教师任课精准数据（teacher_subject 重新填充）
-- ------------------------------------------------------------
-- 背景：
--   v6 升级把 edu_subject 改为 (grade, major_id, subject_name) 三元组后,
--   原 01 号脚本里的 teacher_subject 数据已经 stale (映射的是旧抽象科目);
--   13 号脚本 TRUNCATE 清空后, 此脚本按真实大学教务规律重新填充。
--
-- 设计原则：
--   1) 每位教师精准任课 1-2 门, 不再"覆盖整个专业的全部课程"
--   2) 教师任课的科目 (grade, major_id) 必须在该教师任课班级集合内
--      —— 与 UserServiceImpl.filterSubjectsByCandidatePool 校验一致
--   3) 兼职跨专业 (如何婷婷同时负责电气+土木+日语 6 个班) 的教师,
--      只挑其本专业 1-2 门挂任课, 避免"全包"假象
--
-- 真实师资规划 (按苏科大天平学院计算机/通信/电子等系真实教务安排):
--   - 100 王建华: 高数组, 24 级计算机 高数A(一)/(二)
--   - 117 林涵琳: 24 级计算机 Java EE + 软件工程 (与 v1 一致)
--   - 690 陆卫忠: 22 级计算机 Java EE + 面向对象技术 (用户明确指定)
--   - 其余 22 人各按科目组 1-2 门
--
-- 自检：执行后总行数应 = 25 个教师 × 2 = 50 行 (除个别教师 1 门外)
-- ============================================================

START TRANSACTION;

-- 安全幂等：先清空, 与 13 号脚本配合 (允许独立反复执行)
DELETE FROM teacher_subject;

-- ──────────────────── 24 级·计算机科学与技术 (major_id=100) ────────────────────
-- 王建华 (id=100, 班级 100/101): 高数组
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(100, 100),  -- 24计算机·高等数学A(一)
(100, 187);  -- 24计算机·高等数学A(二)

-- 徐玲华 (id=107, 班级 100-105): 大学英语组, 给计算机方向 2 门
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(107, 101),  -- 24计算机·大学英语(一)
(107, 183);  -- 24计算机·大学英语(二)

-- 陈晓峰 (id=109, 班级 100-103): 离散+编译
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(109, 103),  -- 24计算机·离散结构
(109, 111);  -- 24计算机·编译原理

-- 刘芳 (id=110, 班级 100/101): 数据结构
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(110, 104);  -- 24计算机·数据结构

-- 杨志华 (id=112, 班级 100/101/104/105): 操作系统+计组
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(112, 105),  -- 24计算机·操作系统
(112, 106);  -- 24计算机·计算机组成原理

-- 赵明宇 (id=114, 班级 100/101): 网络+AI
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(114, 107),  -- 24计算机·计算机网络原理及工程应用
(114, 112);  -- 24计算机·人工智能基础

-- 周强 (id=116, 班级 100/101): 数据库
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(116, 108);  -- 24计算机·数据库原理

-- 林涵琳 (id=117, 班级 100-103): Java EE+软工 (v1 经典)
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(117, 109),  -- 24计算机·Java EE 开发技术基础
(117, 110);  -- 24计算机·软件工程

-- 钱志勇 (id=121, 班级 100/101): 算法基础+面向对象
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(121, 193),  -- 24计算机·算法与程序设计基础
(121, 192);  -- 24计算机·面向对象技术

-- 罗敏峰 (id=123, 班级 100/101): 概率+线代
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(123, 188),  -- 24计算机·概率论与数理统计
(123, 102);  -- 24计算机·线性代数

-- ──────────────────── 24 级·通信工程 (major_id=101) ────────────────────
-- 李春梅 (id=104, 班级 100-103 跨计算机+通信): 高数组, 给通信方向 2 门
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(104, 265),  -- 24通信·高等数学A(一)
(104, 266);  -- 24通信·高等数学A(二)

-- 张文亮 (id=111, 班级 102/103): 通信原理+移动通信
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(111, 276),  -- 24通信·通信原理
(111, 279);  -- 24通信·移动通信

-- 马晓明 (id=113, 班级 102/103): 信号系统+DSP
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(113, 275),  -- 24通信·信号与系统
(113, 277);  -- 24通信·数字信号处理

-- 黄丽 (id=115, 班级 102/103): 电路+模电
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(115, 272),  -- 24通信·电路分析
(115, 273);  -- 24通信·模拟电子技术

-- 徐东林 (id=118, 班级 102/103): 数电+电磁场
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(118, 274),  -- 24通信·数字电子技术
(118, 278);  -- 24通信·电磁场与电磁波

-- 王秀兰 (id=119, 班级 102/103): 大学英语(一)/(二)
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(119, 261),  -- 24通信·大学英语(一)
(119, 262);  -- 24通信·大学英语(二)

-- 孙东辰 (id=122, 班级 102/103): 大学英语(三)/(四)
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(122, 263),  -- 24通信·大学英语(三)
(122, 264);  -- 24通信·大学英语(四)

-- ──────────────────── 24 级·电子信息工程 (major_id=102) ────────────────────
-- 孙海燕 (id=101, 班级 104-107 跨电子+机械): 给电子 2 门
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(101, 342),  -- 24电子·微机原理及接口技术
(101, 345);  -- 24电子·嵌入式系统

-- 郭秀梅 (id=105, 班级 104-107 跨电子+机械): 给电子 2 门
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(105, 343),  -- 24电子·数字信号处理
(105, 340);  -- 24电子·数字电子技术

-- ──────────────────── 24 级·机械设计制造及其自动化 (major_id=103) ────────────────────
-- 朱鹏飞 (id=108, 班级 106-111 跨机械+电气+土木): 主挂机械 2 门
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(108, 406),  -- 24机械·机械原理
(108, 407);  -- 24机械·机械设计

-- ──────────────────── 24 级·电气工程及其自动化 (major_id=104) ────────────────────
-- 吴俊华 (id=102, 班级 108/109): 电机学+电力电子
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(102, 473),  -- 24电气·电机学
(102, 474);  -- 24电气·电力电子技术

-- ──────────────────── 24 级·土木工程 (major_id=105) ────────────────────
-- 胡建军 (id=103, 班级 110/111): 土木施工+结构力学
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(103, 543),  -- 24土木·土木工程施工
(103, 539);  -- 24土木·结构力学

-- ──────────────────── 24 级·日语 (major_id=106) ────────────────────
-- 何婷婷 (id=106, 班级 108-113 跨电气+土木+日语): 主挂日语 2 门
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(106, 586),  -- 24日语·基础日语(一)
(106, 587);  -- 24日语·基础日语(二)

-- 马晓燕 (id=120, 班级 112/113): 日语听力
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
(120, 588),  -- 24日语·日语听力(一)
(120, 589);  -- 24日语·日语听力(二)

-- 22 级·计算机 陆卫忠 (id=14) 任课见 18_22级教师任课关系.sql（须在 16 之后执行）

COMMIT;

-- ============================================================
-- 自检 SQL: 总行数 + 按教师分组统计
-- ============================================================
SELECT COUNT(*) AS '插入后 teacher_subject 总行数 (期望 47)' FROM teacher_subject;

SELECT u.id, u.real_name,
       COUNT(ts.subject_id) AS subject_count,
       GROUP_CONCAT(s.subject_name ORDER BY s.id SEPARATOR '、') AS subjects
FROM sys_user u
LEFT JOIN teacher_subject ts ON ts.teacher_id = u.id
LEFT JOIN edu_subject s ON s.id = ts.subject_id
WHERE u.role_id = 2
GROUP BY u.id, u.real_name
ORDER BY u.id;
