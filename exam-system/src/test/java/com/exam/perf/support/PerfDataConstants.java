package com.exam.perf.support;

/**
 * 性能压测造数据常量定义。
 *
 * <p>对齐 v2.0 实施方案 §4.2 的「推荐版」规模设计，所有数量级与「高校小班制（30~50人）」定位严格匹配。
 * 同时支持通过 -D 参数切换为「极简版」用于快速冒烟测试。
 *
 * <pre>
 * 启用极简版：mvn test -Dtest=DataGeneratorTest -Dperf.scale=lite
 * 启用推荐版：mvn test -Dtest=DataGeneratorTest -Dperf.scale=full（默认）
 * </pre>
 */
public final class PerfDataConstants {

    private PerfDataConstants() {}

    // ========== 角色 ID（与 sys_role 表数据一致） ==========
    public static final Long ROLE_ID_ADMIN = 1L;
    public static final Long ROLE_ID_TEACHER = 2L;
    public static final Long ROLE_ID_STUDENT = 3L;

    // ========== 用户名前缀（用于一键清理识别） ==========
    public static final String PREFIX_ADMIN = "perf_admin_";
    public static final String PREFIX_TEACHER = "perf_tea_";
    public static final String PREFIX_STUDENT = "perf_stu_";
    public static final String PREFIX_ALL_USERS = "perf_";

    // ========== 默认密码（明文，由 Service 层 BCrypt 加密） ==========
    public static final String DEFAULT_PASSWORD = "Test@123456";

    // ========== 数据规模（推荐版） ==========
    private static final boolean LITE = "lite".equalsIgnoreCase(System.getProperty("perf.scale", "full"));

    /** 管理员数量（含已有 1 个 + 新增 1 个 = 2，仅新增 1 个） */
    public static final int COUNT_ADMINS_TO_ADD = LITE ? 1 : 1;

    /** 教师数量（推荐 15 / 极简 5） */
    public static final int COUNT_TEACHERS = LITE ? 5 : 15;

    /** 学生数量（推荐 300 / 极简 100） */
    public static final int COUNT_STUDENTS = LITE ? 100 : 300;

    /** 班级数量（推荐 8 / 极简 3，已有 6 / 已有 3） */
    public static final int COUNT_CLASSES_TARGET = LITE ? 3 : 8;

    /** 科目数量（推荐 15 / 极简 8，已有 8 / 已有 8） */
    public static final int COUNT_SUBJECTS_TARGET = LITE ? 8 : 15;

    /** 专业数量（推荐 3 / 极简 3，已有 3） */
    public static final int COUNT_MAJORS_TARGET = 3;

    /** 题库数量（推荐 1500 / 极简 500，已有 179） */
    public static final int COUNT_QUESTIONS_TARGET = LITE ? 500 : 1500;

    /** 试卷数量（推荐 30 / 极简 10） */
    public static final int COUNT_PAPERS = LITE ? 10 : 30;

    /** 每张试卷题目数 */
    public static final int QUESTIONS_PER_PAPER = 25;

    /** 已发布考试数（推荐 20 / 极简 8） */
    public static final int COUNT_EXAMS = LITE ? 8 : 20;

    /** 平均每场考试参与学生数 */
    public static final int STUDENTS_PER_EXAM = 75;

    /** 聊天会话数（推荐 200 / 极简 50） */
    public static final int COUNT_CONVERSATIONS = LITE ? 50 : 200;

    /** 平均每个会话消息数 */
    public static final int MESSAGES_PER_CONVERSATION = 25;

    /** 通知数（推荐 1000 / 极简 200） */
    public static final int COUNT_NOTIFICATIONS = LITE ? 200 : 1000;

    // ========== 题型常量 ==========
    public static final int QTYPE_SINGLE = 1;       // 单选
    public static final int QTYPE_MULTI = 2;        // 多选
    public static final int QTYPE_JUDGE = 3;        // 判断
    public static final int QTYPE_BLANK = 4;        // 填空
    public static final int QTYPE_ESSAY = 5;        // 简答

    // ========== 难度常量 ==========
    public static final int DIFF_EASY = 1;
    public static final int DIFF_MEDIUM = 2;
    public static final int DIFF_HARD = 3;

    /** 当前规模标识（用于日志输出） */
    public static String getScaleLabel() {
        return LITE ? "极简版(lite)" : "推荐版(full)";
    }
}
