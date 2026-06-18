-- 教务数据核对报告生成器（只读）。输出为 Markdown，每个 SELECT 产出一行 MD。
-- 运行：mysql --skip-column-names -B --default-character-set=utf8mb4 online_exam_system < edu_report.sql > doc\edu_audit_report.md
SET SESSION group_concat_max_len = 1000000;

SELECT '# 教务数据核对报告';
SELECT '';
SELECT CONCAT('> 数据库 `online_exam_system`　|　专业 ',
  (SELECT COUNT(*) FROM edu_major), '　班级 ',
  (SELECT COUNT(*) FROM edu_class), '　课程 ',
  (SELECT COUNT(*) FROM edu_subject), '　教师 ',
  (SELECT COUNT(*) FROM sys_user u JOIN sys_role r ON u.role_id=r.id WHERE r.role_code='TEACHER'), '　学生 ',
  (SELECT COUNT(*) FROM sys_user u JOIN sys_role r ON u.role_id=r.id WHERE r.role_code='STUDENT'));
SELECT '';
SELECT '> 年级：2022级、2024级　|　本报告由 SQL 自动生成，用于逐项核对。';
SELECT '> 教师任课说明：**班级**来源 `teacher_class`（衍生模式唯一信息源）；**任课科目**来源 `teacher_subject`（精准任课表）。';
SELECT '';

-- ============ 一、总览 ============
SELECT '## 一、总览（按专业；班级 / 课程 / 学生 列格式 = 2022级 / 2024级）';
SELECT '';
SELECT '| ID | 专业 | 班级 | 课程 | 学生 |';
SELECT '|---|---|---|---|---|';
SELECT CONCAT('| ', m.id, ' | ', m.major_name, ' | ',
  IFNULL((SELECT COUNT(*) FROM edu_class c WHERE c.major_id=m.id AND c.grade='2022级'),0),' / ',
  IFNULL((SELECT COUNT(*) FROM edu_class c WHERE c.major_id=m.id AND c.grade='2024级'),0),' | ',
  IFNULL((SELECT COUNT(*) FROM edu_subject s WHERE s.major_id=m.id AND s.grade='2022级'),0),' / ',
  IFNULL((SELECT COUNT(*) FROM edu_subject s WHERE s.major_id=m.id AND s.grade='2024级'),0),' | ',
  IFNULL((SELECT COUNT(*) FROM sys_user u JOIN sys_role r ON u.role_id=r.id AND r.role_code='STUDENT' JOIN edu_class c ON u.class_id=c.id WHERE c.major_id=m.id AND c.grade='2022级'),0),' / ',
  IFNULL((SELECT COUNT(*) FROM sys_user u JOIN sys_role r ON u.role_id=r.id AND r.role_code='STUDENT' JOIN edu_class c ON u.class_id=c.id WHERE c.major_id=m.id AND c.grade='2024级'),0),' |')
FROM edu_major m ORDER BY m.id;
SELECT '';

-- ============ 二、缺口检测 ============
SELECT '## 二、数据缺口（自动检测）';
SELECT '';
SELECT CONCAT('- **无班级的专业**：', IFNULL((
  SELECT GROUP_CONCAT(m.major_name ORDER BY m.id SEPARATOR '、')
  FROM edu_major m WHERE NOT EXISTS (SELECT 1 FROM edu_class c WHERE c.major_id=m.id)), '（无）'));
SELECT CONCAT('- **有课程但无班级的专业**：', IFNULL((
  SELECT GROUP_CONCAT(DISTINCT m.major_name ORDER BY m.id SEPARATOR '、')
  FROM edu_major m WHERE EXISTS(SELECT 1 FROM edu_subject s WHERE s.major_id=m.id)
    AND NOT EXISTS(SELECT 1 FROM edu_class c WHERE c.major_id=m.id)), '（无）'));
SELECT CONCAT('- **有班级但无学生的班级**（',
  (SELECT COUNT(*) FROM edu_class c WHERE NOT EXISTS(SELECT 1 FROM sys_user u JOIN sys_role r ON u.role_id=r.id AND r.role_code='STUDENT' WHERE u.class_id=c.id)),
  ' 个）：', IFNULL((
  SELECT GROUP_CONCAT(c.class_name ORDER BY c.grade,c.major_id SEPARATOR '、')
  FROM edu_class c WHERE NOT EXISTS(SELECT 1 FROM sys_user u JOIN sys_role r ON u.role_id=r.id AND r.role_code='STUDENT' WHERE u.class_id=c.id)), '（无）'));
SELECT CONCAT('- **没有任课教师的班级**（',
  (SELECT COUNT(*) FROM edu_class c WHERE NOT EXISTS(SELECT 1 FROM teacher_class tc WHERE tc.class_id=c.id)),
  ' 个）：', IFNULL((
  SELECT GROUP_CONCAT(c.class_name ORDER BY c.grade,c.major_id SEPARATOR '、')
  FROM edu_class c WHERE NOT EXISTS(SELECT 1 FROM teacher_class tc WHERE tc.class_id=c.id)), '（无）'));
SELECT '';

-- ============ 三、专业清单 ============
SELECT '## 三、专业清单';
SELECT '';
SELECT CONCAT('- `', id, '`　', major_name) FROM edu_major ORDER BY id;
SELECT '';

-- ============ 四、班级（按专业） ============
SELECT '## 四、班级（按专业）';
SELECT '';
SELECT CONCAT('- **', m.major_name, '**：',
  GROUP_CONCAT(CONCAT(c.class_name,'（',c.grade,'）') ORDER BY c.grade,c.class_name SEPARATOR '、'))
FROM edu_class c JOIN edu_major m ON c.major_id=m.id
GROUP BY m.id ORDER BY m.id;
SELECT '';

-- ============ 五、课程清单（按专业·年级） ============
SELECT '## 五、课程清单（按专业 · 年级）';
SELECT '';
SELECT CONCAT('- **', m.major_name, ' · ', s.grade, '**（', COUNT(*), ' 门）：',
  GROUP_CONCAT(s.subject_name ORDER BY s.id SEPARATOR '、'))
FROM edu_subject s JOIN edu_major m ON s.major_id=m.id
GROUP BY m.id, s.grade ORDER BY m.id, s.grade;
SELECT '';

-- ============ 六、教师 ============
SELECT '## 六、教师';
SELECT '';
SELECT '### 6.1 各专业 · 年级 任课教师（来源 teacher_class 衍生）';
SELECT '';
SELECT CONCAT('- **', m.major_name, ' · ', c.grade, '**：',
  GROUP_CONCAT(DISTINCT t.real_name ORDER BY t.real_name SEPARATOR '、'))
FROM teacher_class tc JOIN edu_class c ON tc.class_id=c.id
JOIN edu_major m ON c.major_id=m.id JOIN sys_user t ON tc.teacher_id=t.id
GROUP BY m.id, c.grade ORDER BY m.id, c.grade;
SELECT '';
SELECT '### 6.2 教师任课详单（班级来源 teacher_class｜科目来源 teacher_subject）';
SELECT '';
SELECT CONCAT('- ', t.real_name, ' `', t.username, '` (id=', t.id, ')｜班级：',
  COALESCE((SELECT GROUP_CONCAT(c.class_name ORDER BY c.grade,c.class_name SEPARATOR '、')
            FROM teacher_class tc JOIN edu_class c ON tc.class_id=c.id WHERE tc.teacher_id=t.id),'—'),
  '｜任课科目：',
  COALESCE((SELECT GROUP_CONCAT(CONCAT(es.grade,' ',es.subject_name) ORDER BY es.grade,es.id SEPARATOR '、')
            FROM teacher_subject ts JOIN edu_subject es ON ts.subject_id=es.id WHERE ts.teacher_id=t.id),'—'))
FROM sys_user t JOIN sys_role r ON t.role_id=r.id AND r.role_code='TEACHER'
ORDER BY t.id;
SELECT '';

-- ============ 七、学生名单（按班级） ============
SELECT '## 七、学生名单（按班级，含学号）';
SELECT '';
SELECT CONCAT('- **', c.class_name, '**（', c.grade, '，', COUNT(u.id), ' 人）：',
  GROUP_CONCAT(CONCAT(u.real_name,'(',u.username,')') ORDER BY u.username SEPARATOR '、'))
FROM edu_class c
JOIN sys_user u ON u.class_id=c.id
JOIN sys_role r ON u.role_id=r.id AND r.role_code='STUDENT'
GROUP BY c.id ORDER BY c.grade, c.major_id, c.class_name;
SELECT '';
SELECT '> 报告结束。';
