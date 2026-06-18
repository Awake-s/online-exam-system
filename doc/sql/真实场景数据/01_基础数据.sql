-- ============================================================
-- 在线考试系统 · 苏科大天平学院真实场景种子数据 · P1 基础数据 (v2.1)
-- ============================================================
-- 学校原型：苏州科技大学天平学院 (TPSUST) — 七院一部、独立学院模式
-- 国标代码：13985（教育部）／省内招生代码：1835（江苏）
-- 规模档次：L3 真实级（专业 7 + 科目 14 + 班级 14 + 教师 24 + 学生 490）
--
-- v2.1 关键改进（v2.0 → v2.1，与学校真实数据严格对齐）：
--   1. 学生用户名（username）从『24CS01001』改为 10 位真实学号格式：
--        YY(年级2) + 专业代码(2) + 1(本科) + 学院代码(2) + 班号(1) + 班内序号(2)
--      · 专业代码取自学校 2024/2025 江苏招生计划表（招生代码 1835）
--      · 例：『2430107203』= 24 级计算机科学与技术 2 班 03 号（与陶展同款）
--   2. 教师用户名从姓名拼音改为 6 位真实工号格式：
--        YY(入职年2) + 学院代码(2) + 学院内序号(2)
--      · 例：『080201』= 08 年入职、02 数理学院、第 1 号老师（王建华）
--   3. 专业 101 软件工程 → 通信工程（天平学院实际未开设软件工程；
--      通信工程归属电子与信息工程学院，与软件工程在公共课/学科基础课
--      重合度极高，学生/教师/题库零损失迁移）
--   4. 邮箱后缀保留：学生 @stu.usts-tp.edu.cn，教师 @usts-tp.edu.cn
--
-- 专业招生代码对照（来自学校 2025 江苏招生计划表）：
--   30 = 计算机科学与技术（电信院 07）
--   32 = 通信工程         （电信院 07，本版替换软工）
--   29 = 电子信息工程     （电信院 07）
--   33 = 机械设计制造及其自动化（智能制造与电气工程学院 08）
--   31 = 电气工程及其自动化   （智能制造与电气工程学院 08）
--   28 = 土木工程             （城市建设学院 03）
--   10 = 日语                 （外国语学院 05）
--
-- 学院代码（学号位 5-6 / 工号位 3-4）：
--   02 数理学院               07 电子与信息工程学院
--   03 城市建设学院           08 智能制造与电气工程学院
--   04 外国语学院             05 文法/语言文化学院（日语归属）
--
-- 实施策略：B 增量补充（保留 1-99 ID，新增 ID 从 100 起步）
--   · 教师 ID：100-123（24 人），username 改为 6 位工号
--   · 学生 ID：200-689（490 人），username 改为 10 位真学号
--   · 实现方式：先用旧式 username 完成 INSERT 写入，最后用 14 条按班
--     UPDATE + 24 条按教师 UPDATE 一次性映射为真实学号/工号，保证 SQL
--     文件可读性 + UNIQUE 约束不冲突 + 完全幂等
-- ============================================================

USE online_exam_system;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- 清理本脚本曾经写入的数据（ID >= 100），保证脚本可重复执行
DELETE FROM teacher_subject WHERE teacher_id >= 100;
DELETE FROM teacher_class   WHERE teacher_id >= 100;
DELETE FROM subject_major   WHERE subject_id >= 100 OR major_id >= 100;
DELETE FROM sys_user        WHERE id >= 100;
DELETE FROM edu_class       WHERE id >= 100;
DELETE FROM edu_subject     WHERE id >= 100;
DELETE FROM edu_major       WHERE id >= 100;

-- ============================================================
-- 1. 专业 edu_major × 7
-- ============================================================
-- 招生代码取自苏科大天平学院 2025 江苏招生计划表（招生代码 1835）
INSERT INTO edu_major (id, major_name, description) VALUES
(100, '计算机科学与技术', '电子与信息工程学院 · 招生代码 30 · 学制四年 · 工学学士 · 培养计算机软硬件结合应用型本科人才'),
(101, '通信工程',         '电子与信息工程学院 · 招生代码 32 · 学制四年 · 工学学士 · 培养现代通信系统、信号处理与无线通信工程师'),
(102, '电子信息工程',     '电子与信息工程学院 · 招生代码 29 · 学制四年 · 工学学士 · 培养电子电路与信号处理工程师'),
(103, '机械设计制造及其自动化', '智能制造与电气工程学院 · 招生代码 33 · 学制四年 · 工学学士 · 江苏省一流（特色）专业建设点'),
(104, '电气工程及其自动化',     '智能制造与电气工程学院 · 招生代码 31 · 学制四年 · 工学学士 · 培养电力系统与电气控制工程师'),
(105, '土木工程',         '城市建设学院 · 招生代码 28 · 学制四年 · 工学学士 · 江苏省品牌工程三期建设项目'),
(106, '日语',             '外国语学院 · 招生代码 10/23 · 学制四年 · 文学学士 · 江苏省一流（特色）专业建设点');

-- ============================================================
-- 2. 科目 edu_subject × 14
-- ============================================================
INSERT INTO edu_subject (id, subject_name, description) VALUES
(100, '高等数学（一）',          '通识基础课 · 5 学分 · 工科专业必修（日语专业不修）'),
(101, '大学英语',                '通识基础课 · 4 学分 · 全校必修'),
(102, '线性代数',                '通识基础课 · 3 学分 · 工科专业必修（日语专业不修）'),
(103, '离散结构',                '学科基础课 · 4 学分 · 计科/软工必修'),
(104, '数据结构',                '学科基础课 · 4 学分 · 计科/软工核心'),
(105, '操作系统',                '学科基础课 · 4 学分 · 计科/软工核心'),
(106, '计算机组成原理',          '学科基础课 · 4 学分 · 计科/电信必修'),
(107, '计算机网络原理及工程应用','学科基础课 · 3 学分 · 计科/软工必修'),
(108, '数据库原理',              '学科基础课 · 4 学分 · 计科/软工核心'),
(109, 'Java EE 开发技术基础',    '专业核心课 · 4 学分 · 计科/软工'),
(110, '软件工程',                '专业核心课 · 3 学分 · 计科/软工'),
(111, '编译原理',                '专业选修课 · 4 学分 · 计科'),
(112, '人工智能基础',            '专业选修课 · 4 学分 · 计科'),
(113, '日语精读',                '专业核心课 · 4 学分 · 日语专业');

-- ============================================================
-- 3. 科目-专业关联 subject_major × 38
-- ============================================================
INSERT INTO subject_major (subject_id, major_id) VALUES
-- 高数 → 6 工科专业（日语不修）
(100,100),(100,101),(100,102),(100,103),(100,104),(100,105),
-- 大学英语 → 全部 7 专业
(101,100),(101,101),(101,102),(101,103),(101,104),(101,105),(101,106),
-- 线性代数 → 6 工科专业（日语不修）
(102,100),(102,101),(102,102),(102,103),(102,104),(102,105),
-- 离散结构 → 计/通
(103,100),(103,101),
-- 数据结构 → 计/通
(104,100),(104,101),
-- 操作系统 → 计/通
(105,100),(105,101),
-- 计算机组成原理 → 计/电信
(106,100),(106,102),
-- 计算机网络 → 计/通
(107,100),(107,101),
-- 数据库原理 → 计/通
(108,100),(108,101),
-- Java EE → 计/通
(109,100),(109,101),
-- 软件工程（科目）→ 计/通
(110,100),(110,101),
-- 编译原理 → 计
(111,100),
-- 人工智能基础 → 计
(112,100),
-- 日语精读 → 日
(113,106);

-- ============================================================
-- 4. 班级 edu_class × 14（每班 35 人，符合天平学院实际规模）
-- ============================================================
INSERT INTO edu_class (id, class_name, grade, major_id, description) VALUES
(100, '计算机2401',  '2024级', 100, '计算机科学与技术专业2024级1班'),
(101, '计算机2402',  '2024级', 100, '计算机科学与技术专业2024级2班'),
(102, '通信工程2401','2024级', 101, '通信工程专业2024级1班'),
(103, '通信工程2402','2024级', 101, '通信工程专业2024级2班'),
(104, '电子信息2401','2024级', 102, '电子信息工程专业2024级1班'),
(105, '电子信息2402','2024级', 102, '电子信息工程专业2024级2班'),
(106, '机械2401',    '2024级', 103, '机械设计制造及其自动化2024级1班'),
(107, '机械2402',    '2024级', 103, '机械设计制造及其自动化2024级2班'),
(108, '电气2401',    '2024级', 104, '电气工程及其自动化2024级1班'),
(109, '电气2402',    '2024级', 104, '电气工程及其自动化2024级2班'),
(110, '土木2401',    '2024级', 105, '土木工程专业2024级1班'),
(111, '土木2402',    '2024级', 105, '土木工程专业2024级2班'),
(112, '日语2401',    '2024级', 106, '日语专业2024级1班'),
(113, '日语2402',    '2024级', 106, '日语专业2024级2班');

-- ============================================================
-- 5. 教师用户 sys_user × 24 (ID 100-123)
-- 密码统一为 123456（BCrypt 哈希复用现有盐值）
-- 教师配置说明：
--   高数 5 人 / 大学英语 4 人 / 线代 2 人 / 计算机专业 11 人 / 日语 1 人
--
-- ⚠ username 字段先以姓名拼音占位写入，文件末尾会用 24 条 UPDATE 一次性
--   映射为 6 位真实工号（YY 入职年 + 学院代码 02/03/04/07/08 + 学院内序号）
-- ============================================================
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
-- ── 高等数学组（5 人）──
(100, 'wangjianhua',    '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王建华',     NULL, 'wangjianhua@usts-tp.edu.cn',    '13800010001', 1, 2, NULL, 1),
(101, 'sunhaiyan',      '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙海燕',     NULL, 'sunhaiyan@usts-tp.edu.cn',      '13800010002', 2, 2, NULL, 1),
(102, 'wujunhua',       '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴俊华',     NULL, 'wujunhua@usts-tp.edu.cn',       '13800010003', 1, 2, NULL, 1),
(103, 'hujianjun',      '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡建军',     NULL, 'hujianjun@usts-tp.edu.cn',      '13800010004', 1, 2, NULL, 1),
(119, 'wangxiulan',     '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王秀兰',     NULL, 'wangxiulan@usts-tp.edu.cn',     '13800010020', 2, 2, NULL, 1),
-- ── 大学英语组（4 人，含跨科目教日语精读 1 人）──
(104, 'lichunmei',      '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李春梅',     NULL, 'lichunmei@usts-tp.edu.cn',      '13800010005', 2, 2, NULL, 1),
(105, 'guoxiumei',      '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭秀梅',     NULL, 'guoxiumei@usts-tp.edu.cn',      '13800010006', 2, 2, NULL, 1),
(106, 'hetingting',     '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何婷婷',     NULL, 'hetingting@usts-tp.edu.cn',     '13800010007', 2, 2, NULL, 1),
(120, 'maxiaoyan',      '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马晓燕',     NULL, 'maxiaoyan@usts-tp.edu.cn',      '13800010021', 2, 2, NULL, 1),
-- ── 线性代数组（2 人）──
(107, 'xulinghua',      '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐玲华',     NULL, 'xulinghua@usts-tp.edu.cn',      '13800010008', 2, 2, NULL, 1),
(108, 'zhupengfei',     '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱鹏飞',     NULL, 'zhupengfei@usts-tp.edu.cn',     '13800010009', 1, 2, NULL, 1),
-- ── 计算机专业课组（11 人）──
(109, 'chenxiaofeng',   '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈晓峰',     NULL, 'chenxiaofeng@usts-tp.edu.cn',   '13800010010', 1, 2, NULL, 1),
(110, 'liufang',        '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘芳',       NULL, 'liufang@usts-tp.edu.cn',        '13800010011', 2, 2, NULL, 1),
(111, 'zhangwenliang',  '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张文亮',     NULL, 'zhangwenliang@usts-tp.edu.cn',  '13800010012', 1, 2, NULL, 1),
(112, 'yangzhihua',     '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨志华',     NULL, 'yangzhihua@usts-tp.edu.cn',     '13800010013', 1, 2, NULL, 1),
(113, 'maxiaoming',     '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马晓明',     NULL, 'maxiaoming@usts-tp.edu.cn',     '13800010014', 1, 2, NULL, 1),
(114, 'zhaomingyu',     '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵明宇',     NULL, 'zhaomingyu@usts-tp.edu.cn',     '13800010015', 1, 2, NULL, 1),
(115, 'huangli',        '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄丽',       NULL, 'huangli@usts-tp.edu.cn',        '13800010016', 2, 2, NULL, 1),
(116, 'zhouqiang',      '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周强',       NULL, 'zhouqiang@usts-tp.edu.cn',      '13800010017', 1, 2, NULL, 1),
(117, 'linhanlin',      '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林涵琳',     NULL, 'linhanlin@usts-tp.edu.cn',      '13800010018', 2, 2, NULL, 1),
(118, 'xudonglin',      '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐东林',     NULL, 'xudonglin@usts-tp.edu.cn',      '13800010019', 1, 2, NULL, 1),
(121, 'qianzhiyong',    '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '钱志勇',     NULL, 'qianzhiyong@usts-tp.edu.cn',    '13800010022', 1, 2, NULL, 1),
(122, 'sundongchen',    '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙东辰',     NULL, 'sundongchen@usts-tp.edu.cn',    '13800010023', 1, 2, NULL, 1),
(123, 'luominfeng',     '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗敏峰',     NULL, 'luominfeng@usts-tp.edu.cn',     '13800010024', 1, 2, NULL, 1);

-- ============================================================
-- 6. 教师-科目关联 teacher_subject × 28
-- 反映「同一老师可教多门相关科目」的真实情况
-- ============================================================
INSERT INTO teacher_subject (teacher_id, subject_id) VALUES
-- 高数（5 名老师）
(100, 100), (101, 100), (102, 100), (103, 100), (119, 100),
-- 大学英语（4 名老师）
(104, 101), (105, 101), (106, 101), (120, 101),
-- 线代（2 名老师）
(107, 102), (108, 102),
-- 离散结构（陈晓峰）
(109, 103),
-- 数据结构（2 名老师）
(110, 104), (111, 104),
-- 操作系统（2 名老师）
(112, 105), (113, 105),
-- 计算机组成原理（杨志华兼 + 钱志勇）
(112, 106), (121, 106),
-- 计算机网络（2 名老师）
(114, 107), (115, 107),
-- 数据库原理（2 名老师）
(116, 108), (122, 108),
-- Java EE（林涵琳）
(117, 109),
-- 软件工程（2 名老师）
(118, 110), (123, 110),
-- 编译原理（陈晓峰兼）
(109, 111),
-- 人工智能基础（赵明宇兼）
(114, 112),
-- 日语精读（何婷婷兼）
(106, 113);

-- ============================================================
-- 7. 教师-班级任课关联 teacher_class × 72
-- 一师多班（公共课）+ 一师 1-2 班（专业核心课）
-- 说明：teacher_class 表语义为「教师与班级的任课关系」（不区分科目）
--   · 唯一约束 uk_teacher_class(teacher_id, class_id) → 同一教师在同一班只占 1 行
--   · 陈晓峰(109)、赵明宇(114) 在计100/101 上同时教 2 门课（离散+编译 / 计网+人工智能），
--     在 teacher_class 中只插 1 行，避免唯一约束冲突
-- ============================================================
INSERT INTO teacher_class (teacher_id, class_id) VALUES
-- ── 高等数学（12 个工科班，5 名老师按班级群分担）──
(100, 100), (100, 101),                                 -- 王建华 → 计100/101
(119, 102), (119, 103),                                 -- 王秀兰 → 通102/103
(101, 104), (101, 105), (101, 106), (101, 107),         -- 孙海燕 → 电信+机械
(102, 108), (102, 109),                                 -- 吴俊华 → 电气
(103, 110), (103, 111),                                 -- 胡建军 → 土木
-- ── 大学英语（14 个班全覆盖）──
(104, 100), (104, 101), (104, 102), (104, 103),         -- 李春梅 → 计+通
(105, 104), (105, 105), (105, 106), (105, 107),         -- 郭秀梅 → 电信+机械
(106, 108), (106, 109), (106, 110), (106, 111),         -- 何婷婷 → 电气+土木
(120, 112), (120, 113),                                 -- 马晓燕 → 日语
-- ── 线性代数（12 个工科班，2 名老师各教 6 个班）──
(107, 100), (107, 101), (107, 102), (107, 103), (107, 104), (107, 105),  -- 徐玲华
(108, 106), (108, 107), (108, 108), (108, 109), (108, 110), (108, 111),  -- 朱鹏飞
-- ── 离散结构（4 个班，陈晓峰一人覆盖）──
(109, 100), (109, 101), (109, 102), (109, 103),
-- ── 数据结构（4 个班，2 名老师各教 2 班）──
(110, 100), (110, 101),                                 -- 刘芳 → 计100/101
(111, 102), (111, 103),                                 -- 张文亮 → 通102/103
-- ── 操作系统（4 个班，2 名老师各教 2 班）──
(112, 100), (112, 101),                                 -- 杨志华 → 计100/101
(113, 102), (113, 103),                                 -- 马晓明 → 通102/103
-- ── 计算机组成原理（4 个班：计 + 电信）──
(121, 100), (121, 101),                                 -- 钱志勇 → 计100/101
(112, 104), (112, 105),                                 -- 杨志华兼 → 电信104/105
-- ── 计算机网络（4 个班）──
(114, 100), (114, 101),                                 -- 赵明宇 → 计100/101
(115, 102), (115, 103),                                 -- 黄丽 → 通102/103
-- ── 数据库原理（4 个班，2 名老师各教 2 班）──
(116, 100), (116, 101),                                 -- 周强 → 计100/101
(122, 102), (122, 103),                                 -- 孙东辰 → 通102/103
-- ── Java EE（4 个班，林涵琳一人覆盖）──
(117, 100), (117, 101), (117, 102), (117, 103),
-- ── 软件工程科目（4 个班，2 名老师各教 2 班）──
(123, 100), (123, 101),                                 -- 罗敏峰 → 计100/101
(118, 102), (118, 103),                                 -- 徐东林 → 通102/103
-- ── 编译原理（陈晓峰兼）：teacher_class(109,100/101) 已由「离散结构」覆盖，无需重复 ──
-- ── 人工智能基础（赵明宇兼）：teacher_class(114,100/101) 已由「计算机网络」覆盖，无需重复 ──
-- ── 日语精读（2 个日语班，何婷婷兼，与英语班不重叠）──
(106, 112), (106, 113);

-- ============================================================
-- 8. 学生用户 sys_user × 490 (ID 200-689)
-- 临时占位 username 规则：24 + 专业字母代码 + 班号(2) + 班内序号(3)
--   CS=计科 SE=占位（→通信） EI=电信 ME=机械 EE=电气 CE=土木 JP=日语
-- 每班 35 人，性别比按专业特点分布：
--   工科（计/通/电信/机/电气/土木）男女比 ≈ 7:3
--   日语班 男女比 ≈ 2:8
--
-- ⚠ 此处 username 仅作为 INSERT 阶段的临时占位（保证 UNIQUE 不冲突）。
--   文件末尾「v2.1 学号工号映射」章节会用 14 条按班 UPDATE 一次性把每位
--   学生的 username 与 email 同步替换为 10 位真实学号格式（与学校教务系统
--   完全对齐），见文件 #9 章节。
-- ============================================================

-- ──── 班 100 计算机2401（35 人，男 24 女 11）ID 200-234 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(200, '24CS01001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王志强', NULL, '24CS01001@stu.usts-tp.edu.cn', '13900010001', 1, 3, 100, 1),
(201, '24CS01002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李文博', NULL, '24CS01002@stu.usts-tp.edu.cn', '13900010002', 1, 3, 100, 1),
(202, '24CS01003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张明轩', NULL, '24CS01003@stu.usts-tp.edu.cn', '13900010003', 1, 3, 100, 1),
(203, '24CS01004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘思源', NULL, '24CS01004@stu.usts-tp.edu.cn', '13900010004', 1, 3, 100, 1),
(204, '24CS01005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈宇航', NULL, '24CS01005@stu.usts-tp.edu.cn', '13900010005', 1, 3, 100, 1),
(205, '24CS01006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨子涵', NULL, '24CS01006@stu.usts-tp.edu.cn', '13900010006', 2, 3, 100, 1),
(206, '24CS01007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵子昂', NULL, '24CS01007@stu.usts-tp.edu.cn', '13900010007', 1, 3, 100, 1),
(207, '24CS01008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄欣然', NULL, '24CS01008@stu.usts-tp.edu.cn', '13900010008', 2, 3, 100, 1),
(208, '24CS01009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周浩然', NULL, '24CS01009@stu.usts-tp.edu.cn', '13900010009', 1, 3, 100, 1),
(209, '24CS01010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴若曦', NULL, '24CS01010@stu.usts-tp.edu.cn', '13900010010', 2, 3, 100, 1),
(210, '24CS01011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐俊杰', NULL, '24CS01011@stu.usts-tp.edu.cn', '13900010011', 1, 3, 100, 1),
(211, '24CS01012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙嘉怡', NULL, '24CS01012@stu.usts-tp.edu.cn', '13900010012', 2, 3, 100, 1),
(212, '24CS01013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡子轩', NULL, '24CS01013@stu.usts-tp.edu.cn', '13900010013', 1, 3, 100, 1),
(213, '24CS01014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱文博', NULL, '24CS01014@stu.usts-tp.edu.cn', '13900010014', 1, 3, 100, 1),
(214, '24CS01015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高明亮', NULL, '24CS01015@stu.usts-tp.edu.cn', '13900010015', 1, 3, 100, 1),
(215, '24CS01016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林思远', NULL, '24CS01016@stu.usts-tp.edu.cn', '13900010016', 1, 3, 100, 1),
(216, '24CS01017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何嘉欣', NULL, '24CS01017@stu.usts-tp.edu.cn', '13900010017', 2, 3, 100, 1),
(217, '24CS01018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭子睿', NULL, '24CS01018@stu.usts-tp.edu.cn', '13900010018', 1, 3, 100, 1),
(218, '24CS01019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马梓涵', NULL, '24CS01019@stu.usts-tp.edu.cn', '13900010019', 2, 3, 100, 1),
(219, '24CS01020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗逸飞', NULL, '24CS01020@stu.usts-tp.edu.cn', '13900010020', 1, 3, 100, 1),
(220, '24CS01021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '梁佳欣', NULL, '24CS01021@stu.usts-tp.edu.cn', '13900010021', 2, 3, 100, 1),
(221, '24CS01022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '宋晨曦', NULL, '24CS01022@stu.usts-tp.edu.cn', '13900010022', 1, 3, 100, 1),
(222, '24CS01023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郑宇泽', NULL, '24CS01023@stu.usts-tp.edu.cn', '13900010023', 1, 3, 100, 1),
(223, '24CS01024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '谢梓阳', NULL, '24CS01024@stu.usts-tp.edu.cn', '13900010024', 1, 3, 100, 1),
(224, '24CS01025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '韩嘉豪', NULL, '24CS01025@stu.usts-tp.edu.cn', '13900010025', 1, 3, 100, 1),
(225, '24CS01026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '唐子杰', NULL, '24CS01026@stu.usts-tp.edu.cn', '13900010026', 1, 3, 100, 1),
(226, '24CS01027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '冯梓萌', NULL, '24CS01027@stu.usts-tp.edu.cn', '13900010027', 2, 3, 100, 1),
(227, '24CS01028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '于浩宇', NULL, '24CS01028@stu.usts-tp.edu.cn', '13900010028', 1, 3, 100, 1),
(228, '24CS01029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '董思雨', NULL, '24CS01029@stu.usts-tp.edu.cn', '13900010029', 2, 3, 100, 1),
(229, '24CS01030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '萧若彤', NULL, '24CS01030@stu.usts-tp.edu.cn', '13900010030', 2, 3, 100, 1),
(230, '24CS01031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '程俊宇', NULL, '24CS01031@stu.usts-tp.edu.cn', '13900010031', 1, 3, 100, 1),
(231, '24CS01032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曹雅雯', NULL, '24CS01032@stu.usts-tp.edu.cn', '13900010032', 2, 3, 100, 1),
(232, '24CS01033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '袁梓琪', NULL, '24CS01033@stu.usts-tp.edu.cn', '13900010033', 2, 3, 100, 1),
(233, '24CS01034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '邓思齐', NULL, '24CS01034@stu.usts-tp.edu.cn', '13900010034', 1, 3, 100, 1),
(234, '24CS01035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '许子墨', NULL, '24CS01035@stu.usts-tp.edu.cn', '13900010035', 1, 3, 100, 1);

-- ──── 班 101 计算机2402（35 人，男 24 女 11）ID 235-269 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(235, '24CS02001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '傅宇辰', NULL, '24CS02001@stu.usts-tp.edu.cn', '13900010036', 1, 3, 101, 1),
(236, '24CS02002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '沈嘉宁', NULL, '24CS02002@stu.usts-tp.edu.cn', '13900010037', 1, 3, 101, 1),
(237, '24CS02003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曾若兰', NULL, '24CS02003@stu.usts-tp.edu.cn', '13900010038', 2, 3, 101, 1),
(238, '24CS02004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '彭志洋', NULL, '24CS02004@stu.usts-tp.edu.cn', '13900010039', 1, 3, 101, 1),
(239, '24CS02005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吕文瀚', NULL, '24CS02005@stu.usts-tp.edu.cn', '13900010040', 1, 3, 101, 1),
(240, '24CS02006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '苏俊熙', NULL, '24CS02006@stu.usts-tp.edu.cn', '13900010041', 1, 3, 101, 1),
(241, '24CS02007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '卢嘉怡', NULL, '24CS02007@stu.usts-tp.edu.cn', '13900010042', 2, 3, 101, 1),
(242, '24CS02008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蒋子韬', NULL, '24CS02008@stu.usts-tp.edu.cn', '13900010043', 1, 3, 101, 1),
(243, '24CS02009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蔡梓晨', NULL, '24CS02009@stu.usts-tp.edu.cn', '13900010044', 2, 3, 101, 1),
(244, '24CS02010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '贾逸辰', NULL, '24CS02010@stu.usts-tp.edu.cn', '13900010045', 1, 3, 101, 1),
(245, '24CS02011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '丁佳琪', NULL, '24CS02011@stu.usts-tp.edu.cn', '13900010046', 2, 3, 101, 1),
(246, '24CS02012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '魏宇阳', NULL, '24CS02012@stu.usts-tp.edu.cn', '13900010047', 1, 3, 101, 1),
(247, '24CS02013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '薛欣怡', NULL, '24CS02013@stu.usts-tp.edu.cn', '13900010048', 2, 3, 101, 1),
(248, '24CS02014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '叶浩嘉', NULL, '24CS02014@stu.usts-tp.edu.cn', '13900010049', 1, 3, 101, 1),
(249, '24CS02015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '阎若云', NULL, '24CS02015@stu.usts-tp.edu.cn', '13900010050', 2, 3, 101, 1),
(250, '24CS02016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王思博', NULL, '24CS02016@stu.usts-tp.edu.cn', '13900010051', 1, 3, 101, 1),
(251, '24CS02017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李文涛', NULL, '24CS02017@stu.usts-tp.edu.cn', '13900010052', 1, 3, 101, 1),
(252, '24CS02018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张明杰', NULL, '24CS02018@stu.usts-tp.edu.cn', '13900010053', 1, 3, 101, 1),
(253, '24CS02019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘子涵', NULL, '24CS02019@stu.usts-tp.edu.cn', '13900010054', 2, 3, 101, 1),
(254, '24CS02020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈宇飞', NULL, '24CS02020@stu.usts-tp.edu.cn', '13900010055', 1, 3, 101, 1),
(255, '24CS02021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨佳乐', NULL, '24CS02021@stu.usts-tp.edu.cn', '13900010056', 2, 3, 101, 1),
(256, '24CS02022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵欣彤', NULL, '24CS02022@stu.usts-tp.edu.cn', '13900010057', 2, 3, 101, 1),
(257, '24CS02023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄浩波', NULL, '24CS02023@stu.usts-tp.edu.cn', '13900010058', 1, 3, 101, 1),
(258, '24CS02024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周若寒', NULL, '24CS02024@stu.usts-tp.edu.cn', '13900010059', 2, 3, 101, 1),
(259, '24CS02025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴志诚', NULL, '24CS02025@stu.usts-tp.edu.cn', '13900010060', 1, 3, 101, 1),
(260, '24CS02026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐俊豪', NULL, '24CS02026@stu.usts-tp.edu.cn', '13900010061', 1, 3, 101, 1),
(261, '24CS02027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙嘉睿', NULL, '24CS02027@stu.usts-tp.edu.cn', '13900010062', 1, 3, 101, 1),
(262, '24CS02028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡子辰', NULL, '24CS02028@stu.usts-tp.edu.cn', '13900010063', 1, 3, 101, 1),
(263, '24CS02029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱梓豪', NULL, '24CS02029@stu.usts-tp.edu.cn', '13900010064', 1, 3, 101, 1),
(264, '24CS02030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高逸宇', NULL, '24CS02030@stu.usts-tp.edu.cn', '13900010065', 1, 3, 101, 1),
(265, '24CS02031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林佳乐', NULL, '24CS02031@stu.usts-tp.edu.cn', '13900010066', 2, 3, 101, 1),
(266, '24CS02032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何宇宁', NULL, '24CS02032@stu.usts-tp.edu.cn', '13900010067', 1, 3, 101, 1),
(267, '24CS02033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭欣然', NULL, '24CS02033@stu.usts-tp.edu.cn', '13900010068', 2, 3, 101, 1),
(268, '24CS02034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马浩宇', NULL, '24CS02034@stu.usts-tp.edu.cn', '13900010069', 1, 3, 101, 1),
(269, '24CS02035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗若兰', NULL, '24CS02035@stu.usts-tp.edu.cn', '13900010070', 2, 3, 101, 1);

-- ──── 班 102 通信工程2401（35 人，男 24 女 11）ID 270-304；占位 username 24SE01xxx ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(270, '24SE01001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '梁思宇', NULL, '24SE01001@stu.usts-tp.edu.cn', '13900010071', 1, 3, 102, 1),
(271, '24SE01002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '宋文豪', NULL, '24SE01002@stu.usts-tp.edu.cn', '13900010072', 1, 3, 102, 1),
(272, '24SE01003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郑明涛', NULL, '24SE01003@stu.usts-tp.edu.cn', '13900010073', 1, 3, 102, 1),
(273, '24SE01004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '谢子远', NULL, '24SE01004@stu.usts-tp.edu.cn', '13900010074', 1, 3, 102, 1),
(274, '24SE01005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '韩宇腾', NULL, '24SE01005@stu.usts-tp.edu.cn', '13900010075', 1, 3, 102, 1),
(275, '24SE01006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '唐佳琳', NULL, '24SE01006@stu.usts-tp.edu.cn', '13900010076', 2, 3, 102, 1),
(276, '24SE01007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '冯欣琪', NULL, '24SE01007@stu.usts-tp.edu.cn', '13900010077', 2, 3, 102, 1),
(277, '24SE01008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '于浩然', NULL, '24SE01008@stu.usts-tp.edu.cn', '13900010078', 1, 3, 102, 1),
(278, '24SE01009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '董若梅', NULL, '24SE01009@stu.usts-tp.edu.cn', '13900010079', 2, 3, 102, 1),
(279, '24SE01010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '萧志杰', NULL, '24SE01010@stu.usts-tp.edu.cn', '13900010080', 1, 3, 102, 1),
(280, '24SE01011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '程俊辉', NULL, '24SE01011@stu.usts-tp.edu.cn', '13900010081', 1, 3, 102, 1),
(281, '24SE01012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曹嘉伟', NULL, '24SE01012@stu.usts-tp.edu.cn', '13900010082', 1, 3, 102, 1),
(282, '24SE01013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '袁子博', NULL, '24SE01013@stu.usts-tp.edu.cn', '13900010083', 1, 3, 102, 1),
(283, '24SE01014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '邓梓杰', NULL, '24SE01014@stu.usts-tp.edu.cn', '13900010084', 1, 3, 102, 1),
(284, '24SE01015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '许逸涛', NULL, '24SE01015@stu.usts-tp.edu.cn', '13900010085', 1, 3, 102, 1),
(285, '24SE01016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '傅佳鑫', NULL, '24SE01016@stu.usts-tp.edu.cn', '13900010086', 2, 3, 102, 1),
(286, '24SE01017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '沈宇恒', NULL, '24SE01017@stu.usts-tp.edu.cn', '13900010087', 1, 3, 102, 1),
(287, '24SE01018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曾欣怡', NULL, '24SE01018@stu.usts-tp.edu.cn', '13900010088', 2, 3, 102, 1),
(288, '24SE01019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '彭浩瀚', NULL, '24SE01019@stu.usts-tp.edu.cn', '13900010089', 1, 3, 102, 1),
(289, '24SE01020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吕若雯', NULL, '24SE01020@stu.usts-tp.edu.cn', '13900010090', 2, 3, 102, 1),
(290, '24SE01021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '苏思浩', NULL, '24SE01021@stu.usts-tp.edu.cn', '13900010091', 1, 3, 102, 1),
(291, '24SE01022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '卢文康', NULL, '24SE01022@stu.usts-tp.edu.cn', '13900010092', 1, 3, 102, 1),
(292, '24SE01023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蒋明华', NULL, '24SE01023@stu.usts-tp.edu.cn', '13900010093', 1, 3, 102, 1),
(293, '24SE01024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蔡子谦', NULL, '24SE01024@stu.usts-tp.edu.cn', '13900010094', 1, 3, 102, 1),
(294, '24SE01025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '贾宇航', NULL, '24SE01025@stu.usts-tp.edu.cn', '13900010095', 1, 3, 102, 1),
(295, '24SE01026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '丁佳婷', NULL, '24SE01026@stu.usts-tp.edu.cn', '13900010096', 2, 3, 102, 1),
(296, '24SE01027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '魏欣冉', NULL, '24SE01027@stu.usts-tp.edu.cn', '13900010097', 2, 3, 102, 1),
(297, '24SE01028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '薛浩泽', NULL, '24SE01028@stu.usts-tp.edu.cn', '13900010098', 1, 3, 102, 1),
(298, '24SE01029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '叶若曦', NULL, '24SE01029@stu.usts-tp.edu.cn', '13900010099', 2, 3, 102, 1),
(299, '24SE01030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '阎志成', NULL, '24SE01030@stu.usts-tp.edu.cn', '13900010100', 1, 3, 102, 1),
(300, '24SE01031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王俊豪', NULL, '24SE01031@stu.usts-tp.edu.cn', '13900010101', 1, 3, 102, 1),
(301, '24SE01032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李文清', NULL, '24SE01032@stu.usts-tp.edu.cn', '13900010102', 1, 3, 102, 1),
(302, '24SE01033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张明远', NULL, '24SE01033@stu.usts-tp.edu.cn', '13900010103', 1, 3, 102, 1),
(303, '24SE01034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘思凡', NULL, '24SE01034@stu.usts-tp.edu.cn', '13900010104', 2, 3, 102, 1),
(304, '24SE01035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈宇凡', NULL, '24SE01035@stu.usts-tp.edu.cn', '13900010105', 1, 3, 102, 1);

-- ──── 班 103 通信工程2402（35 人，男 24 女 11）ID 305-339；占位 username 24SE02xxx ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(305, '24SE02001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨子涵', NULL, '24SE02001@stu.usts-tp.edu.cn', '13900010106', 2, 3, 103, 1),
(306, '24SE02002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵嘉伟', NULL, '24SE02002@stu.usts-tp.edu.cn', '13900010107', 1, 3, 103, 1),
(307, '24SE02003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄宇浩', NULL, '24SE02003@stu.usts-tp.edu.cn', '13900010108', 1, 3, 103, 1),
(308, '24SE02004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周思琪', NULL, '24SE02004@stu.usts-tp.edu.cn', '13900010109', 2, 3, 103, 1),
(309, '24SE02005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴若晨', NULL, '24SE02005@stu.usts-tp.edu.cn', '13900010110', 2, 3, 103, 1),
(310, '24SE02006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐子健', NULL, '24SE02006@stu.usts-tp.edu.cn', '13900010111', 1, 3, 103, 1),
(311, '24SE02007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙嘉伦', NULL, '24SE02007@stu.usts-tp.edu.cn', '13900010112', 1, 3, 103, 1),
(312, '24SE02008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡文森', NULL, '24SE02008@stu.usts-tp.edu.cn', '13900010113', 1, 3, 103, 1),
(313, '24SE02009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱明远', NULL, '24SE02009@stu.usts-tp.edu.cn', '13900010114', 1, 3, 103, 1),
(314, '24SE02010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高梓骞', NULL, '24SE02010@stu.usts-tp.edu.cn', '13900010115', 1, 3, 103, 1),
(315, '24SE02011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林俊宇', NULL, '24SE02011@stu.usts-tp.edu.cn', '13900010116', 1, 3, 103, 1),
(316, '24SE02012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何嘉怡', NULL, '24SE02012@stu.usts-tp.edu.cn', '13900010117', 2, 3, 103, 1),
(317, '24SE02013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭子博', NULL, '24SE02013@stu.usts-tp.edu.cn', '13900010118', 1, 3, 103, 1),
(318, '24SE02014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马欣悦', NULL, '24SE02014@stu.usts-tp.edu.cn', '13900010119', 2, 3, 103, 1),
(319, '24SE02015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗逸辰', NULL, '24SE02015@stu.usts-tp.edu.cn', '13900010120', 1, 3, 103, 1),
(320, '24SE02016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '梁佳沁', NULL, '24SE02016@stu.usts-tp.edu.cn', '13900010121', 2, 3, 103, 1),
(321, '24SE02017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '宋宇桐', NULL, '24SE02017@stu.usts-tp.edu.cn', '13900010122', 1, 3, 103, 1),
(322, '24SE02018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郑梓阳', NULL, '24SE02018@stu.usts-tp.edu.cn', '13900010123', 1, 3, 103, 1),
(323, '24SE02019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '谢俊翔', NULL, '24SE02019@stu.usts-tp.edu.cn', '13900010124', 1, 3, 103, 1),
(324, '24SE02020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '韩浩然', NULL, '24SE02020@stu.usts-tp.edu.cn', '13900010125', 1, 3, 103, 1),
(325, '24SE02021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '唐子铭', NULL, '24SE02021@stu.usts-tp.edu.cn', '13900010126', 1, 3, 103, 1),
(326, '24SE02022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '冯欣琪', NULL, '24SE02022@stu.usts-tp.edu.cn', '13900010127', 2, 3, 103, 1),
(327, '24SE02023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '于浩翔', NULL, '24SE02023@stu.usts-tp.edu.cn', '13900010128', 1, 3, 103, 1),
(328, '24SE02024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '董若兰', NULL, '24SE02024@stu.usts-tp.edu.cn', '13900010129', 2, 3, 103, 1),
(329, '24SE02025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '萧俊豪', NULL, '24SE02025@stu.usts-tp.edu.cn', '13900010130', 1, 3, 103, 1),
(330, '24SE02026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '程梓萱', NULL, '24SE02026@stu.usts-tp.edu.cn', '13900010131', 2, 3, 103, 1),
(331, '24SE02027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曹佳颖', NULL, '24SE02027@stu.usts-tp.edu.cn', '13900010132', 2, 3, 103, 1),
(332, '24SE02028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '袁子瑞', NULL, '24SE02028@stu.usts-tp.edu.cn', '13900010133', 1, 3, 103, 1),
(333, '24SE02029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '邓宇恒', NULL, '24SE02029@stu.usts-tp.edu.cn', '13900010134', 1, 3, 103, 1),
(334, '24SE02030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '许梓涵', NULL, '24SE02030@stu.usts-tp.edu.cn', '13900010135', 2, 3, 103, 1),
(335, '24SE02031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '傅文博', NULL, '24SE02031@stu.usts-tp.edu.cn', '13900010136', 1, 3, 103, 1),
(336, '24SE02032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '沈嘉颖', NULL, '24SE02032@stu.usts-tp.edu.cn', '13900010137', 2, 3, 103, 1),
(337, '24SE02033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曾子睿', NULL, '24SE02033@stu.usts-tp.edu.cn', '13900010138', 1, 3, 103, 1),
(338, '24SE02034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '彭浩泽', NULL, '24SE02034@stu.usts-tp.edu.cn', '13900010139', 1, 3, 103, 1),
(339, '24SE02035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吕若彤', NULL, '24SE02035@stu.usts-tp.edu.cn', '13900010140', 2, 3, 103, 1);

-- ──── 班 104 电子信息2401（35 人，男 24 女 11）ID 340-374 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(340, '24EI01001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '苏志远', NULL, '24EI01001@stu.usts-tp.edu.cn', '13900010141', 1, 3, 104, 1),
(341, '24EI01002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '卢俊辉', NULL, '24EI01002@stu.usts-tp.edu.cn', '13900010142', 1, 3, 104, 1),
(342, '24EI01003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蒋嘉伟', NULL, '24EI01003@stu.usts-tp.edu.cn', '13900010143', 1, 3, 104, 1),
(343, '24EI01004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蔡子博', NULL, '24EI01004@stu.usts-tp.edu.cn', '13900010144', 1, 3, 104, 1),
(344, '24EI01005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '贾梓杰', NULL, '24EI01005@stu.usts-tp.edu.cn', '13900010145', 1, 3, 104, 1),
(345, '24EI01006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '丁逸飞', NULL, '24EI01006@stu.usts-tp.edu.cn', '13900010146', 1, 3, 104, 1),
(346, '24EI01007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '魏佳鑫', NULL, '24EI01007@stu.usts-tp.edu.cn', '13900010147', 2, 3, 104, 1),
(347, '24EI01008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '薛宇恒', NULL, '24EI01008@stu.usts-tp.edu.cn', '13900010148', 1, 3, 104, 1),
(348, '24EI01009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '叶欣怡', NULL, '24EI01009@stu.usts-tp.edu.cn', '13900010149', 2, 3, 104, 1),
(349, '24EI01010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '阎浩瀚', NULL, '24EI01010@stu.usts-tp.edu.cn', '13900010150', 1, 3, 104, 1),
(350, '24EI01011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王若雯', NULL, '24EI01011@stu.usts-tp.edu.cn', '13900010151', 2, 3, 104, 1),
(351, '24EI01012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李思浩', NULL, '24EI01012@stu.usts-tp.edu.cn', '13900010152', 1, 3, 104, 1),
(352, '24EI01013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张文康', NULL, '24EI01013@stu.usts-tp.edu.cn', '13900010153', 1, 3, 104, 1),
(353, '24EI01014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘明华', NULL, '24EI01014@stu.usts-tp.edu.cn', '13900010154', 1, 3, 104, 1),
(354, '24EI01015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈子谦', NULL, '24EI01015@stu.usts-tp.edu.cn', '13900010155', 1, 3, 104, 1),
(355, '24EI01016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨宇航', NULL, '24EI01016@stu.usts-tp.edu.cn', '13900010156', 1, 3, 104, 1),
(356, '24EI01017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵佳婷', NULL, '24EI01017@stu.usts-tp.edu.cn', '13900010157', 2, 3, 104, 1),
(357, '24EI01018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄欣冉', NULL, '24EI01018@stu.usts-tp.edu.cn', '13900010158', 2, 3, 104, 1),
(358, '24EI01019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周浩泽', NULL, '24EI01019@stu.usts-tp.edu.cn', '13900010159', 1, 3, 104, 1),
(359, '24EI01020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴若曦', NULL, '24EI01020@stu.usts-tp.edu.cn', '13900010160', 2, 3, 104, 1),
(360, '24EI01021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐志成', NULL, '24EI01021@stu.usts-tp.edu.cn', '13900010161', 1, 3, 104, 1),
(361, '24EI01022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙俊豪', NULL, '24EI01022@stu.usts-tp.edu.cn', '13900010162', 1, 3, 104, 1),
(362, '24EI01023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡文清', NULL, '24EI01023@stu.usts-tp.edu.cn', '13900010163', 1, 3, 104, 1),
(363, '24EI01024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱明亮', NULL, '24EI01024@stu.usts-tp.edu.cn', '13900010164', 1, 3, 104, 1),
(364, '24EI01025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高梓骞', NULL, '24EI01025@stu.usts-tp.edu.cn', '13900010165', 1, 3, 104, 1),
(365, '24EI01026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林思凡', NULL, '24EI01026@stu.usts-tp.edu.cn', '13900010166', 2, 3, 104, 1),
(366, '24EI01027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何宇凡', NULL, '24EI01027@stu.usts-tp.edu.cn', '13900010167', 1, 3, 104, 1),
(367, '24EI01028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭子涵', NULL, '24EI01028@stu.usts-tp.edu.cn', '13900010168', 2, 3, 104, 1),
(368, '24EI01029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马俊鹏', NULL, '24EI01029@stu.usts-tp.edu.cn', '13900010169', 1, 3, 104, 1),
(369, '24EI01030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗梓琪', NULL, '24EI01030@stu.usts-tp.edu.cn', '13900010170', 2, 3, 104, 1),
(370, '24EI01031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '梁佳怡', NULL, '24EI01031@stu.usts-tp.edu.cn', '13900010171', 2, 3, 104, 1),
(371, '24EI01032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '宋宇辉', NULL, '24EI01032@stu.usts-tp.edu.cn', '13900010172', 1, 3, 104, 1),
(372, '24EI01033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郑梓楷', NULL, '24EI01033@stu.usts-tp.edu.cn', '13900010173', 1, 3, 104, 1),
(373, '24EI01034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '谢俊宇', NULL, '24EI01034@stu.usts-tp.edu.cn', '13900010174', 1, 3, 104, 1),
(374, '24EI01035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '韩浩天', NULL, '24EI01035@stu.usts-tp.edu.cn', '13900010175', 1, 3, 104, 1);

-- ──── 班 105 电子信息2402（35 人，男 24 女 11）ID 375-409 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(375, '24EI02001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '唐子杰', NULL, '24EI02001@stu.usts-tp.edu.cn', '13900010176', 1, 3, 105, 1),
(376, '24EI02002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '冯梓宁', NULL, '24EI02002@stu.usts-tp.edu.cn', '13900010177', 2, 3, 105, 1),
(377, '24EI02003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '于浩然', NULL, '24EI02003@stu.usts-tp.edu.cn', '13900010178', 1, 3, 105, 1),
(378, '24EI02004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '董若云', NULL, '24EI02004@stu.usts-tp.edu.cn', '13900010179', 2, 3, 105, 1),
(379, '24EI02005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '萧志洋', NULL, '24EI02005@stu.usts-tp.edu.cn', '13900010180', 1, 3, 105, 1),
(380, '24EI02006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '程文瀚', NULL, '24EI02006@stu.usts-tp.edu.cn', '13900010181', 1, 3, 105, 1),
(381, '24EI02007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曹俊熙', NULL, '24EI02007@stu.usts-tp.edu.cn', '13900010182', 1, 3, 105, 1),
(382, '24EI02008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '袁嘉怡', NULL, '24EI02008@stu.usts-tp.edu.cn', '13900010183', 2, 3, 105, 1),
(383, '24EI02009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '邓子韬', NULL, '24EI02009@stu.usts-tp.edu.cn', '13900010184', 1, 3, 105, 1),
(384, '24EI02010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '许梓晨', NULL, '24EI02010@stu.usts-tp.edu.cn', '13900010185', 1, 3, 105, 1),
(385, '24EI02011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '傅逸辰', NULL, '24EI02011@stu.usts-tp.edu.cn', '13900010186', 1, 3, 105, 1),
(386, '24EI02012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '沈佳琪', NULL, '24EI02012@stu.usts-tp.edu.cn', '13900010187', 2, 3, 105, 1),
(387, '24EI02013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曾宇阳', NULL, '24EI02013@stu.usts-tp.edu.cn', '13900010188', 1, 3, 105, 1),
(388, '24EI02014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '彭欣怡', NULL, '24EI02014@stu.usts-tp.edu.cn', '13900010189', 2, 3, 105, 1),
(389, '24EI02015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吕浩嘉', NULL, '24EI02015@stu.usts-tp.edu.cn', '13900010190', 1, 3, 105, 1),
(390, '24EI02016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '苏若云', NULL, '24EI02016@stu.usts-tp.edu.cn', '13900010191', 2, 3, 105, 1),
(391, '24EI02017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '卢思博', NULL, '24EI02017@stu.usts-tp.edu.cn', '13900010192', 1, 3, 105, 1),
(392, '24EI02018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蒋文涛', NULL, '24EI02018@stu.usts-tp.edu.cn', '13900010193', 1, 3, 105, 1),
(393, '24EI02019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蔡明杰', NULL, '24EI02019@stu.usts-tp.edu.cn', '13900010194', 1, 3, 105, 1),
(394, '24EI02020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '贾子涵', NULL, '24EI02020@stu.usts-tp.edu.cn', '13900010195', 2, 3, 105, 1),
(395, '24EI02021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '丁宇飞', NULL, '24EI02021@stu.usts-tp.edu.cn', '13900010196', 1, 3, 105, 1),
(396, '24EI02022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '魏佳乐', NULL, '24EI02022@stu.usts-tp.edu.cn', '13900010197', 2, 3, 105, 1),
(397, '24EI02023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '薛欣彤', NULL, '24EI02023@stu.usts-tp.edu.cn', '13900010198', 2, 3, 105, 1),
(398, '24EI02024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '叶浩波', NULL, '24EI02024@stu.usts-tp.edu.cn', '13900010199', 1, 3, 105, 1),
(399, '24EI02025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '阎若寒', NULL, '24EI02025@stu.usts-tp.edu.cn', '13900010200', 2, 3, 105, 1),
(400, '24EI02026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王志诚', NULL, '24EI02026@stu.usts-tp.edu.cn', '13900010201', 1, 3, 105, 1),
(401, '24EI02027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李俊豪', NULL, '24EI02027@stu.usts-tp.edu.cn', '13900010202', 1, 3, 105, 1),
(402, '24EI02028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张嘉睿', NULL, '24EI02028@stu.usts-tp.edu.cn', '13900010203', 1, 3, 105, 1),
(403, '24EI02029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘子辰', NULL, '24EI02029@stu.usts-tp.edu.cn', '13900010204', 1, 3, 105, 1),
(404, '24EI02030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈梓豪', NULL, '24EI02030@stu.usts-tp.edu.cn', '13900010205', 1, 3, 105, 1),
(405, '24EI02031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨逸宇', NULL, '24EI02031@stu.usts-tp.edu.cn', '13900010206', 1, 3, 105, 1),
(406, '24EI02032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵佳乐', NULL, '24EI02032@stu.usts-tp.edu.cn', '13900010207', 2, 3, 105, 1),
(407, '24EI02033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄宇宁', NULL, '24EI02033@stu.usts-tp.edu.cn', '13900010208', 1, 3, 105, 1),
(408, '24EI02034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周欣然', NULL, '24EI02034@stu.usts-tp.edu.cn', '13900010209', 2, 3, 105, 1),
(409, '24EI02035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴浩宇', NULL, '24EI02035@stu.usts-tp.edu.cn', '13900010210', 1, 3, 105, 1);

-- ──── 班 106 机械2401（35 人，男 28 女 7）ID 410-444 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(410, '24ME01001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐若兰', NULL, '24ME01001@stu.usts-tp.edu.cn', '13900010211', 2, 3, 106, 1),
(411, '24ME01002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙思宇', NULL, '24ME01002@stu.usts-tp.edu.cn', '13900010212', 1, 3, 106, 1),
(412, '24ME01003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡文豪', NULL, '24ME01003@stu.usts-tp.edu.cn', '13900010213', 1, 3, 106, 1),
(413, '24ME01004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱明涛', NULL, '24ME01004@stu.usts-tp.edu.cn', '13900010214', 1, 3, 106, 1),
(414, '24ME01005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高子远', NULL, '24ME01005@stu.usts-tp.edu.cn', '13900010215', 1, 3, 106, 1),
(415, '24ME01006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林宇腾', NULL, '24ME01006@stu.usts-tp.edu.cn', '13900010216', 1, 3, 106, 1),
(416, '24ME01007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何佳琳', NULL, '24ME01007@stu.usts-tp.edu.cn', '13900010217', 2, 3, 106, 1),
(417, '24ME01008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭梓豪', NULL, '24ME01008@stu.usts-tp.edu.cn', '13900010218', 1, 3, 106, 1),
(418, '24ME01009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马浩然', NULL, '24ME01009@stu.usts-tp.edu.cn', '13900010219', 1, 3, 106, 1),
(419, '24ME01010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗若梅', NULL, '24ME01010@stu.usts-tp.edu.cn', '13900010220', 2, 3, 106, 1),
(420, '24ME01011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '梁志杰', NULL, '24ME01011@stu.usts-tp.edu.cn', '13900010221', 1, 3, 106, 1),
(421, '24ME01012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '宋俊辉', NULL, '24ME01012@stu.usts-tp.edu.cn', '13900010222', 1, 3, 106, 1),
(422, '24ME01013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郑嘉伟', NULL, '24ME01013@stu.usts-tp.edu.cn', '13900010223', 1, 3, 106, 1),
(423, '24ME01014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '谢子博', NULL, '24ME01014@stu.usts-tp.edu.cn', '13900010224', 1, 3, 106, 1),
(424, '24ME01015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '韩梓杰', NULL, '24ME01015@stu.usts-tp.edu.cn', '13900010225', 1, 3, 106, 1),
(425, '24ME01016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '唐逸涛', NULL, '24ME01016@stu.usts-tp.edu.cn', '13900010226', 1, 3, 106, 1),
(426, '24ME01017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '冯佳鑫', NULL, '24ME01017@stu.usts-tp.edu.cn', '13900010227', 2, 3, 106, 1),
(427, '24ME01018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '于宇恒', NULL, '24ME01018@stu.usts-tp.edu.cn', '13900010228', 1, 3, 106, 1),
(428, '24ME01019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '董欣怡', NULL, '24ME01019@stu.usts-tp.edu.cn', '13900010229', 2, 3, 106, 1),
(429, '24ME01020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '萧浩瀚', NULL, '24ME01020@stu.usts-tp.edu.cn', '13900010230', 1, 3, 106, 1),
(430, '24ME01021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '程梓骞', NULL, '24ME01021@stu.usts-tp.edu.cn', '13900010231', 1, 3, 106, 1),
(431, '24ME01022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曹思浩', NULL, '24ME01022@stu.usts-tp.edu.cn', '13900010232', 1, 3, 106, 1),
(432, '24ME01023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '袁文康', NULL, '24ME01023@stu.usts-tp.edu.cn', '13900010233', 1, 3, 106, 1),
(433, '24ME01024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '邓明华', NULL, '24ME01024@stu.usts-tp.edu.cn', '13900010234', 1, 3, 106, 1),
(434, '24ME01025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '许子谦', NULL, '24ME01025@stu.usts-tp.edu.cn', '13900010235', 1, 3, 106, 1),
(435, '24ME01026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '傅宇航', NULL, '24ME01026@stu.usts-tp.edu.cn', '13900010236', 1, 3, 106, 1),
(436, '24ME01027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '沈佳婷', NULL, '24ME01027@stu.usts-tp.edu.cn', '13900010237', 2, 3, 106, 1),
(437, '24ME01028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曾欣冉', NULL, '24ME01028@stu.usts-tp.edu.cn', '13900010238', 2, 3, 106, 1),
(438, '24ME01029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '彭浩泽', NULL, '24ME01029@stu.usts-tp.edu.cn', '13900010239', 1, 3, 106, 1),
(439, '24ME01030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吕志刚', NULL, '24ME01030@stu.usts-tp.edu.cn', '13900010240', 1, 3, 106, 1),
(440, '24ME01031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '苏志成', NULL, '24ME01031@stu.usts-tp.edu.cn', '13900010241', 1, 3, 106, 1),
(441, '24ME01032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '卢俊豪', NULL, '24ME01032@stu.usts-tp.edu.cn', '13900010242', 1, 3, 106, 1),
(442, '24ME01033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蒋嘉睿', NULL, '24ME01033@stu.usts-tp.edu.cn', '13900010243', 1, 3, 106, 1),
(443, '24ME01034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蔡子辰', NULL, '24ME01034@stu.usts-tp.edu.cn', '13900010244', 1, 3, 106, 1),
(444, '24ME01035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '贾梓豪', NULL, '24ME01035@stu.usts-tp.edu.cn', '13900010245', 1, 3, 106, 1);

-- ──── 班 107 机械2402（35 人，男 28 女 7）ID 445-479 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(445, '24ME02001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '丁宇翔', NULL, '24ME02001@stu.usts-tp.edu.cn', '13900010246', 1, 3, 107, 1),
(446, '24ME02002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '魏志远', NULL, '24ME02002@stu.usts-tp.edu.cn', '13900010247', 1, 3, 107, 1),
(447, '24ME02003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '薛俊文', NULL, '24ME02003@stu.usts-tp.edu.cn', '13900010248', 1, 3, 107, 1),
(448, '24ME02004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '叶嘉伟', NULL, '24ME02004@stu.usts-tp.edu.cn', '13900010249', 1, 3, 107, 1),
(449, '24ME02005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '阎子健', NULL, '24ME02005@stu.usts-tp.edu.cn', '13900010250', 1, 3, 107, 1),
(450, '24ME02006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王明杰', NULL, '24ME02006@stu.usts-tp.edu.cn', '13900010251', 1, 3, 107, 1),
(451, '24ME02007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李俊鹏', NULL, '24ME02007@stu.usts-tp.edu.cn', '13900010252', 1, 3, 107, 1),
(452, '24ME02008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张梓豪', NULL, '24ME02008@stu.usts-tp.edu.cn', '13900010253', 1, 3, 107, 1),
(453, '24ME02009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘嘉毅', NULL, '24ME02009@stu.usts-tp.edu.cn', '13900010254', 1, 3, 107, 1),
(454, '24ME02010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈宇成', NULL, '24ME02010@stu.usts-tp.edu.cn', '13900010255', 1, 3, 107, 1),
(455, '24ME02011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨佳怡', NULL, '24ME02011@stu.usts-tp.edu.cn', '13900010256', 2, 3, 107, 1),
(456, '24ME02012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵浩波', NULL, '24ME02012@stu.usts-tp.edu.cn', '13900010257', 1, 3, 107, 1),
(457, '24ME02013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄文涛', NULL, '24ME02013@stu.usts-tp.edu.cn', '13900010258', 1, 3, 107, 1),
(458, '24ME02014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周思齐', NULL, '24ME02014@stu.usts-tp.edu.cn', '13900010259', 1, 3, 107, 1),
(459, '24ME02015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴梓涵', NULL, '24ME02015@stu.usts-tp.edu.cn', '13900010260', 2, 3, 107, 1),
(460, '24ME02016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐子睿', NULL, '24ME02016@stu.usts-tp.edu.cn', '13900010261', 1, 3, 107, 1),
(461, '24ME02017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙宇航', NULL, '24ME02017@stu.usts-tp.edu.cn', '13900010262', 1, 3, 107, 1),
(462, '24ME02018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡逸辰', NULL, '24ME02018@stu.usts-tp.edu.cn', '13900010263', 1, 3, 107, 1),
(463, '24ME02019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱佳琪', NULL, '24ME02019@stu.usts-tp.edu.cn', '13900010264', 2, 3, 107, 1),
(464, '24ME02020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高志强', NULL, '24ME02020@stu.usts-tp.edu.cn', '13900010265', 1, 3, 107, 1),
(465, '24ME02021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林浩瀚', NULL, '24ME02021@stu.usts-tp.edu.cn', '13900010266', 1, 3, 107, 1),
(466, '24ME02022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何俊熙', NULL, '24ME02022@stu.usts-tp.edu.cn', '13900010267', 1, 3, 107, 1),
(467, '24ME02023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭欣彤', NULL, '24ME02023@stu.usts-tp.edu.cn', '13900010268', 2, 3, 107, 1),
(468, '24ME02024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马思博', NULL, '24ME02024@stu.usts-tp.edu.cn', '13900010269', 1, 3, 107, 1),
(469, '24ME02025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗子腾', NULL, '24ME02025@stu.usts-tp.edu.cn', '13900010270', 1, 3, 107, 1),
(470, '24ME02026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '梁文康', NULL, '24ME02026@stu.usts-tp.edu.cn', '13900010271', 1, 3, 107, 1),
(471, '24ME02027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '宋佳鑫', NULL, '24ME02027@stu.usts-tp.edu.cn', '13900010272', 2, 3, 107, 1),
(472, '24ME02028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郑志洋', NULL, '24ME02028@stu.usts-tp.edu.cn', '13900010273', 1, 3, 107, 1),
(473, '24ME02029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '谢明华', NULL, '24ME02029@stu.usts-tp.edu.cn', '13900010274', 1, 3, 107, 1),
(474, '24ME02030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '韩子谦', NULL, '24ME02030@stu.usts-tp.edu.cn', '13900010275', 1, 3, 107, 1),
(475, '24ME02031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '唐俊辉', NULL, '24ME02031@stu.usts-tp.edu.cn', '13900010276', 1, 3, 107, 1),
(476, '24ME02032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '冯嘉伟', NULL, '24ME02032@stu.usts-tp.edu.cn', '13900010277', 1, 3, 107, 1),
(477, '24ME02033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '于浩然', NULL, '24ME02033@stu.usts-tp.edu.cn', '13900010278', 1, 3, 107, 1),
(478, '24ME02034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '董若梅', NULL, '24ME02034@stu.usts-tp.edu.cn', '13900010279', 2, 3, 107, 1),
(479, '24ME02035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '萧浩泽', NULL, '24ME02035@stu.usts-tp.edu.cn', '13900010280', 1, 3, 107, 1);

-- ──── 班 108 电气2401（35 人，男 28 女 7）ID 480-514 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(480, '24EE01001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '程梓骞', NULL, '24EE01001@stu.usts-tp.edu.cn', '13900010281', 1, 3, 108, 1),
(481, '24EE01002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曹文渊', NULL, '24EE01002@stu.usts-tp.edu.cn', '13900010282', 1, 3, 108, 1),
(482, '24EE01003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '袁俊豪', NULL, '24EE01003@stu.usts-tp.edu.cn', '13900010283', 1, 3, 108, 1),
(483, '24EE01004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '邓宇航', NULL, '24EE01004@stu.usts-tp.edu.cn', '13900010284', 1, 3, 108, 1),
(484, '24EE01005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '许嘉睿', NULL, '24EE01005@stu.usts-tp.edu.cn', '13900010285', 1, 3, 108, 1),
(485, '24EE01006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '傅子辰', NULL, '24EE01006@stu.usts-tp.edu.cn', '13900010286', 1, 3, 108, 1),
(486, '24EE01007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '沈梓豪', NULL, '24EE01007@stu.usts-tp.edu.cn', '13900010287', 1, 3, 108, 1),
(487, '24EE01008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曾佳鑫', NULL, '24EE01008@stu.usts-tp.edu.cn', '13900010288', 2, 3, 108, 1),
(488, '24EE01009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '彭志成', NULL, '24EE01009@stu.usts-tp.edu.cn', '13900010289', 1, 3, 108, 1),
(489, '24EE01010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吕浩天', NULL, '24EE01010@stu.usts-tp.edu.cn', '13900010290', 1, 3, 108, 1),
(490, '24EE01011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '苏文豪', NULL, '24EE01011@stu.usts-tp.edu.cn', '13900010291', 1, 3, 108, 1),
(491, '24EE01012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '卢俊辉', NULL, '24EE01012@stu.usts-tp.edu.cn', '13900010292', 1, 3, 108, 1),
(492, '24EE01013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蒋嘉伟', NULL, '24EE01013@stu.usts-tp.edu.cn', '13900010293', 1, 3, 108, 1),
(493, '24EE01014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蔡子博', NULL, '24EE01014@stu.usts-tp.edu.cn', '13900010294', 1, 3, 108, 1),
(494, '24EE01015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '贾梓杰', NULL, '24EE01015@stu.usts-tp.edu.cn', '13900010295', 1, 3, 108, 1),
(495, '24EE01016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '丁逸飞', NULL, '24EE01016@stu.usts-tp.edu.cn', '13900010296', 1, 3, 108, 1),
(496, '24EE01017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '魏佳琳', NULL, '24EE01017@stu.usts-tp.edu.cn', '13900010297', 2, 3, 108, 1),
(497, '24EE01018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '薛宇恒', NULL, '24EE01018@stu.usts-tp.edu.cn', '13900010298', 1, 3, 108, 1),
(498, '24EE01019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '叶欣宁', NULL, '24EE01019@stu.usts-tp.edu.cn', '13900010299', 2, 3, 108, 1),
(499, '24EE01020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '阎浩瀚', NULL, '24EE01020@stu.usts-tp.edu.cn', '13900010300', 1, 3, 108, 1),
(500, '24EE01021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王若桐', NULL, '24EE01021@stu.usts-tp.edu.cn', '13900010301', 2, 3, 108, 1),
(501, '24EE01022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李思源', NULL, '24EE01022@stu.usts-tp.edu.cn', '13900010302', 1, 3, 108, 1),
(502, '24EE01023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张文涛', NULL, '24EE01023@stu.usts-tp.edu.cn', '13900010303', 1, 3, 108, 1),
(503, '24EE01024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘明清', NULL, '24EE01024@stu.usts-tp.edu.cn', '13900010304', 1, 3, 108, 1),
(504, '24EE01025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈子铭', NULL, '24EE01025@stu.usts-tp.edu.cn', '13900010305', 1, 3, 108, 1),
(505, '24EE01026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨宇泽', NULL, '24EE01026@stu.usts-tp.edu.cn', '13900010306', 1, 3, 108, 1),
(506, '24EE01027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵嘉颖', NULL, '24EE01027@stu.usts-tp.edu.cn', '13900010307', 2, 3, 108, 1),
(507, '24EE01028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄欣冉', NULL, '24EE01028@stu.usts-tp.edu.cn', '13900010308', 2, 3, 108, 1),
(508, '24EE01029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周浩宇', NULL, '24EE01029@stu.usts-tp.edu.cn', '13900010309', 1, 3, 108, 1),
(509, '24EE01030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴志洋', NULL, '24EE01030@stu.usts-tp.edu.cn', '13900010310', 1, 3, 108, 1),
(510, '24EE01031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐俊豪', NULL, '24EE01031@stu.usts-tp.edu.cn', '13900010311', 1, 3, 108, 1),
(511, '24EE01032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙嘉睿', NULL, '24EE01032@stu.usts-tp.edu.cn', '13900010312', 1, 3, 108, 1),
(512, '24EE01033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡子辰', NULL, '24EE01033@stu.usts-tp.edu.cn', '13900010313', 1, 3, 108, 1),
(513, '24EE01034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱梓豪', NULL, '24EE01034@stu.usts-tp.edu.cn', '13900010314', 1, 3, 108, 1),
(514, '24EE01035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高逸宇', NULL, '24EE01035@stu.usts-tp.edu.cn', '13900010315', 1, 3, 108, 1);

-- ──── 班 109 电气2402（35 人，男 28 女 7）ID 515-549 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(515, '24EE02001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林佳乐', NULL, '24EE02001@stu.usts-tp.edu.cn', '13900010316', 2, 3, 109, 1),
(516, '24EE02002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何宇宁', NULL, '24EE02002@stu.usts-tp.edu.cn', '13900010317', 1, 3, 109, 1),
(517, '24EE02003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭欣然', NULL, '24EE02003@stu.usts-tp.edu.cn', '13900010318', 2, 3, 109, 1),
(518, '24EE02004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马浩宇', NULL, '24EE02004@stu.usts-tp.edu.cn', '13900010319', 1, 3, 109, 1),
(519, '24EE02005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗若兰', NULL, '24EE02005@stu.usts-tp.edu.cn', '13900010320', 2, 3, 109, 1),
(520, '24EE02006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '梁俊豪', NULL, '24EE02006@stu.usts-tp.edu.cn', '13900010321', 1, 3, 109, 1),
(521, '24EE02007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '宋子博', NULL, '24EE02007@stu.usts-tp.edu.cn', '13900010322', 1, 3, 109, 1),
(522, '24EE02008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郑文康', NULL, '24EE02008@stu.usts-tp.edu.cn', '13900010323', 1, 3, 109, 1),
(523, '24EE02009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '谢梓阳', NULL, '24EE02009@stu.usts-tp.edu.cn', '13900010324', 1, 3, 109, 1),
(524, '24EE02010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '韩浩宇', NULL, '24EE02010@stu.usts-tp.edu.cn', '13900010325', 1, 3, 109, 1),
(525, '24EE02011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '唐嘉豪', NULL, '24EE02011@stu.usts-tp.edu.cn', '13900010326', 1, 3, 109, 1),
(526, '24EE02012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '冯子杰', NULL, '24EE02012@stu.usts-tp.edu.cn', '13900010327', 1, 3, 109, 1),
(527, '24EE02013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '于梓萌', NULL, '24EE02013@stu.usts-tp.edu.cn', '13900010328', 2, 3, 109, 1),
(528, '24EE02014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '董浩宇', NULL, '24EE02014@stu.usts-tp.edu.cn', '13900010329', 1, 3, 109, 1),
(529, '24EE02015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '萧思雨', NULL, '24EE02015@stu.usts-tp.edu.cn', '13900010330', 2, 3, 109, 1),
(530, '24EE02016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '程若彤', NULL, '24EE02016@stu.usts-tp.edu.cn', '13900010331', 2, 3, 109, 1),
(531, '24EE02017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曹俊宇', NULL, '24EE02017@stu.usts-tp.edu.cn', '13900010332', 1, 3, 109, 1),
(532, '24EE02018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '袁雅雯', NULL, '24EE02018@stu.usts-tp.edu.cn', '13900010333', 2, 3, 109, 1),
(533, '24EE02019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '邓思齐', NULL, '24EE02019@stu.usts-tp.edu.cn', '13900010334', 1, 3, 109, 1),
(534, '24EE02020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '许子墨', NULL, '24EE02020@stu.usts-tp.edu.cn', '13900010335', 1, 3, 109, 1),
(535, '24EE02021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '傅宇辰', NULL, '24EE02021@stu.usts-tp.edu.cn', '13900010336', 1, 3, 109, 1),
(536, '24EE02022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '沈嘉宁', NULL, '24EE02022@stu.usts-tp.edu.cn', '13900010337', 1, 3, 109, 1),
(537, '24EE02023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曾若兰', NULL, '24EE02023@stu.usts-tp.edu.cn', '13900010338', 2, 3, 109, 1),
(538, '24EE02024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '彭志洋', NULL, '24EE02024@stu.usts-tp.edu.cn', '13900010339', 1, 3, 109, 1),
(539, '24EE02025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吕文瀚', NULL, '24EE02025@stu.usts-tp.edu.cn', '13900010340', 1, 3, 109, 1),
(540, '24EE02026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '苏俊熙', NULL, '24EE02026@stu.usts-tp.edu.cn', '13900010341', 1, 3, 109, 1),
(541, '24EE02027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '卢嘉怡', NULL, '24EE02027@stu.usts-tp.edu.cn', '13900010342', 2, 3, 109, 1),
(542, '24EE02028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蒋子韬', NULL, '24EE02028@stu.usts-tp.edu.cn', '13900010343', 1, 3, 109, 1),
(543, '24EE02029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蔡梓晨', NULL, '24EE02029@stu.usts-tp.edu.cn', '13900010344', 1, 3, 109, 1),
(544, '24EE02030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '贾逸辰', NULL, '24EE02030@stu.usts-tp.edu.cn', '13900010345', 1, 3, 109, 1),
(545, '24EE02031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '丁佳琪', NULL, '24EE02031@stu.usts-tp.edu.cn', '13900010346', 2, 3, 109, 1),
(546, '24EE02032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '魏宇阳', NULL, '24EE02032@stu.usts-tp.edu.cn', '13900010347', 1, 3, 109, 1),
(547, '24EE02033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '薛欣怡', NULL, '24EE02033@stu.usts-tp.edu.cn', '13900010348', 2, 3, 109, 1),
(548, '24EE02034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '叶浩嘉', NULL, '24EE02034@stu.usts-tp.edu.cn', '13900010349', 1, 3, 109, 1),
(549, '24EE02035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '阎若云', NULL, '24EE02035@stu.usts-tp.edu.cn', '13900010350', 2, 3, 109, 1);

-- ──── 班 110 土木2401（35 人，男 28 女 7）ID 550-584 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(550, '24CE01001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王浩楠', NULL, '24CE01001@stu.usts-tp.edu.cn', '13900010351', 1, 3, 110, 1),
(551, '24CE01002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李俊朗', NULL, '24CE01002@stu.usts-tp.edu.cn', '13900010352', 1, 3, 110, 1),
(552, '24CE01003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张子琨', NULL, '24CE01003@stu.usts-tp.edu.cn', '13900010353', 1, 3, 110, 1),
(553, '24CE01004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘明远', NULL, '24CE01004@stu.usts-tp.edu.cn', '13900010354', 1, 3, 110, 1),
(554, '24CE01005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈宇豪', NULL, '24CE01005@stu.usts-tp.edu.cn', '13900010355', 1, 3, 110, 1),
(555, '24CE01006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨思杰', NULL, '24CE01006@stu.usts-tp.edu.cn', '13900010356', 1, 3, 110, 1),
(556, '24CE01007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵嘉禾', NULL, '24CE01007@stu.usts-tp.edu.cn', '13900010357', 1, 3, 110, 1),
(557, '24CE01008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄文康', NULL, '24CE01008@stu.usts-tp.edu.cn', '13900010358', 1, 3, 110, 1),
(558, '24CE01009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周毓秀', NULL, '24CE01009@stu.usts-tp.edu.cn', '13900010359', 2, 3, 110, 1),
(559, '24CE01010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴志强', NULL, '24CE01010@stu.usts-tp.edu.cn', '13900010360', 1, 3, 110, 1),
(560, '24CE01011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐俊朗', NULL, '24CE01011@stu.usts-tp.edu.cn', '13900010361', 1, 3, 110, 1),
(561, '24CE01012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙嘉睿', NULL, '24CE01012@stu.usts-tp.edu.cn', '13900010362', 1, 3, 110, 1),
(562, '24CE01013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡浩波', NULL, '24CE01013@stu.usts-tp.edu.cn', '13900010363', 1, 3, 110, 1),
(563, '24CE01014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱梓辰', NULL, '24CE01014@stu.usts-tp.edu.cn', '13900010364', 1, 3, 110, 1),
(564, '24CE01015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高文渊', NULL, '24CE01015@stu.usts-tp.edu.cn', '13900010365', 1, 3, 110, 1),
(565, '24CE01016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林子豪', NULL, '24CE01016@stu.usts-tp.edu.cn', '13900010366', 1, 3, 110, 1),
(566, '24CE01017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何雅琴', NULL, '24CE01017@stu.usts-tp.edu.cn', '13900010367', 2, 3, 110, 1),
(567, '24CE01018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭浩然', NULL, '24CE01018@stu.usts-tp.edu.cn', '13900010368', 1, 3, 110, 1),
(568, '24CE01019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马俊宏', NULL, '24CE01019@stu.usts-tp.edu.cn', '13900010369', 1, 3, 110, 1),
(569, '24CE01020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗梓萌', NULL, '24CE01020@stu.usts-tp.edu.cn', '13900010370', 2, 3, 110, 1),
(570, '24CE01021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '梁逸飞', NULL, '24CE01021@stu.usts-tp.edu.cn', '13900010371', 1, 3, 110, 1),
(571, '24CE01022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '宋宇航', NULL, '24CE01022@stu.usts-tp.edu.cn', '13900010372', 1, 3, 110, 1),
(572, '24CE01023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郑宇泽', NULL, '24CE01023@stu.usts-tp.edu.cn', '13900010373', 1, 3, 110, 1),
(573, '24CE01024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '谢嘉伟', NULL, '24CE01024@stu.usts-tp.edu.cn', '13900010374', 1, 3, 110, 1),
(574, '24CE01025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '韩志文', NULL, '24CE01025@stu.usts-tp.edu.cn', '13900010375', 1, 3, 110, 1),
(575, '24CE01026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '唐子杰', NULL, '24CE01026@stu.usts-tp.edu.cn', '13900010376', 1, 3, 110, 1),
(576, '24CE01027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '冯佳颖', NULL, '24CE01027@stu.usts-tp.edu.cn', '13900010377', 2, 3, 110, 1),
(577, '24CE01028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '于浩翔', NULL, '24CE01028@stu.usts-tp.edu.cn', '13900010378', 1, 3, 110, 1),
(578, '24CE01029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '董若梅', NULL, '24CE01029@stu.usts-tp.edu.cn', '13900010379', 2, 3, 110, 1),
(579, '24CE01030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '萧浩淼', NULL, '24CE01030@stu.usts-tp.edu.cn', '13900010380', 1, 3, 110, 1),
(580, '24CE01031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '程志远', NULL, '24CE01031@stu.usts-tp.edu.cn', '13900010381', 1, 3, 110, 1),
(581, '24CE01032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曹文俊', NULL, '24CE01032@stu.usts-tp.edu.cn', '13900010382', 1, 3, 110, 1),
(582, '24CE01033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '袁俊豪', NULL, '24CE01033@stu.usts-tp.edu.cn', '13900010383', 1, 3, 110, 1),
(583, '24CE01034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '邓嘉辉', NULL, '24CE01034@stu.usts-tp.edu.cn', '13900010384', 1, 3, 110, 1),
(584, '24CE01035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '许子谦', NULL, '24CE01035@stu.usts-tp.edu.cn', '13900010385', 1, 3, 110, 1);

-- ──── 班 111 土木2402（35 人，男 28 女 7）ID 585-619 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(585, '24CE02001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '傅明哲', NULL, '24CE02001@stu.usts-tp.edu.cn', '13900010386', 1, 3, 111, 1),
(586, '24CE02002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '沈宇恒', NULL, '24CE02002@stu.usts-tp.edu.cn', '13900010387', 1, 3, 111, 1),
(587, '24CE02003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曾欣怡', NULL, '24CE02003@stu.usts-tp.edu.cn', '13900010388', 2, 3, 111, 1),
(588, '24CE02004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '彭俊磊', NULL, '24CE02004@stu.usts-tp.edu.cn', '13900010389', 1, 3, 111, 1),
(589, '24CE02005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吕浩天', NULL, '24CE02005@stu.usts-tp.edu.cn', '13900010390', 1, 3, 111, 1),
(590, '24CE02006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '苏文豪', NULL, '24CE02006@stu.usts-tp.edu.cn', '13900010391', 1, 3, 111, 1),
(591, '24CE02007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '卢俊辉', NULL, '24CE02007@stu.usts-tp.edu.cn', '13900010392', 1, 3, 111, 1),
(592, '24CE02008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蒋嘉伟', NULL, '24CE02008@stu.usts-tp.edu.cn', '13900010393', 1, 3, 111, 1),
(593, '24CE02009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蔡子博', NULL, '24CE02009@stu.usts-tp.edu.cn', '13900010394', 1, 3, 111, 1),
(594, '24CE02010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '贾梓杰', NULL, '24CE02010@stu.usts-tp.edu.cn', '13900010395', 1, 3, 111, 1),
(595, '24CE02011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '丁逸飞', NULL, '24CE02011@stu.usts-tp.edu.cn', '13900010396', 1, 3, 111, 1),
(596, '24CE02012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '魏欣冉', NULL, '24CE02012@stu.usts-tp.edu.cn', '13900010397', 2, 3, 111, 1),
(597, '24CE02013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '薛宇恒', NULL, '24CE02013@stu.usts-tp.edu.cn', '13900010398', 1, 3, 111, 1),
(598, '24CE02014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '叶志成', NULL, '24CE02014@stu.usts-tp.edu.cn', '13900010399', 1, 3, 111, 1),
(599, '24CE02015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '阎浩瀚', NULL, '24CE02015@stu.usts-tp.edu.cn', '13900010400', 1, 3, 111, 1),
(600, '24CE02016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王志刚', NULL, '24CE02016@stu.usts-tp.edu.cn', '13900010401', 1, 3, 111, 1),
(601, '24CE02017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李俊鹏', NULL, '24CE02017@stu.usts-tp.edu.cn', '13900010402', 1, 3, 111, 1),
(602, '24CE02018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张文渊', NULL, '24CE02018@stu.usts-tp.edu.cn', '13900010403', 1, 3, 111, 1),
(603, '24CE02019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘俊豪', NULL, '24CE02019@stu.usts-tp.edu.cn', '13900010404', 1, 3, 111, 1),
(604, '24CE02020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈明远', NULL, '24CE02020@stu.usts-tp.edu.cn', '13900010405', 1, 3, 111, 1),
(605, '24CE02021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨佳琪', NULL, '24CE02021@stu.usts-tp.edu.cn', '13900010406', 2, 3, 111, 1),
(606, '24CE02022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵嘉伟', NULL, '24CE02022@stu.usts-tp.edu.cn', '13900010407', 1, 3, 111, 1),
(607, '24CE02023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄宇澄', NULL, '24CE02023@stu.usts-tp.edu.cn', '13900010408', 1, 3, 111, 1),
(608, '24CE02024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周思琪', NULL, '24CE02024@stu.usts-tp.edu.cn', '13900010409', 2, 3, 111, 1),
(609, '24CE02025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴若晨', NULL, '24CE02025@stu.usts-tp.edu.cn', '13900010410', 2, 3, 111, 1),
(610, '24CE02026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐子健', NULL, '24CE02026@stu.usts-tp.edu.cn', '13900010411', 1, 3, 111, 1),
(611, '24CE02027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙嘉伦', NULL, '24CE02027@stu.usts-tp.edu.cn', '13900010412', 1, 3, 111, 1),
(612, '24CE02028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡文森', NULL, '24CE02028@stu.usts-tp.edu.cn', '13900010413', 1, 3, 111, 1),
(613, '24CE02029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱明远', NULL, '24CE02029@stu.usts-tp.edu.cn', '13900010414', 1, 3, 111, 1),
(614, '24CE02030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高梓骞', NULL, '24CE02030@stu.usts-tp.edu.cn', '13900010415', 1, 3, 111, 1),
(615, '24CE02031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林俊宇', NULL, '24CE02031@stu.usts-tp.edu.cn', '13900010416', 1, 3, 111, 1),
(616, '24CE02032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何雅芝', NULL, '24CE02032@stu.usts-tp.edu.cn', '13900010417', 2, 3, 111, 1),
(617, '24CE02033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭子博', NULL, '24CE02033@stu.usts-tp.edu.cn', '13900010418', 1, 3, 111, 1),
(618, '24CE02034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马俊翔', NULL, '24CE02034@stu.usts-tp.edu.cn', '13900010419', 1, 3, 111, 1),
(619, '24CE02035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗逸辰', NULL, '24CE02035@stu.usts-tp.edu.cn', '13900010420', 1, 3, 111, 1);

-- ──── 班 112 日语2401（35 人，男 7 女 28，反映文科外语专业实际比例）ID 620-654 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(620, '24JP01001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王雅蓉', NULL, '24JP01001@stu.usts-tp.edu.cn', '13900010421', 2, 3, 112, 1),
(621, '24JP01002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李欣怡', NULL, '24JP01002@stu.usts-tp.edu.cn', '13900010422', 2, 3, 112, 1),
(622, '24JP01003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张梓萱', NULL, '24JP01003@stu.usts-tp.edu.cn', '13900010423', 2, 3, 112, 1),
(623, '24JP01004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘佳琪', NULL, '24JP01004@stu.usts-tp.edu.cn', '13900010424', 2, 3, 112, 1),
(624, '24JP01005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈雅琴', NULL, '24JP01005@stu.usts-tp.edu.cn', '13900010425', 2, 3, 112, 1),
(625, '24JP01006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨思雨', NULL, '24JP01006@stu.usts-tp.edu.cn', '13900010426', 2, 3, 112, 1),
(626, '24JP01007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵子涵', NULL, '24JP01007@stu.usts-tp.edu.cn', '13900010427', 2, 3, 112, 1),
(627, '24JP01008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄欣然', NULL, '24JP01008@stu.usts-tp.edu.cn', '13900010428', 2, 3, 112, 1),
(628, '24JP01009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周若曦', NULL, '24JP01009@stu.usts-tp.edu.cn', '13900010429', 2, 3, 112, 1),
(629, '24JP01010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴梓琳', NULL, '24JP01010@stu.usts-tp.edu.cn', '13900010430', 2, 3, 112, 1),
(630, '24JP01011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐睿哲', NULL, '24JP01011@stu.usts-tp.edu.cn', '13900010431', 1, 3, 112, 1),
(631, '24JP01012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙若兰', NULL, '24JP01012@stu.usts-tp.edu.cn', '13900010432', 2, 3, 112, 1),
(632, '24JP01013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡梓宁', NULL, '24JP01013@stu.usts-tp.edu.cn', '13900010433', 2, 3, 112, 1),
(633, '24JP01014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱嘉颖', NULL, '24JP01014@stu.usts-tp.edu.cn', '13900010434', 2, 3, 112, 1),
(634, '24JP01015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高浩然', NULL, '24JP01015@stu.usts-tp.edu.cn', '13900010435', 1, 3, 112, 1),
(635, '24JP01016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林婉清', NULL, '24JP01016@stu.usts-tp.edu.cn', '13900010436', 2, 3, 112, 1),
(636, '24JP01017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何思琪', NULL, '24JP01017@stu.usts-tp.edu.cn', '13900010437', 2, 3, 112, 1),
(637, '24JP01018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭佳怡', NULL, '24JP01018@stu.usts-tp.edu.cn', '13900010438', 2, 3, 112, 1),
(638, '24JP01019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马欣彤', NULL, '24JP01019@stu.usts-tp.edu.cn', '13900010439', 2, 3, 112, 1),
(639, '24JP01020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗子淇', NULL, '24JP01020@stu.usts-tp.edu.cn', '13900010440', 2, 3, 112, 1),
(640, '24JP01021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '梁晓婷', NULL, '24JP01021@stu.usts-tp.edu.cn', '13900010441', 2, 3, 112, 1),
(641, '24JP01022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '宋文博', NULL, '24JP01022@stu.usts-tp.edu.cn', '13900010442', 1, 3, 112, 1),
(642, '24JP01023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郑梓萌', NULL, '24JP01023@stu.usts-tp.edu.cn', '13900010443', 2, 3, 112, 1),
(643, '24JP01024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '谢梦琪', NULL, '24JP01024@stu.usts-tp.edu.cn', '13900010444', 2, 3, 112, 1),
(644, '24JP01025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '韩佳音', NULL, '24JP01025@stu.usts-tp.edu.cn', '13900010445', 2, 3, 112, 1),
(645, '24JP01026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '唐若雯', NULL, '24JP01026@stu.usts-tp.edu.cn', '13900010446', 2, 3, 112, 1),
(646, '24JP01027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '冯子瑜', NULL, '24JP01027@stu.usts-tp.edu.cn', '13900010447', 2, 3, 112, 1),
(647, '24JP01028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '于子韬', NULL, '24JP01028@stu.usts-tp.edu.cn', '13900010448', 1, 3, 112, 1),
(648, '24JP01029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '董雅琪', NULL, '24JP01029@stu.usts-tp.edu.cn', '13900010449', 2, 3, 112, 1),
(649, '24JP01030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '萧子淇', NULL, '24JP01030@stu.usts-tp.edu.cn', '13900010450', 2, 3, 112, 1),
(650, '24JP01031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '程文博', NULL, '24JP01031@stu.usts-tp.edu.cn', '13900010451', 1, 3, 112, 1),
(651, '24JP01032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曹欣晴', NULL, '24JP01032@stu.usts-tp.edu.cn', '13900010452', 2, 3, 112, 1),
(652, '24JP01033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '袁嘉莹', NULL, '24JP01033@stu.usts-tp.edu.cn', '13900010453', 2, 3, 112, 1),
(653, '24JP01034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '邓诗琪', NULL, '24JP01034@stu.usts-tp.edu.cn', '13900010454', 2, 3, 112, 1),
(654, '24JP01035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '许雅婷', NULL, '24JP01035@stu.usts-tp.edu.cn', '13900010455', 2, 3, 112, 1);

-- ──── 班 113 日语2402（35 人，男 7 女 28）ID 655-689 ────
INSERT INTO sys_user (id, username, password, real_name, avatar, email, phone, gender, role_id, class_id, status) VALUES
(655, '24JP02001', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '傅婉清', NULL, '24JP02001@stu.usts-tp.edu.cn', '13900010456', 2, 3, 113, 1),
(656, '24JP02002', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '沈欣琪', NULL, '24JP02002@stu.usts-tp.edu.cn', '13900010457', 2, 3, 113, 1),
(657, '24JP02003', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '曾梓萌', NULL, '24JP02003@stu.usts-tp.edu.cn', '13900010458', 2, 3, 113, 1),
(658, '24JP02004', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '彭浩然', NULL, '24JP02004@stu.usts-tp.edu.cn', '13900010459', 1, 3, 113, 1),
(659, '24JP02005', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吕雅楠', NULL, '24JP02005@stu.usts-tp.edu.cn', '13900010460', 2, 3, 113, 1),
(660, '24JP02006', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '苏佳颖', NULL, '24JP02006@stu.usts-tp.edu.cn', '13900010461', 2, 3, 113, 1),
(661, '24JP02007', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '卢梦瑶', NULL, '24JP02007@stu.usts-tp.edu.cn', '13900010462', 2, 3, 113, 1),
(662, '24JP02008', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蒋若雪', NULL, '24JP02008@stu.usts-tp.edu.cn', '13900010463', 2, 3, 113, 1),
(663, '24JP02009', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '蔡诗涵', NULL, '24JP02009@stu.usts-tp.edu.cn', '13900010464', 2, 3, 113, 1),
(664, '24JP02010', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '贾子轩', NULL, '24JP02010@stu.usts-tp.edu.cn', '13900010465', 1, 3, 113, 1),
(665, '24JP02011', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '丁佳音', NULL, '24JP02011@stu.usts-tp.edu.cn', '13900010466', 2, 3, 113, 1),
(666, '24JP02012', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '魏婉婷', NULL, '24JP02012@stu.usts-tp.edu.cn', '13900010467', 2, 3, 113, 1),
(667, '24JP02013', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '薛思怡', NULL, '24JP02013@stu.usts-tp.edu.cn', '13900010468', 2, 3, 113, 1),
(668, '24JP02014', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '叶雅芳', NULL, '24JP02014@stu.usts-tp.edu.cn', '13900010469', 2, 3, 113, 1),
(669, '24JP02015', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '阎雨萱', NULL, '24JP02015@stu.usts-tp.edu.cn', '13900010470', 2, 3, 113, 1),
(670, '24JP02016', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '王宇翔', NULL, '24JP02016@stu.usts-tp.edu.cn', '13900010471', 1, 3, 113, 1),
(671, '24JP02017', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '李思辰', NULL, '24JP02017@stu.usts-tp.edu.cn', '13900010472', 1, 3, 113, 1),
(672, '24JP02018', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '张子琳', NULL, '24JP02018@stu.usts-tp.edu.cn', '13900010473', 2, 3, 113, 1),
(673, '24JP02019', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '刘梓妍', NULL, '24JP02019@stu.usts-tp.edu.cn', '13900010474', 2, 3, 113, 1),
(674, '24JP02020', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '陈雅婷', NULL, '24JP02020@stu.usts-tp.edu.cn', '13900010475', 2, 3, 113, 1),
(675, '24JP02021', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '杨佳怡', NULL, '24JP02021@stu.usts-tp.edu.cn', '13900010476', 2, 3, 113, 1),
(676, '24JP02022', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '赵欣然', NULL, '24JP02022@stu.usts-tp.edu.cn', '13900010477', 2, 3, 113, 1),
(677, '24JP02023', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '黄睿哲', NULL, '24JP02023@stu.usts-tp.edu.cn', '13900010478', 1, 3, 113, 1),
(678, '24JP02024', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '周诗琪', NULL, '24JP02024@stu.usts-tp.edu.cn', '13900010479', 2, 3, 113, 1),
(679, '24JP02025', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '吴梦瑶', NULL, '24JP02025@stu.usts-tp.edu.cn', '13900010480', 2, 3, 113, 1),
(680, '24JP02026', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '徐若宸', NULL, '24JP02026@stu.usts-tp.edu.cn', '13900010481', 1, 3, 113, 1),
(681, '24JP02027', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '孙雅琴', NULL, '24JP02027@stu.usts-tp.edu.cn', '13900010482', 2, 3, 113, 1),
(682, '24JP02028', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '胡梓琪', NULL, '24JP02028@stu.usts-tp.edu.cn', '13900010483', 2, 3, 113, 1),
(683, '24JP02029', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '朱欣彤', NULL, '24JP02029@stu.usts-tp.edu.cn', '13900010484', 2, 3, 113, 1),
(684, '24JP02030', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '高若瑶', NULL, '24JP02030@stu.usts-tp.edu.cn', '13900010485', 2, 3, 113, 1),
(685, '24JP02031', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '林佳琦', NULL, '24JP02031@stu.usts-tp.edu.cn', '13900010486', 2, 3, 113, 1),
(686, '24JP02032', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '何子瑶', NULL, '24JP02032@stu.usts-tp.edu.cn', '13900010487', 2, 3, 113, 1),
(687, '24JP02033', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '郭书瑶', NULL, '24JP02033@stu.usts-tp.edu.cn', '13900010488', 2, 3, 113, 1),
(688, '24JP02034', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '马思婷', NULL, '24JP02034@stu.usts-tp.edu.cn', '13900010489', 2, 3, 113, 1),
(689, '24JP02035', '$2a$10$.w8wQyvUsIdvThwM/ITO/.tnxsay7QLc0tDWcDYq1lCxvZeTGGUkW', '罗梦瑶', NULL, '24JP02035@stu.usts-tp.edu.cn', '13900010490', 2, 3, 113, 1);

-- ============================================================
-- 9. v2.1 学号工号映射（与苏科大天平学院教务系统真实编码对齐）
-- ============================================================
-- 实现策略：先用旧式 username 完成 INSERT，本章节用 14 条按班 UPDATE +
-- 24 条按教师 UPDATE 一次性把每位用户的 username 与 email 同步替换为
-- 真实学号/工号格式。SQL 文件可读性 + UNIQUE 约束不冲突 + 完全幂等。
--
-- 学生学号公式：'24' + 专业代码(2) + '1' + 学院代码(2) + 班号(1) + LPAD(班内序号,2,'0')
--   id - 班首ID + 1 = 班内序号（1..35），LPAD 补零成 2 位
--   邮箱 = <学号> + '@stu.usts-tp.edu.cn'
-- ============================================================

-- ──── 学生：14 条按班 UPDATE，将旧式占位 username 重写为 10 位真实学号 ────

-- 班 100 计算机2401 (id 200-234) → 学号 2430107101..2430107135
UPDATE sys_user SET
  username = CONCAT('2430107', '1', LPAD(id - 200 + 1, 2, '0')),
  email    = CONCAT('2430107', '1', LPAD(id - 200 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 100 AND role_id = 3 AND id BETWEEN 200 AND 234;

-- 班 101 计算机2402 (id 235-269) → 学号 2430107201..2430107235
UPDATE sys_user SET
  username = CONCAT('2430107', '2', LPAD(id - 235 + 1, 2, '0')),
  email    = CONCAT('2430107', '2', LPAD(id - 235 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 101 AND role_id = 3 AND id BETWEEN 235 AND 269;

-- 班 102 通信工程2401 (id 270-304) → 学号 2432107101..2432107135
UPDATE sys_user SET
  username = CONCAT('2432107', '1', LPAD(id - 270 + 1, 2, '0')),
  email    = CONCAT('2432107', '1', LPAD(id - 270 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 102 AND role_id = 3 AND id BETWEEN 270 AND 304;

-- 班 103 通信工程2402 (id 305-339) → 学号 2432107201..2432107235
UPDATE sys_user SET
  username = CONCAT('2432107', '2', LPAD(id - 305 + 1, 2, '0')),
  email    = CONCAT('2432107', '2', LPAD(id - 305 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 103 AND role_id = 3 AND id BETWEEN 305 AND 339;

-- 班 104 电子信息2401 (id 340-374) → 学号 2429107101..2429107135
UPDATE sys_user SET
  username = CONCAT('2429107', '1', LPAD(id - 340 + 1, 2, '0')),
  email    = CONCAT('2429107', '1', LPAD(id - 340 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 104 AND role_id = 3 AND id BETWEEN 340 AND 374;

-- 班 105 电子信息2402 (id 375-409) → 学号 2429107201..2429107235
UPDATE sys_user SET
  username = CONCAT('2429107', '2', LPAD(id - 375 + 1, 2, '0')),
  email    = CONCAT('2429107', '2', LPAD(id - 375 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 105 AND role_id = 3 AND id BETWEEN 375 AND 409;

-- 班 106 机械2401 (id 410-444) → 学号 2433108101..2433108135
UPDATE sys_user SET
  username = CONCAT('2433108', '1', LPAD(id - 410 + 1, 2, '0')),
  email    = CONCAT('2433108', '1', LPAD(id - 410 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 106 AND role_id = 3 AND id BETWEEN 410 AND 444;

-- 班 107 机械2402 (id 445-479) → 学号 2433108201..2433108235
UPDATE sys_user SET
  username = CONCAT('2433108', '2', LPAD(id - 445 + 1, 2, '0')),
  email    = CONCAT('2433108', '2', LPAD(id - 445 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 107 AND role_id = 3 AND id BETWEEN 445 AND 479;

-- 班 108 电气2401 (id 480-514) → 学号 2431108101..2431108135
UPDATE sys_user SET
  username = CONCAT('2431108', '1', LPAD(id - 480 + 1, 2, '0')),
  email    = CONCAT('2431108', '1', LPAD(id - 480 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 108 AND role_id = 3 AND id BETWEEN 480 AND 514;

-- 班 109 电气2402 (id 515-549) → 学号 2431108201..2431108235
UPDATE sys_user SET
  username = CONCAT('2431108', '2', LPAD(id - 515 + 1, 2, '0')),
  email    = CONCAT('2431108', '2', LPAD(id - 515 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 109 AND role_id = 3 AND id BETWEEN 515 AND 549;

-- 班 110 土木2401 (id 550-584) → 学号 2428103101..2428103135
UPDATE sys_user SET
  username = CONCAT('2428103', '1', LPAD(id - 550 + 1, 2, '0')),
  email    = CONCAT('2428103', '1', LPAD(id - 550 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 110 AND role_id = 3 AND id BETWEEN 550 AND 584;

-- 班 111 土木2402 (id 585-619) → 学号 2428103201..2428103235
UPDATE sys_user SET
  username = CONCAT('2428103', '2', LPAD(id - 585 + 1, 2, '0')),
  email    = CONCAT('2428103', '2', LPAD(id - 585 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 111 AND role_id = 3 AND id BETWEEN 585 AND 619;

-- 班 112 日语2401 (id 620-654) → 学号 2410105101..2410105135
UPDATE sys_user SET
  username = CONCAT('2410105', '1', LPAD(id - 620 + 1, 2, '0')),
  email    = CONCAT('2410105', '1', LPAD(id - 620 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 112 AND role_id = 3 AND id BETWEEN 620 AND 654;

-- 班 113 日语2402 (id 655-689) → 学号 2410105201..2410105235
UPDATE sys_user SET
  username = CONCAT('2410105', '2', LPAD(id - 655 + 1, 2, '0')),
  email    = CONCAT('2410105', '2', LPAD(id - 655 + 1, 2, '0'), '@stu.usts-tp.edu.cn')
WHERE class_id = 113 AND role_id = 3 AND id BETWEEN 655 AND 689;

-- ──── v2.4：教师 username 保持 INSERT 时的姓名拼音（与邮箱前缀一致） ────
-- v2.1 曾按「YY(入职年) + 学院代码(2) + 学院内序号(2)」设计 6 位真实工号（如 080201），
-- 但 6 位工号无法直观识别教师身份（admin 测试登录时需查表）；v2.4 改为姓名拼音登录方式：
--   - username = 拼音（wangjianhua / sunhaiyan / wujunhua ...），与邮箱前缀完全一致
--   - 真实苏科大教职工统一身份认证支持「邮箱前缀登录」，本系统遵循同一惯例
--   - 学生 username 保持 10 位真实学号（如 2430107203），符合学生用学号登录教务系统的习惯
-- 该调整与论文 sys_user 字段表（仅含 username 一列登录字段）完全一致，零侵入。

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

-- ============================================================
-- 验证断言
-- ============================================================
SELECT '专业'           AS 实体, COUNT(*) AS 实际,   7 AS 预期 FROM edu_major       WHERE id >= 100
UNION ALL SELECT '科目',           COUNT(*),  14 FROM edu_subject     WHERE id >= 100
UNION ALL SELECT '科目专业关联',   COUNT(*),  38 FROM subject_major   WHERE subject_id >= 100 OR major_id >= 100
UNION ALL SELECT '班级',           COUNT(*),  14 FROM edu_class       WHERE id >= 100
UNION ALL SELECT '教师',           COUNT(*),  24 FROM sys_user        WHERE id >= 100 AND id <= 199 AND role_id = 2
UNION ALL SELECT '学生',           COUNT(*), 490 FROM sys_user        WHERE id >= 200 AND role_id = 3
UNION ALL SELECT '教师班级关联',   COUNT(*),  72 FROM teacher_class   WHERE teacher_id >= 100
UNION ALL SELECT '教师科目关联',   COUNT(*),  28 FROM teacher_subject WHERE teacher_id >= 100
UNION ALL SELECT '学生username为10位数字', COUNT(*), 490 FROM sys_user WHERE id BETWEEN 200 AND 689 AND role_id = 3 AND username REGEXP '^[0-9]{10}$'
UNION ALL SELECT '教师username为6位数字',  COUNT(*),  24 FROM sys_user WHERE id BETWEEN 100 AND 199 AND role_id = 2 AND username REGEXP '^[0-9]{6}$';

-- v2.1 学号格式抽样核对（应全部命中）
SELECT '学号样本：计算机2401第1号' AS 样本, username AS 实际, '2430107101' AS 预期 FROM sys_user WHERE id = 200
UNION ALL SELECT '学号样本：计算机2402第35号',    username, '2430107235' FROM sys_user WHERE id = 269
UNION ALL SELECT '学号样本：通信工程2401第1号',  username, '2432107101' FROM sys_user WHERE id = 270
UNION ALL SELECT '学号样本：电子信息2401第1号',  username, '2429107101' FROM sys_user WHERE id = 340
UNION ALL SELECT '学号样本：机械2401第1号',      username, '2433108101' FROM sys_user WHERE id = 410
UNION ALL SELECT '学号样本：电气2402第35号',     username, '2431108235' FROM sys_user WHERE id = 549
UNION ALL SELECT '学号样本：土木2401第1号',      username, '2428103101' FROM sys_user WHERE id = 550
UNION ALL SELECT '学号样本：日语2402第35号',     username, '2410105235' FROM sys_user WHERE id = 689
UNION ALL SELECT '工号样本：王建华(数理)',       username, '080201'     FROM sys_user WHERE id = 100
UNION ALL SELECT '工号样本：李春梅(外语)',       username, '080401'     FROM sys_user WHERE id = 104
UNION ALL SELECT '工号样本：陈晓峰(电信)',       username, '090701'     FROM sys_user WHERE id = 109
UNION ALL SELECT '工号样本：林涵琳(电信)',       username, '170709'     FROM sys_user WHERE id = 117;

-- 每班学生人数核验（应全部为 35）
SELECT c.id AS 班级ID, c.class_name AS 班级名, COUNT(u.id) AS 实际人数, 35 AS 预期
FROM edu_class c
LEFT JOIN sys_user u ON u.class_id = c.id AND u.role_id = 3
WHERE c.id >= 100
GROUP BY c.id, c.class_name
ORDER BY c.id;

-- 每位老师任课班级数核验
SELECT u.id AS 教师ID, u.real_name AS 姓名,
       (SELECT GROUP_CONCAT(s.subject_name SEPARATOR '/') FROM teacher_subject ts JOIN edu_subject s ON ts.subject_id = s.id WHERE ts.teacher_id = u.id) AS 任教科目,
       COUNT(tc.class_id) AS 任课班数
FROM sys_user u
LEFT JOIN teacher_class tc ON u.id = tc.teacher_id
WHERE u.id BETWEEN 100 AND 199 AND u.role_id = 2
GROUP BY u.id, u.real_name
ORDER BY u.id;
