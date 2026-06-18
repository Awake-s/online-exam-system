package com.exam.perf;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.dto.request.*;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.exam.perf.support.PerfDataConstants.*;

/**
 * 在线考试系统 - 性能压测数据生成器（v2.0）。
 *
 * <p>14 个 @Order 严格顺序的 test step 完成全量造数据：
 * <ol>
 *   <li>step01_majors      → 补足专业到 3 个</li>
 *   <li>step02_subjects    → 补足科目到 15 个 + subject_major 关联</li>
 *   <li>step03_classes     → 补足班级到 8 个</li>
 *   <li>step04_admins      → 新增 1 个测试管理员</li>
 *   <li>step05_teachers    → 新增 15 个教师 + teacher_class + teacher_subject</li>
 *   <li>step06_students    → 新增 300 个学生</li>
 *   <li>step07_questions   → 补足题库到 1500</li>
 *   <li>step08_templates   → 创建 15 个组卷模板 + 75 条规则</li>
 *   <li>step09_papers      → 创建 30 张试卷 + paper_question 关联</li>
 *   <li>step10_exams       → 创建 20 场考试</li>
 *   <li>step11_records     → 创建 1500 条考试记录（含多种状态）</li>
 *   <li>step12_answers     → 创建 ~28000 条答题记录</li>
 *   <li>step13_chats       → 创建 200 个会话 + 5000 条消息</li>
 *   <li>step14_notifies    → 创建 1000 条通知</li>
 * </ol>
 *
 * <p>所有用户名加 {@code perf_} 前缀，保证可被一键清理（DataCleanupTest）。
 *
 * <p>启动命令：
 * <pre>
 * cd exam-system
 * mvn test -Dtest=DataGeneratorTest -Dspring.profiles.active=perf
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataGeneratorTest extends BasePerfTest {

    // ========== Service 注入（造基础数据，走业务校验） ==========
    @Autowired private MajorService majorService;
    @Autowired private SubjectService subjectService;
    @Autowired private ClassService classService;
    @Autowired private UserService userService;
    @Autowired private QuestionService questionService;
    @Autowired private PaperService paperService;

    // ========== Mapper 注入（造流水数据，避免副作用） ==========
    @Autowired private SysUserMapper userMapper;
    @Autowired private EduMajorMapper majorMapper;
    @Autowired private EduSubjectMapper subjectMapper;
    @Autowired private EduClassMapper classMapper;
    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private ExamPaperMapper paperMapper;
    @Autowired private ExamPaperQuestionMapper paperQuestionMapper;
    @Autowired private ExamPaperTemplateMapper templateMapper;
    @Autowired private ExamTemplateRuleMapper ruleMapper;
    @Autowired private ExamExamMapper examMapper;
    @Autowired private ExamRecordMapper recordMapper;
    @Autowired private ExamAnswerMapper answerMapper;
    @Autowired private ChatConversationMapper conversationMapper;
    @Autowired private ChatMessageMapper messageMapper;
    @Autowired private SysNotificationMapper notificationMapper;
    @Autowired private TeacherClassMapper teacherClassMapper;
    @Autowired private TeacherSubjectMapper teacherSubjectMapper;
    @Autowired private SubjectMajorMapper subjectMajorMapper;

    // ========== Step 间共享缓存（避免重复查询） ==========
    private List<Long> majorIds;
    private List<Long> subjectIds;
    private List<Long> classIds;
    private Long adminPerfId;
    private List<Long> teacherIds;
    private List<Long> studentIds;
    private List<Long> questionIds;
    private List<Long> paperIds;
    private List<Long> examIds;
    private List<Long> recordIds;
    private Map<Long, Integer> recordStatusMap;     // recordId -> status
    private Map<Long, Long> recordPaperMap;         // recordId -> paperId
    private Map<Long, Long> recordUserMap;          // recordId -> userId

    // ====================================================================
    // STEP 01: 补足专业到目标数量
    // ====================================================================
    @Test
    @Order(1)
    void step01_majors() {
        long t = stepBegin("step01_majors");
        long current = majorMapper.selectCount(null);
        log("当前 edu_major 行数：" + current);

        int toAdd = (int) Math.max(0, COUNT_MAJORS_TARGET - current);
        for (int i = 0; i < toAdd; i++) {
            MajorAddRequest req = new MajorAddRequest();
            req.setMajorName("性能测试专业_" + (i + 1));
            req.setDescription("v2.0 压测自动生成");
            majorService.addMajor(req);
        }

        majorIds = majorMapper.selectList(null).stream()
                .map(EduMajor::getId).collect(Collectors.toList());
        stepEnd("step01_majors", t, toAdd);
        log("📋 majorIds = " + majorIds);
    }

    // ====================================================================
    // STEP 02: 补足科目 + 关联到专业
    // ====================================================================
    @Test
    @Order(2)
    void step02_subjects() {
        long t = stepBegin("step02_subjects");
        long current = subjectMapper.selectCount(null);
        log("当前 edu_subject 行数：" + current);

        String[] subjectNamesPool = {
                "Java程序设计", "数据库原理", "操作系统", "计算机网络", "软件工程",
                "数据结构", "算法分析", "计算机组成原理", "Web开发技术", "Python编程",
                "机器学习导论", "前端开发实战", "云计算基础", "网络安全", "分布式系统",
                "Spring Boot 实战", "MySQL 数据库", "Redis 缓存原理", "大数据技术", "人工智能基础"
        };
        int toAdd = (int) Math.max(0, COUNT_SUBJECTS_TARGET - current);
        for (int i = 0; i < toAdd; i++) {
            SubjectAddRequest req = new SubjectAddRequest();
            String name = "性能_" + subjectNamesPool[i % subjectNamesPool.length] + "_" + (i + 1);
            req.setSubjectName(name);
            req.setDescription("v2.0 压测自动生成");
            // 随机关联到 1~2 个专业
            List<Long> majors = new ArrayList<>(majorIds);
            Collections.shuffle(majors, faker.getRandom());
            req.setMajorIds(majors.subList(0, Math.min(2, majors.size())));
            subjectService.addSubject(req);
        }

        subjectIds = subjectMapper.selectList(null).stream()
                .map(EduSubject::getId).collect(Collectors.toList());
        stepEnd("step02_subjects", t, toAdd);
        log("📋 subjectIds size = " + subjectIds.size());
    }

    // ====================================================================
    // STEP 03: 补足班级
    // ====================================================================
    @Test
    @Order(3)
    void step03_classes() {
        long t = stepBegin("step03_classes");
        long current = classMapper.selectCount(null);
        log("当前 edu_class 行数：" + current);

        String[] majorAbbrevs = {"计科", "软工", "网工"};
        int toAdd = (int) Math.max(0, COUNT_CLASSES_TARGET - current);
        for (int i = 0; i < toAdd; i++) {
            ClassAddRequest req = new ClassAddRequest();
            String major = majorAbbrevs[i % majorAbbrevs.length];
            int year = 21 + (i % 4);
            int classNo = (i % 2) + 1;
            req.setClassName(major + year + "-" + classNo + "（压测）");
            req.setGrade("20" + year);
            req.setMajorId(faker.pickRandom(majorIds));
            req.setDescription("v2.0 压测自动生成");
            classService.addClass(req);
        }

        classIds = classMapper.selectList(null).stream()
                .map(EduClass::getId).collect(Collectors.toList());
        stepEnd("step03_classes", t, toAdd);
        log("📋 classIds = " + classIds);
    }

    // ====================================================================
    // STEP 04: 新增测试管理员
    // ====================================================================
    @Test
    @Order(4)
    void step04_admins() {
        long t = stepBegin("step04_admins");
        for (int i = 1; i <= COUNT_ADMINS_TO_ADD; i++) {
            String username = PREFIX_ADMIN + String.format("%02d", i);
            if (existsUser(username)) {
                log("跳过已存在用户：" + username);
                continue;
            }
            UserAddRequest req = new UserAddRequest();
            req.setUsername(username);
            req.setPassword(DEFAULT_PASSWORD);
            req.setRealName("测试管理员_" + i);
            req.setRoleId(ROLE_ID_ADMIN);
            req.setGender(faker.randomGender());
            req.setEmail(username + "@admin.usts.edu.cn");
            req.setPhone(faker.chinesePhone());
            req.setStatus(1);
            userService.addUser(req);
        }
        adminPerfId = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, PREFIX_ADMIN + "01")).getId();
        stepEnd("step04_admins", t, COUNT_ADMINS_TO_ADD);
        log("📋 adminPerfId = " + adminPerfId);
    }

    // ====================================================================
    // STEP 05: 新增 15 个教师 + 关联班级 + 关联科目
    // ====================================================================
    @Test
    @Order(5)
    void step05_teachers() {
        long t = stepBegin("step05_teachers");
        // 确保 classIds / subjectIds 已加载（如果是单独跑此 step）
        ensureBaseLoaded();

        for (int i = 1; i <= COUNT_TEACHERS; i++) {
            String username = PREFIX_TEACHER + String.format("%03d", i);
            if (existsUser(username)) continue;

            UserAddRequest req = new UserAddRequest();
            req.setUsername(username);
            req.setPassword(DEFAULT_PASSWORD);
            req.setRealName(faker.chineseName() + "老师");
            req.setRoleId(ROLE_ID_TEACHER);
            req.setGender(faker.randomGender());
            req.setEmail(faker.teacherEmail(username));
            req.setPhone(faker.chinesePhone());
            req.setStatus(1);

            // 教师覆盖 1~2 个班级
            List<Long> tcs = new ArrayList<>(classIds);
            Collections.shuffle(tcs, faker.getRandom());
            req.setClassIds(tcs.subList(0, Math.min(2, tcs.size())));

            // 教师任教 2 门科目
            List<Long> tss = new ArrayList<>(subjectIds);
            Collections.shuffle(tss, faker.getRandom());
            req.setSubjectIds(tss.subList(0, Math.min(2, tss.size())));

            userService.addUser(req);
        }

        teacherIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_TEACHER))
                .stream().map(SysUser::getId).collect(Collectors.toList());
        stepEnd("step05_teachers", t, COUNT_TEACHERS);
        log("📋 teacherIds size = " + teacherIds.size());
    }

    // ====================================================================
    // STEP 06: 新增 300 个学生
    // ====================================================================
    @Test
    @Order(6)
    void step06_students() {
        long t = stepBegin("step06_students");
        ensureBaseLoaded();

        for (int i = 1; i <= COUNT_STUDENTS; i++) {
            String username = PREFIX_STUDENT + String.format("%03d", i);
            if (existsUser(username)) continue;

            UserAddRequest req = new UserAddRequest();
            req.setUsername(username);
            req.setPassword(DEFAULT_PASSWORD);
            req.setRealName(faker.chineseName());
            req.setRoleId(ROLE_ID_STUDENT);
            req.setClassId(faker.pickRandom(classIds));
            req.setGender(faker.randomGender());
            req.setEmail(faker.studentEmail(username));
            req.setPhone(faker.chinesePhone());
            req.setStatus(1);
            userService.addUser(req);

            if (i % 50 == 0) log("  已创建 " + i + " 个学生");
        }

        studentIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_STUDENT))
                .stream().map(SysUser::getId).collect(Collectors.toList());
        stepEnd("step06_students", t, COUNT_STUDENTS);
        log("📋 studentIds size = " + studentIds.size());
    }

    // ====================================================================
    // STEP 07: 补足题库到 1500（5 种题型按 4:2:2:1:1 分布）
    // ====================================================================
    @Test
    @Order(7)
    void step07_questions() {
        long t = stepBegin("step07_questions");
        ensureBaseLoaded();
        ensureTeacherLoaded();

        long current = questionMapper.selectCount(null);
        log("当前 exam_question 行数（含已存在的 179）：" + current);
        int toAdd = (int) Math.max(0, COUNT_QUESTIONS_TARGET - current);

        // 题型分布：单选 40%、多选 20%、判断 20%、填空 10%、简答 10%
        int[] typeWeights = {0, 40, 60, 80, 90, 100};

        for (int i = 0; i < toAdd; i++) {
            int r = faker.randomInt(1, 100);
            int qtype = 1;
            for (int k = 1; k <= 5; k++) {
                if (r > typeWeights[k - 1] && r <= typeWeights[k]) { qtype = k; break; }
            }

            Long subjectId = faker.pickRandom(subjectIds);
            // 通过 mapper 查 subject 名（仅取一次效率不高，但 step07 每条独立，可接受）
            String subjectName = "课程"; // 简化处理，避免重复 SQL
            try {
                EduSubject sub = subjectMapper.selectById(subjectId);
                if (sub != null) subjectName = sub.getSubjectName();
            } catch (Exception ignore) {}

            QuestionAddRequest req = new QuestionAddRequest();
            req.setSubjectId(subjectId);
            req.setQuestionType(qtype);
            req.setContent(faker.questionContent(subjectName, qtype));
            req.setOptions(buildOptions(qtype));
            req.setAnswer(buildAnswer(qtype));
            req.setAnalysis(faker.analysis());
            req.setScore(faker.questionScore(qtype));
            req.setDifficulty(faker.randomInt(1, 3));

            // 由教师创建（随机分配给一个 perf 教师）
            Long creatorId = faker.pickRandom(teacherIds);
            questionService.addQuestion(req, creatorId);

            if ((i + 1) % 200 == 0) log("  已创建题目 " + (i + 1) + "/" + toAdd);
        }

        questionIds = questionMapper.selectList(null).stream()
                .map(ExamQuestion::getId).collect(Collectors.toList());
        stepEnd("step07_questions", t, toAdd);
        log("📋 questionIds total = " + questionIds.size());
    }

    /** 根据题型生成 options（List<String>） */
    private List<String> buildOptions(int qtype) {
        switch (qtype) {
            case 1: case 2:
                return Arrays.asList("选项A 概念定义说明", "选项B 概念定义说明", "选项C 概念定义说明", "选项D 概念定义说明");
            case 3:
                return Arrays.asList("正确", "错误");
            case 4: case 5:
            default:
                return null;
        }
    }

    /** 根据题型生成 answer（字符串） */
    private String buildAnswer(int qtype) {
        switch (qtype) {
            case 1:  return "A";
            case 2:  return "AB";
            case 3:  return "正确";
            case 4:  return "标准答案关键词";
            case 5:  return "（参考要点）核心概念 + 应用场景 + 优缺点。";
            default: return "A";
        }
    }

    // ====================================================================
    // STEP 08: 创建 15 个组卷模板（直接 Mapper，无 Service 接口需要批量）
    // ====================================================================
    @Test
    @Order(8)
    void step08_templates() {
        long t = stepBegin("step08_templates");
        ensureBaseLoaded();
        ensureTeacherLoaded();

        int templates = 0, rules = 0;
        for (int i = 0; i < COUNT_SUBJECTS_TARGET; i++) {
            Long subjectId = subjectIds.get(i % subjectIds.size());
            ExamPaperTemplate tpl = new ExamPaperTemplate();
            tpl.setTemplateName("性能压测模板_" + (i + 1));
            tpl.setSubjectId(subjectId);
            tpl.setTargetScore(new BigDecimal("100"));
            tpl.setPassScore(new BigDecimal("60"));
            tpl.setDuration(90);
            tpl.setDescription("v2.0 压测自动生成");
            tpl.setCreatorId(faker.pickRandom(teacherIds));
            templateMapper.insert(tpl);
            templates++;

            // 每模板 5 条规则（5 题型）
            int[][] cfg = {
                    // {qtype, count, score, difficulty}
                    {1, 10, 2,  2},
                    {2, 5,  3,  2},
                    {3, 10, 2,  1},
                    {4, 5,  4,  2},
                    {5, 3,  10, 3}
            };
            for (int k = 0; k < cfg.length; k++) {
                ExamTemplateRule r = new ExamTemplateRule();
                r.setTemplateId(tpl.getId());
                r.setQuestionType(cfg[k][0]);
                r.setQuestionCount(cfg[k][1]);
                r.setScorePerQuestion(new BigDecimal(cfg[k][2]));
                r.setDifficulty(cfg[k][3]);
                r.setSortOrder(k);
                ruleMapper.insert(r);
                rules++;
            }
        }
        stepEnd("step08_templates", t, templates + rules);
    }

    // ====================================================================
    // STEP 09: 创建 30 张试卷（走 PaperService.createPaper 保证 paper_question 关联完整）
    // ====================================================================
    @Test
    @Order(9)
    void step09_papers() {
        long t = stepBegin("step09_papers");
        ensureBaseLoaded();
        ensureTeacherLoaded();
        ensureQuestionLoaded();

        // 按科目分组题目，便于组卷时挑同科目题
        Map<Long, List<Long>> subjectQuestions = new HashMap<>();
        for (ExamQuestion q : questionMapper.selectList(null)) {
            subjectQuestions.computeIfAbsent(q.getSubjectId(), k -> new ArrayList<>()).add(q.getId());
        }

        int created = 0;
        for (int i = 1; i <= COUNT_PAPERS; i++) {
            Long subjectId = subjectIds.get(i % subjectIds.size());
            List<Long> pool = subjectQuestions.getOrDefault(subjectId, Collections.emptyList());
            if (pool.size() < QUESTIONS_PER_PAPER) {
                // 科目题目不够，跳过本次（小概率）
                log("⚠ 科目 " + subjectId + " 题目数 " + pool.size() + " 不足 " + QUESTIONS_PER_PAPER + "，跳过");
                continue;
            }

            // 抽 25 题
            List<Long> shuffled = new ArrayList<>(pool);
            Collections.shuffle(shuffled, faker.getRandom());
            List<Long> chosen = shuffled.subList(0, QUESTIONS_PER_PAPER);

            PaperCreateRequest req = new PaperCreateRequest();
            req.setPaperName("性能压测试卷_" + i);
            req.setSubjectId(subjectId);
            req.setPassScore(new BigDecimal("60"));
            req.setDuration(90);
            List<PaperCreateRequest.PaperQuestionItem> items = new ArrayList<>();
            int sortIdx = 0;
            for (Long qid : chosen) {
                ExamQuestion q = questionMapper.selectById(qid);
                if (q == null) continue;
                PaperCreateRequest.PaperQuestionItem item = new PaperCreateRequest.PaperQuestionItem();
                item.setQuestionId(qid);
                item.setScore(q.getScore() != null ? q.getScore() : new BigDecimal("4"));
                item.setSortOrder(sortIdx++);
                items.add(item);
            }
            req.setQuestions(items);

            paperService.createPaper(req, faker.pickRandom(teacherIds));
            created++;
        }

        paperIds = paperMapper.selectList(null).stream()
                .map(ExamPaper::getId).collect(Collectors.toList());
        stepEnd("step09_papers", t, created);
        log("📋 paperIds size = " + paperIds.size());
    }

    // ====================================================================
    // STEP 10: 创建 20 场考试（直接 Mapper，避免 Service 触发批量通知）
    // ====================================================================
    @Test
    @Order(10)
    void step10_exams() {
        long t = stepBegin("step10_exams");
        ensureBaseLoaded();
        ensurePaperLoaded();
        ensureTeacherLoaded();

        // 状态分布：历史已结束 10 / 进行中 5 / 待开始 5
        int[][] status = {
                {0, 10},  // 0=已结束（往前 30~7 天）
                {1, 5},   // 1=进行中
                {2, 5}    // 2=待开始（往后 1~14 天）
        };
        // 注：实际项目 status 字段语义在不同版本可能不同，这里按"通用"语义；
        //     若与项目 ExamServiceImpl 内部 buildStatus() 不一致，压测端不影响（status 仅作展示用，
        //     真正的"考试时间窗"由 startTime/endTime 决定）

        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        int idx = 0;
        for (int[] s : status) {
            for (int i = 0; i < s[1]; i++) {
                ExamExam e = new ExamExam();
                e.setExamName("性能压测考试_" + (++idx));
                e.setPaperId(faker.pickRandom(paperIds));
                e.setClassId(faker.pickRandom(classIds));
                e.setCreatorId(faker.pickRandom(teacherIds));
                e.setStatus(s[0]);
                e.setScorePublished(s[0] == 0 && faker.randomInt(1, 10) > 5 ? 1 : 0);
                if (s[0] == 0) {
                    // 已结束：30 天前 ~ 7 天前
                    int daysAgo = faker.randomInt(7, 30);
                    e.setStartTime(now.minusDays(daysAgo).minusHours(1));
                    e.setEndTime(now.minusDays(daysAgo));
                    if (e.getScorePublished() == 1) {
                        e.setLastPublishTime(now.minusDays(daysAgo).plusHours(2));
                    }
                } else if (s[0] == 1) {
                    // 进行中：开始时间已过，结束时间还在
                    e.setStartTime(now.minusMinutes(faker.randomInt(10, 60)));
                    e.setEndTime(now.plusMinutes(faker.randomInt(30, 120)));
                } else {
                    // 待开始
                    int daysLater = faker.randomInt(1, 14);
                    e.setStartTime(now.plusDays(daysLater));
                    e.setEndTime(now.plusDays(daysLater).plusHours(2));
                }
                // 防作弊配置 JSON
                e.setAntiCheatConfig("{\"switchScreenMax\":3,\"shuffleQuestion\":true,\"shuffleOption\":true,\"fullscreenRequired\":true,\"noCopyPaste\":true}");
                e.setCreateTime(now);
                examMapper.insert(e);
                created++;
            }
        }

        examIds = examMapper.selectList(null).stream()
                .map(ExamExam::getId).collect(Collectors.toList());
        stepEnd("step10_exams", t, created);
        log("📋 examIds size = " + examIds.size());
    }

    // ====================================================================
    // STEP 11: 创建 1500 条考试记录
    //   状态分布：未开始 200 / 答题中 200 / 已交卷 700 / 已批改 300 / 缺考 100
    // ====================================================================
    @Test
    @Order(11)
    void step11_records() {
        long t = stepBegin("step11_records");
        ensureExamLoaded();
        ensureStudentLoaded();

        Map<Long, Long> examPaperMap = new HashMap<>();
        for (ExamExam e : examMapper.selectList(null)) {
            examPaperMap.put(e.getId(), e.getPaperId());
        }

        recordIds = new ArrayList<>();
        recordStatusMap = new HashMap<>();
        recordPaperMap = new HashMap<>();
        recordUserMap = new HashMap<>();

        // 每场考试约 75 条 record
        int created = 0;
        for (Long examId : examIds) {
            // 从学生池中随机抽 STUDENTS_PER_EXAM 名学生
            List<Long> students = new ArrayList<>(studentIds);
            Collections.shuffle(students, faker.getRandom());
            int n = Math.min(STUDENTS_PER_EXAM, students.size());
            for (int i = 0; i < n; i++) {
                Long userId = students.get(i);
                Long paperId = examPaperMap.get(examId);

                ExamRecord r = new ExamRecord();
                r.setExamId(examId);
                r.setUserId(userId);
                r.setPaperId(paperId);

                // 状态分布
                int rnd = faker.randomInt(1, 100);
                int status;
                if (rnd <= 13)      status = 0;   // 未开始
                else if (rnd <= 26) status = 1;   // 答题中
                else if (rnd <= 73) status = 2;   // 已交卷
                else if (rnd <= 93) status = 3;   // 已批改
                else                status = 4;   // 缺考
                r.setStatus(status);

                LocalDateTime now = LocalDateTime.now();
                if (status >= 1) r.setStartTime(now.minusHours(faker.randomInt(1, 48)));
                if (status >= 2) r.setSubmitTime(r.getStartTime().plusMinutes(faker.randomInt(30, 90)));
                if (status >= 2) {
                    r.setObjectiveScore(faker.randomScore(20, 70));
                }
                if (status == 3) {
                    r.setSubjectiveScore(faker.randomScore(0, 30));
                    r.setTotalScore(r.getObjectiveScore().add(r.getSubjectiveScore()));
                }
                r.setSwitchCount(faker.randomInt(0, 3));
                r.setCreateTime(r.getStartTime() != null ? r.getStartTime() : now);

                recordMapper.insert(r);
                recordIds.add(r.getId());
                recordStatusMap.put(r.getId(), status);
                recordPaperMap.put(r.getId(), paperId);
                recordUserMap.put(r.getId(), userId);
                created++;
            }
        }
        stepEnd("step11_records", t, created);
    }

    // ====================================================================
    // STEP 12: 创建答题记录（仅 status>=1 的 record 才有答案）
    // ====================================================================
    @Test
    @Order(12)
    void step12_answers() {
        long t = stepBegin("step12_answers");
        ensureRecordLoaded();

        // 【性能优化】一次性把所有 question 查出来缓存到 Map（避免每条 answer 都 selectById）
        Map<Long, ExamQuestion> questionCache = new HashMap<>();
        for (ExamQuestion q : questionMapper.selectList(null)) {
            questionCache.put(q.getId(), q);
        }
        log("  题目缓存预加载完成，共 " + questionCache.size() + " 题");

        // 缓存每张试卷的题目列表
        Map<Long, List<ExamPaperQuestion>> paperQuestionsMap = new HashMap<>();
        for (ExamPaperQuestion pq : paperQuestionMapper.selectList(null)) {
            paperQuestionsMap.computeIfAbsent(pq.getPaperId(), k -> new ArrayList<>()).add(pq);
        }
        log("  试卷-题目映射预加载完成，共 " + paperQuestionsMap.size() + " 试卷");

        int created = 0;
        for (Long recordId : recordIds) {
            int status = recordStatusMap.get(recordId);
            if (status == 0 || status == 4) continue;  // 未开始/缺考无答题

            Long paperId = recordPaperMap.get(recordId);
            List<ExamPaperQuestion> pqs = paperQuestionsMap.get(paperId);
            if (pqs == null || pqs.isEmpty()) continue;

            // status=1 答题中：随机做了 30%~80% 的题
            // status=2/3 已交卷/已批改：100% 都做完
            int answeredCount;
            if (status == 1) {
                answeredCount = (int) Math.ceil(pqs.size() * (faker.randomInt(30, 80) / 100.0));
            } else {
                answeredCount = pqs.size();
            }

            LocalDateTime answerTime = LocalDateTime.now().minusMinutes(faker.randomInt(10, 120));
            for (int i = 0; i < answeredCount; i++) {
                ExamPaperQuestion pq = pqs.get(i);
                ExamAnswer a = new ExamAnswer();
                a.setRecordId(recordId);
                a.setQuestionId(pq.getQuestionId());

                ExamQuestion q = questionCache.get(pq.getQuestionId());
                int qtype = q == null ? 1 : q.getQuestionType();

                // 模拟学生答题：70% 答对，30% 答错
                boolean correct = faker.randomInt(1, 100) <= 70;
                a.setAnswer(simulateStudentAnswer(qtype, q == null ? null : q.getAnswer(), correct));

                if (qtype == 5) {
                    // 简答题：客观题不打分，subjectiveScore 在 step11 已经处理
                    a.setIsCorrect(null);
                    if (status == 3) {
                        a.setScore(faker.randomScore(0, pq.getScore().doubleValue()));
                        a.setComment(correct ? "答得不错" : "需加强");
                    } else {
                        a.setScore(BigDecimal.ZERO);
                    }
                } else {
                    a.setIsCorrect(correct ? 1 : 0);
                    a.setScore(correct ? pq.getScore() : BigDecimal.ZERO);
                }
                a.setIsRemoved(0);
                a.setCreateTime(answerTime);
                answerMapper.insert(a);
                created++;
            }
            if (created % 5000 == 0 && created > 0) log("  已生成答题 " + created + " 条");
        }
        stepEnd("step12_answers", t, created);
    }

    private String simulateStudentAnswer(int qtype, String correctAnswer, boolean simulateCorrect) {
        if (simulateCorrect && correctAnswer != null) return correctAnswer;
        switch (qtype) {
            case 1:  return faker.pickRandom(Arrays.asList("A", "B", "C", "D"));
            case 2:  return faker.pickRandom(Arrays.asList("AB", "BC", "CD", "AC", "BD"));
            case 3:  return faker.pickRandom(Arrays.asList("正确", "错误"));
            case 4:  return "学生填的答案";
            case 5:  return "学生答题内容（约 50 字简答）。本题学生回答了部分要点。";
            default: return "A";
        }
    }

    // ====================================================================
    // STEP 13: 创建聊天会话 + 消息（直接 Mapper，避免 WebSocket 推送）
    // ====================================================================
    @Test
    @Order(13)
    void step13_chats() {
        long t = stepBegin("step13_chats");
        ensureTeacherLoaded();
        ensureStudentLoaded();

        // 【幂等性优化】预加载所有已存在的 (u1, u2) 对到内存，避免与 chat_conversation.uk_users 唯一约束冲突
        java.util.Set<String> usedPairs = new java.util.HashSet<>();
        for (ChatConversation existing : conversationMapper.selectList(null)) {
            usedPairs.add(existing.getUser1Id() + "-" + existing.getUser2Id());
        }
        log("  已存在会话数：" + usedPairs.size());

        // 增量补足到目标数
        int toCreate = Math.max(0, COUNT_CONVERSATIONS - usedPairs.size());
        log("  本轮需要新建：" + toCreate);

        int convs = 0, msgs = 0, skipped = 0;
        for (int i = 0; i < toCreate; i++) {
            // 一半教师-学生对话，一半学生-学生
            Long u1, u2;
            int retry = 0;
            String pairKey;
            do {
                if (i % 2 == 0) {
                    u1 = faker.pickRandom(teacherIds);
                    u2 = faker.pickRandom(studentIds);
                } else {
                    List<Long> all = new ArrayList<>(studentIds);
                    Collections.shuffle(all, faker.getRandom());
                    u1 = all.get(0); u2 = all.get(1);
                }
                // 保证 u1Id < u2Id（项目惯例，避免重复会话）
                if (u1 > u2) { Long tmp = u1; u1 = u2; u2 = tmp; }
                pairKey = u1 + "-" + u2;
                retry++;
            } while (usedPairs.contains(pairKey) && retry < 30);

            if (usedPairs.contains(pairKey)) {
                // 30 次都没找到不重复的 pair，跳过本轮（极小概率）
                skipped++;
                continue;
            }
            usedPairs.add(pairKey);

            ChatConversation c = new ChatConversation();
            c.setUser1Id(u1);
            c.setUser2Id(u2);
            c.setUser1Hidden(0);
            c.setUser2Hidden(0);
            c.setUser1Pinned(faker.randomInt(1, 100) <= 10 ? 1 : 0);  // 10% 置顶
            c.setUser2Pinned(faker.randomInt(1, 100) <= 10 ? 1 : 0);
            c.setUser1Muted(faker.randomInt(1, 100) <= 5 ? 1 : 0);    // 5% 免打扰
            c.setUser2Muted(0);
            c.setCreateTime(LocalDateTime.now().minusDays(faker.randomInt(1, 30)));
            conversationMapper.insert(c);
            convs++;

            // 每会话 ~25 条消息
            LocalDateTime msgTime = c.getCreateTime();
            String lastContent = null;
            Long lastSenderId = null;
            for (int j = 0; j < MESSAGES_PER_CONVERSATION; j++) {
                ChatMessage m = new ChatMessage();
                m.setConversationId(c.getId());
                Long senderId = (j % 2 == 0) ? u1 : u2;
                Long receiverId = (senderId.equals(u1)) ? u2 : u1;
                m.setSenderId(senderId);
                m.setReceiverId(receiverId);
                m.setContent(faker.chatMessage());
                m.setMessageType(1);  // 1=文本
                m.setIsRead(faker.randomInt(1, 100) <= 80 ? 1 : 0);
                msgTime = msgTime.plusMinutes(faker.randomInt(1, 60));
                m.setCreateTime(msgTime);
                messageMapper.insert(m);
                msgs++;
                lastContent = m.getContent();
                lastSenderId = senderId;
            }
            // 更新会话的 last_message
            c.setLastMessage(lastContent);
            c.setLastMessageTime(msgTime);
            c.setLastMessageSenderId(lastSenderId);
            conversationMapper.updateById(c);
        }
        stepEnd("step13_chats", t, convs + msgs);
        log("📋 conversations = " + convs + ", messages = " + msgs + ", skipped = " + skipped);
    }

    // ====================================================================
    // STEP 14: 创建系统通知（直接 Mapper）
    // ====================================================================
    @Test
    @Order(14)
    void step14_notifies() {
        long t = stepBegin("step14_notifies");
        ensureStudentLoaded();

        String[] types = {"EXAM_PUBLISHED", "EXAM_REMINDER", "SCORE_PUBLISHED", "GRADING_FINISHED"};
        int created = 0;
        for (int i = 0; i < COUNT_NOTIFICATIONS; i++) {
            String type = types[i % types.length];
            Long userId = faker.pickRandom(studentIds);

            SysNotification n = new SysNotification();
            n.setUserId(userId);
            n.setType(type);
            n.setTitle(faker.notificationTitle(type));
            n.setContent(faker.notificationContent(type));
            n.setBizType("EXAM");
            n.setBizId(faker.pickRandom(examIds == null || examIds.isEmpty() ? Arrays.asList(0L) : examIds));
            n.setIsRead(faker.randomInt(1, 100) <= 60 ? 1 : 0);
            n.setPriority(faker.randomInt(1, 3));
            n.setPayload("{\"senderId\":1,\"senderName\":\"系统管理员\"}");

            notificationMapper.insert(n);
            created++;
        }
        stepEnd("step14_notifies", t, created);
    }

    // ========== 内部辅助：懒加载缓存 ==========

    private boolean existsUser(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)) > 0;
    }

    private void ensureBaseLoaded() {
        if (majorIds == null) majorIds = majorMapper.selectList(null).stream().map(EduMajor::getId).collect(Collectors.toList());
        if (subjectIds == null) subjectIds = subjectMapper.selectList(null).stream().map(EduSubject::getId).collect(Collectors.toList());
        if (classIds == null) classIds = classMapper.selectList(null).stream().map(EduClass::getId).collect(Collectors.toList());
    }

    private void ensureTeacherLoaded() {
        if (teacherIds == null || teacherIds.isEmpty()) {
            teacherIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .likeRight(SysUser::getUsername, PREFIX_TEACHER))
                    .stream().map(SysUser::getId).collect(Collectors.toList());
        }
    }

    private void ensureStudentLoaded() {
        if (studentIds == null || studentIds.isEmpty()) {
            studentIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .likeRight(SysUser::getUsername, PREFIX_STUDENT))
                    .stream().map(SysUser::getId).collect(Collectors.toList());
        }
    }

    private void ensureQuestionLoaded() {
        if (questionIds == null || questionIds.isEmpty()) {
            questionIds = questionMapper.selectList(null).stream()
                    .map(ExamQuestion::getId).collect(Collectors.toList());
        }
    }

    private void ensurePaperLoaded() {
        if (paperIds == null || paperIds.isEmpty()) {
            paperIds = paperMapper.selectList(null).stream()
                    .map(ExamPaper::getId).collect(Collectors.toList());
        }
    }

    private void ensureExamLoaded() {
        if (examIds == null || examIds.isEmpty()) {
            examIds = examMapper.selectList(null).stream()
                    .map(ExamExam::getId).collect(Collectors.toList());
        }
    }

    private void ensureRecordLoaded() {
        if (recordIds == null || recordIds.isEmpty()) {
            recordIds = new ArrayList<>();
            recordStatusMap = new HashMap<>();
            recordPaperMap = new HashMap<>();
            recordUserMap = new HashMap<>();
            for (ExamRecord r : recordMapper.selectList(null)) {
                recordIds.add(r.getId());
                recordStatusMap.put(r.getId(), r.getStatus());
                recordPaperMap.put(r.getId(), r.getPaperId());
                recordUserMap.put(r.getId(), r.getUserId());
            }
        }
    }
}
