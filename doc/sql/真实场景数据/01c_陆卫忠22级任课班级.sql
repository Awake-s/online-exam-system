-- ============================================================
-- 01c_陆卫忠22级任课班级.sql
--   基础库 id=14 (luweizhong) 绑定 22 级计科 2201/2202 (class 200/201)
--   须在 05_班级扩容 之后、14/18 任课脚本之前执行
-- ============================================================

USE online_exam_system;
SET NAMES utf8mb4;

INSERT IGNORE INTO teacher_class (teacher_id, class_id) VALUES
(14, 200),
(14, 201);

SELECT '陆卫忠22级班级绑定' AS 项目, COUNT(*) AS 实际, 2 AS 预期
FROM teacher_class WHERE teacher_id = 14 AND class_id IN (200, 201);
