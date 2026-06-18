--
--   新设计：teacher_class 是唯一信息源；教师任课某科目当且仅当其班级
--   (grade, major_id) 与科目 (grade, major_id) 严格相等。科目页「任课
--   教师」列、用户页「负责科目」列均由后端实时衍生，无需手动维护。
--
-- 本脚本：
--   1) 清空 teacher_subject 表残留数据（这些数据已不再被读写，留着也是
--      死数据；删除后磁盘干净，索引轻量）。
--   2) 保留表结构（暂不 DROP），以便万一需要回滚衍生模式时可恢复历史
--      绑定；线上稳定运行一段时间后可再 DROP。
--
-- 副作用：执行后，user-manage 中「负责科目」展示与 subject-manage 中
--   「任课教师」展示，都将完全由 teacher_class 决定，与本表彻底解耦。
-- ============================================================

-- ⚠ 以下原破坏性 SQL 已被禁用 (会清掉 14 号脚本写入的精准任课数据)
-- 如需重置 teacher_subject 数据, 请直接重跑 14_教师任课精准数据.sql,
-- 它顶部的 DELETE FROM 会幂等清理后再插入。

-- SELECT COUNT(*) AS '清空前 teacher_subject 行数' FROM teacher_subject;
-- TRUNCATE TABLE teacher_subject;
-- SELECT COUNT(*) AS '清空后 teacher_subject 行数' FROM teacher_subject;

-- 4. 衍生模式自检：查看当前各班级实际覆盖的科目数（按 grade × major_id 双键）
--    用于验证后端衍生查询的预期行数；陆卫忠 (id=690) 任 计算机2201/2202 → 应能看到
--    一行 (grade=2022级, major_id=100, 课程数 ≈ 37)。
SELECT
  c.grade,
  c.major_id,
  m.major_name,
  COUNT(DISTINCT s.id) AS subject_count
FROM edu_class c
JOIN edu_major m ON m.id = c.major_id
LEFT JOIN edu_subject s
  ON s.grade = c.grade AND s.major_id = c.major_id
GROUP BY c.grade, c.major_id, m.major_name
ORDER BY c.grade, m.major_name;
