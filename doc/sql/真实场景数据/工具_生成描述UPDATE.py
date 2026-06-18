# -*- coding: utf-8 -*-
"""
工具_生成描述UPDATE.py
v8 描述统一: 把 07 数据(1491 条)的 description 升级为 6 字段格式
策略: UPDATE BY (grade, major_id, subject_name) 三元组, 不改 id, 不破坏 teacher_subject 关联

输出: 19_课程描述统一升级.sql
"""
import os
import importlib.util
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "19_课程描述统一升级.sql")

# 动态导入同目录的"工具_生成课程数据.py"(文件名含中文,不能直接 import)
spec = importlib.util.spec_from_file_location(
    "gen_course",
    os.path.join(HERE, "工具_生成课程数据.py"),
)
gen = importlib.util.module_from_spec(spec)
sys.modules["gen_course"] = gen
spec.loader.exec_module(gen)


def escape_sql(s):
    return s.replace("\\", "\\\\").replace("'", "\\'")


def main():
    out = []
    out.append("-- ============================================================================")
    out.append("-- 19_课程描述统一升级.sql  (由 工具_生成描述UPDATE.py 生成)")
    out.append("-- v8 描述统一 (2026-05): 把已入库的 1491 条 edu_subject.description")
    out.append("-- 升级为 6 字段培养方案标准格式")
    out.append("--   格式: [课程类型] · [专业简称][年级]第[N]学期 · [学分]学分/[学时]学时 · [核心内容]")
    out.append("--   示例: 专业核心必修 · 计科24级第5学期 · 4学分/72学时 · SSM/Spring 框架")
    out.append("-- ----------------------------------------------------------------------------")
    out.append("-- 策略: UPDATE BY (grade, major_id, subject_name) 三元组")
    out.append("--   - 不改 id, 不动 hours/exam_type, 保护 teacher_subject 关联完整性")
    out.append("--   - 包含两部分:")
    out.append("--       I.  14 条 UPDATE BY id 100-113 (06 老课程, 单独定位避免与 07 同名)")
    out.append("--       II. 1491 条 UPDATE BY 三元组 (07 真实课表数据)")
    out.append("-- ----------------------------------------------------------------------------")
    out.append("-- 幂等: 可反复执行, 描述只会被覆盖为最新一致值")
    out.append("-- ============================================================================")
    out.append("")
    out.append("USE online_exam_system;")
    out.append("SET NAMES utf8mb4;")
    out.append("")
    out.append("START TRANSACTION;")
    out.append("")

    # ============ I. 06 老 14 条 (id 100-113) ============
    out.append("-- ---------- I. 06 老课程 14 条 (id 100-113) ----------")
    old_14 = [
        (100, "通识必修 · 计科24级第1学期 · 5学分/80学时 · 工科基础高数A"),
        (101, "通识必修 · 计科24级第1学期 · 4学分/64学时 · 公共外语全校必修"),
        (102, "通识必修 · 计科24级第2学期 · 3学分/48学时 · 矩阵与方程组"),
        (103, "学科基础必修 · 计科24级第2学期 · 3学分/48学时 · 命题逻辑/集合论/图论/组合数学"),
        (104, "学科基础必修 · 计科24级第3学期 · 4学分/64学时 · 线性表/树/图/排序查找"),
        (105, "学科基础必修 · 计科24级第4学期 · 3学分/52学时 · 进程管理/内存管理/文件系统"),
        (106, "学科基础必修 · 计科24级第4学期 · 4学分/72学时 · CPU/存储/IO/总线"),
        (107, "学科基础必修 · 计科24级第5学期 · 4学分/72学时 · OSI七层/TCP-IP/路由协议"),
        (108, "学科基础必修 · 计科24级第4学期 · 3学分/56学时 · ER模型/SQL/规范化/事务处理"),
        (109, "专业核心必修 · 计科24级第5学期 · 4学分/72学时 · SSM/Spring 框架"),
        (110, "专业核心必修 · 计科24级第5学期 · 3学分/56学时 · 软件过程/需求/设计/测试"),
        (111, "专业核心必修 · 计科24级第6学期 · 2学分/36学时 · 词法/语法/语义分析/代码生成"),
        (112, "专业核心必修 · 计科24级第6学期 · 3学分/56学时 · 搜索/机器学习/神经网络入门"),
        (113, "专业核心必修 · 日语24级第3学期 · 4学分/64学时 · 综合日语精读训练"),
    ]
    for sid, desc in old_14:
        out.append(f"UPDATE edu_subject SET description='{escape_sql(desc)}' WHERE id={sid};")
    out.append("")

    # ============ II. 07 真实课表 1491 条 (三元组定位) ============
    out.append("-- ---------- II. 07 真实课表 1491 条 (BY (grade, major_id, subject_name)) ----------")
    total = 0
    for major_id in sorted(gen.MAJOR_NAMES.keys()):
        major_name = gen.MAJOR_NAMES[major_id]
        short = gen.MAJOR_SHORT[major_id]
        for grade in (2022, 2024):
            out.append(f"-- {grade} 级 · {major_name} (id={major_id})")
            courses = gen.get_all_courses(major_id, grade)
            if major_id == 100 and grade == 2024:
                courses = [c for c in courses if c[0] not in gen.CS_OLD_24_NAMES]
            for (cname, ctype, credit, hours, semester, exam, desc) in courses:
                full_desc = gen.build_description(
                    ctype, short, grade, semester, credit, hours, desc
                )
                sql = (
                    "UPDATE edu_subject SET description='"
                    + escape_sql(full_desc)
                    + f"' WHERE grade='{grade}级' AND major_id={major_id} "
                    + f"AND subject_name='{escape_sql(cname)}';"
                )
                out.append(sql)
                total += 1
            out.append("")

    out.append("COMMIT;")
    out.append("")
    out.append("-- ============================================================================")
    out.append("-- 验证: 抽样查看升级后的描述")
    out.append("-- ============================================================================")
    out.append(
        "SELECT id, grade, major_id, subject_name, description "
        "FROM edu_subject WHERE id IN (100, 109, 113) "
        "OR (grade='2022级' AND major_id=101 AND subject_name='电路分析') "
        "OR (grade='2024级' AND major_id=104 AND subject_name='自动控制原理') "
        "LIMIT 20;"
    )
    out.append("")
    out.append(f"-- 预计 UPDATE 行数: 14 (老) + {total} (真实) = {14 + total}")

    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(out))
    print(f"[OK] SQL written to: {OUT}")
    print(f"[OK] Total UPDATE statements: 14 + {total} = {14 + total}")


if __name__ == "__main__":
    main()
