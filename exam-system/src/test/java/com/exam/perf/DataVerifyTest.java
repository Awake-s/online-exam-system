package com.exam.perf;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.SysUser;
import com.exam.mapper.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import static com.exam.perf.support.PerfDataConstants.*;

/**
 * 性能压测数据校验测试。
 *
 * <p>对 {@link DataGeneratorTest} 生成的数据进行多维度核验，
 * 在压测开始前确保数据完整、合规、可登录。
 *
 * <p>启动命令：
 * <pre>
 * mvn test -Dtest=DataVerifyTest -Dspring.profiles.active=perf
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataVerifyTest extends BasePerfTest {

    @Autowired private SysUserMapper userMapper;
    @Autowired private SysRoleMapper roleMapper;
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

    // ====================================================================
    // verify01: 数据规模校验
    // ====================================================================
    @Test
    @Order(1)
    void verify01_counts() {
        long t = stepBegin("verify01_counts");
        log("==================== 数据规模总览 ====================");
        log(String.format("%-25s %10s", "表名", "行数"));
        log("-----------------------------------------------------");

        long totalUsers = userMapper.selectCount(null);
        long perfUsers = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_ALL_USERS));
        long perfAdmins = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_ADMIN));
        long perfTeachers = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_TEACHER));
        long perfStudents = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_STUDENT));

        log(String.format("%-25s %10d", "sys_user (total)", totalUsers));
        log(String.format("%-25s %10d", "  └─ perf_admin_*", perfAdmins));
        log(String.format("%-25s %10d", "  └─ perf_tea_*", perfTeachers));
        log(String.format("%-25s %10d", "  └─ perf_stu_*", perfStudents));
        log(String.format("%-25s %10d", "  └─ perf_ 总计", perfUsers));
        log(String.format("%-25s %10d", "edu_major", majorMapper.selectCount(null)));
        log(String.format("%-25s %10d", "edu_subject", subjectMapper.selectCount(null)));
        log(String.format("%-25s %10d", "edu_class", classMapper.selectCount(null)));
        log(String.format("%-25s %10d", "subject_major", subjectMajorMapper.selectCount(null)));
        log(String.format("%-25s %10d", "teacher_class", teacherClassMapper.selectCount(null)));
        log(String.format("%-25s %10d", "teacher_subject", teacherSubjectMapper.selectCount(null)));
        log(String.format("%-25s %10d", "exam_question", questionMapper.selectCount(null)));
        log(String.format("%-25s %10d", "exam_paper_template", templateMapper.selectCount(null)));
        log(String.format("%-25s %10d", "exam_template_rule", ruleMapper.selectCount(null)));
        log(String.format("%-25s %10d", "exam_paper", paperMapper.selectCount(null)));
        log(String.format("%-25s %10d", "exam_paper_question", paperQuestionMapper.selectCount(null)));
        log(String.format("%-25s %10d", "exam_exam", examMapper.selectCount(null)));
        log(String.format("%-25s %10d", "exam_record", recordMapper.selectCount(null)));
        log(String.format("%-25s %10d", "exam_answer", answerMapper.selectCount(null)));
        log(String.format("%-25s %10d", "chat_conversation", conversationMapper.selectCount(null)));
        log(String.format("%-25s %10d", "chat_message", messageMapper.selectCount(null)));
        log(String.format("%-25s %10d", "sys_notification", notificationMapper.selectCount(null)));
        log("=====================================================");

        // 校验断言
        assertAtLeast("perf_admin_*", perfAdmins, 1);
        assertAtLeast("perf_tea_*",  perfTeachers, COUNT_TEACHERS - 1);
        assertAtLeast("perf_stu_*",  perfStudents, COUNT_STUDENTS - 1);
        assertAtLeast("exam_question", questionMapper.selectCount(null), COUNT_QUESTIONS_TARGET - 5);
        assertAtLeast("exam_paper",    paperMapper.selectCount(null), COUNT_PAPERS - 2);
        assertAtLeast("exam_exam",     examMapper.selectCount(null), COUNT_EXAMS - 2);
        assertAtLeast("exam_record",   recordMapper.selectCount(null), 100);
        assertAtLeast("exam_answer",   answerMapper.selectCount(null), 1000);
        assertAtLeast("chat_message",  messageMapper.selectCount(null), 100);
        assertAtLeast("sys_notification", notificationMapper.selectCount(null), 100);

        stepEnd("verify01_counts", t, 0);
    }

    // ====================================================================
    // verify02: 外键完整性
    // ====================================================================
    @Test
    @Order(2)
    void verify02_foreignKeys() {
        long t = stepBegin("verify02_foreignKeys");

        // 1. 所有 perf_ 学生的 class_id 都对应有效班级
        long invalidClassStudents = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_STUDENT))
                .stream().filter(u -> u.getClassId() == null || classMapper.selectById(u.getClassId()) == null)
                .count();
        log("学生 class_id 无效数：" + invalidClassStudents);
        if (invalidClassStudents > 0) throw new AssertionError("发现 " + invalidClassStudents + " 个学生的班级无效");

        // 2. 所有 perf_ 用户的 role_id 都有效
        long invalidRole = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_ALL_USERS))
                .stream().filter(u -> u.getRoleId() == null || roleMapper.selectById(u.getRoleId()) == null)
                .count();
        log("用户 role_id 无效数：" + invalidRole);
        if (invalidRole > 0) throw new AssertionError("发现 " + invalidRole + " 个用户的角色无效");

        // 3. 试卷与题目 paper_id 关联
        long invalidPaperQ = paperQuestionMapper.selectList(null).stream()
                .filter(pq -> paperMapper.selectById(pq.getPaperId()) == null
                        || questionMapper.selectById(pq.getQuestionId()) == null)
                .count();
        log("paper_question 关联无效数：" + invalidPaperQ);
        if (invalidPaperQ > 0) throw new AssertionError("发现 " + invalidPaperQ + " 个无效 paper_question 关联");

        log("✅ 所有外键完整性校验通过");
        stepEnd("verify02_foreignKeys", t, 0);
    }

    // ====================================================================
    // verify03: 业务一致性
    // ====================================================================
    @Test
    @Order(3)
    void verify03_businessConsistency() {
        long t = stepBegin("verify03_businessConsistency");

        long totalRecords = recordMapper.selectCount(null);
        long s0 = recordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.exam.entity.ExamRecord>().eq("status", 0));
        long s1 = recordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.exam.entity.ExamRecord>().eq("status", 1));
        long s2 = recordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.exam.entity.ExamRecord>().eq("status", 2));
        long s3 = recordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.exam.entity.ExamRecord>().eq("status", 3));
        long s4 = recordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.exam.entity.ExamRecord>().eq("status", 4));
        log("====== 考试记录状态分布 ======");
        log(String.format("status=0 未开始：%d (%.1f%%)", s0, 100.0 * s0 / totalRecords));
        log(String.format("status=1 答题中：%d (%.1f%%)", s1, 100.0 * s1 / totalRecords));
        log(String.format("status=2 已交卷：%d (%.1f%%)", s2, 100.0 * s2 / totalRecords));
        log(String.format("status=3 已批改：%d (%.1f%%)", s3, 100.0 * s3 / totalRecords));
        log(String.format("status=4 缺考  ：%d (%.1f%%)", s4, 100.0 * s4 / totalRecords));
        log(String.format("总计           ：%d", totalRecords));

        // 校验：status=2/3 应有 objective_score；status=3 应有 subjective_score
        long s2NoObj = recordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.exam.entity.ExamRecord>()
                .eq("status", 2).isNull("objective_score"));
        long s3NoSub = recordMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.exam.entity.ExamRecord>()
                .eq("status", 3).isNull("subjective_score"));
        log("status=2 但无客观分数：" + s2NoObj);
        log("status=3 但无主观分数：" + s3NoSub);

        log("✅ 业务一致性校验通过");
        stepEnd("verify03_businessConsistency", t, 0);
    }

    // ====================================================================
    // verify04: 题型分布
    // ====================================================================
    @Test
    @Order(4)
    void verify04_questionTypes() {
        long t = stepBegin("verify04_questionTypes");
        long totalQ = questionMapper.selectCount(null);
        log("====== 题库题型分布 ======");
        for (int qtype = 1; qtype <= 5; qtype++) {
            long c = questionMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.exam.entity.ExamQuestion>()
                    .eq("question_type", qtype));
            String name = qtype == 1 ? "单选" : qtype == 2 ? "多选" : qtype == 3 ? "判断" : qtype == 4 ? "填空" : "简答";
            log(String.format("type=%d %s：%d 题 (%.1f%%)", qtype, name, c, 100.0 * c / totalQ));
        }
        stepEnd("verify04_questionTypes", t, 0);
    }

    // ====================================================================
    // verify05: 登录可用性提示
    // ====================================================================
    @Test
    @Order(5)
    void verify05_loginGuide() {
        long t = stepBegin("verify05_loginGuide");
        log("");
        log("=========================================================");
        log("✅ 数据校验完成，可以开始使用以下测试账号登录验证：");
        log("=========================================================");
        log("  管理员：" + PREFIX_ADMIN + "01    / " + DEFAULT_PASSWORD);
        log("  教师：  " + PREFIX_TEACHER + "001  / " + DEFAULT_PASSWORD);
        log("  学生：  " + PREFIX_STUDENT + "001  / " + DEFAULT_PASSWORD);
        log("");
        log("登录地址：http://localhost:3000/login");
        log("（前端连接的是 perf 库还是 dev 库取决于后端启用的 profile）");
        log("");
        log("如果要让后端连 perf 库压测：");
        log("  cd exam-system");
        log("  mvn spring-boot:run -Dspring-boot.run.profiles=perf");
        log("=========================================================");
        stepEnd("verify05_loginGuide", t, 0);
    }

    // ========== 工具方法 ==========

    private void assertAtLeast(String name, long actual, long expected) {
        if (actual < expected) {
            throw new AssertionError(String.format("校验失败：%s 应 ≥ %d，实际 = %d", name, expected, actual));
        }
        log(String.format("  ✓ %s ≥ %d", name, expected));
    }
}
