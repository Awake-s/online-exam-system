package com.exam.perf.support;

import net.datafaker.Faker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Datafaker 中文数据生成工具类。
 *
 * <p>包装 {@link net.datafaker.Faker}，针对在线考试系统业务场景
 * （姓名、手机号、学号、教师号、试卷名等）提供语义化方法。
 *
 * <p>使用 {@code Locale.SIMPLIFIED_CHINESE} 生成符合中国用户习惯的数据。
 *
 * @author 性能压测工具链 v2.0
 */
public class ChineseFakerHelper {

    private final Faker faker;
    private final Random random;

    public ChineseFakerHelper() {
        this(System.currentTimeMillis());
    }

    public ChineseFakerHelper(long seed) {
        this.random = new Random(seed);
        this.faker = new Faker(Locale.SIMPLIFIED_CHINESE, this.random);
    }

    // ========== 用户基础信息 ==========

    /** 生成中文姓名（如：张三、李晓明） */
    public String chineseName() {
        return faker.name().fullName();
    }

    /** 生成中国手机号（13/14/15/17/18/19 段） */
    public String chinesePhone() {
        String[] prefixes = {"130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
                "150", "151", "152", "153", "155", "156", "157", "158", "159",
                "170", "171", "172", "173", "175", "176", "177", "178",
                "180", "181", "182", "183", "184", "185", "186", "187", "188", "189",
                "199"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        int suffix = 10000000 + random.nextInt(90000000);
        return prefix + suffix;
    }

    /** 生成邮箱（学号@学校域名） */
    public String studentEmail(String studentNo) {
        return studentNo + "@stu.usts.edu.cn";
    }

    /** 教师邮箱 */
    public String teacherEmail(String workNo) {
        return workNo + "@usts.edu.cn";
    }

    // ========== 性别 ==========

    /** 0=男 / 1=女（与项目 sys_user.gender 字段一致） */
    public Integer randomGender() {
        return random.nextInt(2);
    }

    // ========== 题目内容生成 ==========

    /** 生成中文题目内容（句子 + 几个关键概念） */
    public String questionContent(String subjectName, int qtype) {
        String[] templates;
        switch (qtype) {
            case 1:
                templates = new String[]{
                        "在《%s》课程中，关于以下哪个概念的描述是正确的？",
                        "%s 课程中下列哪一项属于核心知识点？",
                        "%s 课程关于此概念的定义中，正确的是？"
                };
                break;
            case 2:
                templates = new String[]{
                        "在《%s》课程中，下列哪些选项是正确的？（可多选）",
                        "%s 课程中以下哪些属于该体系的组成部分？（多选）"
                };
                break;
            case 3:
                templates = new String[]{
                        "判断题（%s 课程）：该理论的核心思想是被广泛接受的标准解释。",
                        "判断题（%s）：以下表述符合该课程的基本原理。"
                };
                break;
            case 4:
                templates = new String[]{
                        "填空题（%s）：下列定义中的关键术语是 _____ 。",
                        "填空（%s 课程）：该方法被简称为 _____ 模式。"
                };
                break;
            case 5:
                templates = new String[]{
                        "简答题：请简述《%s》课程中此专题的基本原理与典型应用。",
                        "简答（%s）：结合实例，分析该理论的优缺点。"
                };
                break;
            default:
                templates = new String[]{"题目内容（%s）"};
        }
        String tpl = templates[random.nextInt(templates.length)];
        return String.format(tpl, subjectName);
    }

    /** 生成题目解析（可选） */
    public String analysis() {
        String[] templates = {
                "本题考查对核心概念的理解，参考教材第 X 章。",
                "解析：本题为基础题，重点理解定义即可。",
                "解析：注意区分该术语与相近术语的差别，避免混淆。",
                "解析：结合上下文判断，正确选项符合该原理的标准定义。"
        };
        return templates[random.nextInt(templates.length)];
    }

    // ========== 聊天消息内容 ==========

    /** 生成教学场景常用聊天消息 */
    public String chatMessage() {
        String[] messages = {
                "老师，作业截止时间是什么时候？",
                "请问明天的考试需要带什么资料？",
                "同学你好，关于上次实验的问题想请教一下。",
                "已收到，谢谢老师！",
                "好的，我会准时参加。",
                "老师，这个知识点我还没完全理解，可以再讲一遍吗？",
                "可以的，明天上课时再详细讲解。",
                "请问期末考试的复习范围包括哪些章节？",
                "复习提纲已经上传到课程网站，请查看。",
                "明白了，非常感谢！",
                "请问这次作业的提交方式是什么？",
                "通过课程平台上传 PDF 文件即可。",
                "老师，关于第三章的练习题我有些问题。",
                "你可以把具体问题发给我，我有空回复你。",
                "好的老师，麻烦您了。",
                "同学们，下周一记得带教材来上课。",
                "收到，老师辛苦了。",
                "请问下次小组讨论是什么时候？",
                "时间地点已经在群通知里了，请查看。",
                "明天答辩需要准备 PPT 吗？"
        };
        return messages[random.nextInt(messages.length)];
    }

    /** 生成通知标题 */
    public String notificationTitle(String type) {
        switch (type) {
            case "EXAM_PUBLISHED":   return "新考试已发布";
            case "EXAM_REMINDER":    return "考试提醒";
            case "SCORE_PUBLISHED":  return "成绩已发布";
            case "GRADING_FINISHED": return "阅卷已完成";
            default:                  return "系统通知";
        }
    }

    /** 生成通知内容 */
    public String notificationContent(String type) {
        switch (type) {
            case "EXAM_PUBLISHED":   return "您有一场新考试即将开始，请准时参加。";
            case "EXAM_REMINDER":    return "考试将在 30 分钟后开始，请提前进入考场。";
            case "SCORE_PUBLISHED":  return "您的考试成绩已发布，请登录系统查看。";
            case "GRADING_FINISHED": return "阅卷工作已完成，等待管理员审核后发布。";
            default:                  return "您有一条新的系统通知。";
        }
    }

    // ========== 数值类 ==========

    public BigDecimal questionScore(int qtype) {
        // 根据题型决定典型分值
        switch (qtype) {
            case QType.SINGLE:
            case QType.JUDGE: return new BigDecimal("2.0");
            case QType.MULTI: return new BigDecimal("3.0");
            case QType.BLANK: return new BigDecimal("4.0");
            case QType.ESSAY: return new BigDecimal("10.0");
            default:          return new BigDecimal("2.0");
        }
    }

    public BigDecimal randomScore(double min, double max) {
        double v = ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    // ========== 通用 ==========

    public Random getRandom() {
        return random;
    }

    public Faker getFaker() {
        return faker;
    }

    public int randomInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    public <T> T pickRandom(java.util.List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    /** 生成不重复的随机索引（用于抽题） */
    public int[] uniqueIndices(int total, int n) {
        if (n > total) throw new IllegalArgumentException("n>total");
        int[] arr = new int[total];
        for (int i = 0; i < total; i++) arr[i] = i;
        // Fisher-Yates shuffle
        for (int i = total - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = arr[i]; arr[i] = arr[j]; arr[j] = t;
        }
        int[] out = new int[n];
        System.arraycopy(arr, 0, out, 0, n);
        return out;
    }

    /** 题型常量内部类 */
    public static final class QType {
        public static final int SINGLE = 1;
        public static final int MULTI = 2;
        public static final int JUDGE = 3;
        public static final int BLANK = 4;
        public static final int ESSAY = 5;
    }
}
