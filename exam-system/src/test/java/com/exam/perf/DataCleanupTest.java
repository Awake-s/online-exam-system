package com.exam.perf;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.exam.entity.SysUser;
import com.exam.mapper.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static com.exam.perf.support.PerfDataConstants.*;

/**
 * 性能压测数据一键清理测试。
 *
 * <p>仅清理由 {@link DataGeneratorTest} 生成的、用户名以 {@code perf_} 开头的数据。
 * 完全不会影响正常的开发数据，可任意时刻执行。
 *
 * <p>清理顺序严格按外键依赖逆向：
 * <ol>
 *   <li>exam_answer       (依赖 exam_record)</li>
 *   <li>exam_record       (依赖 sys_user / exam_exam)</li>
 *   <li>chat_message      (依赖 chat_conversation / sys_user)</li>
 *   <li>chat_conversation (依赖 sys_user)</li>
 *   <li>sys_notification  (依赖 sys_user)</li>
 *   <li>teacher_class     (依赖 sys_user)</li>
 *   <li>teacher_subject   (依赖 sys_user)</li>
 *   <li>sys_user          (perf_ 前缀)</li>
 *   <li>exam_paper_question / exam_paper / exam_template_rule / exam_paper_template / exam_exam（按 perf 标识）</li>
 *   <li>exam_question     (perf 创建的)</li>
 * </ol>
 *
 * <p>启动命令：
 * <pre>
 * mvn test -Dtest=DataCleanupTest -Dspring.profiles.active=perf
 * </pre>
 *
 * <p>⚠ 警告：此操作不可逆！但由于使用独立 schema online_exam_system_perf，
 * 即使误删也只影响压测库，开发库 online_exam_system 完全不受影响。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataCleanupTest extends BasePerfTest {

    @Autowired private SysUserMapper userMapper;
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
    /** 用于物理删除被 MyBatis-Plus 软删除拦截的 exam_question 记录 */
    @Autowired private JdbcTemplate jdbcTemplate;

    // ====================================================================
    // STEP 0: 显示清理前数据规模
    // ====================================================================
    @Test
    @Order(0)
    void step00_preCleanupSummary() {
        long t = stepBegin("step00_preCleanupSummary");
        log("");
        log("====================================================");
        log("⚠ DataCleanupTest 即将执行清理");
        log("⚠ 仅删除以下前缀的用户及其级联数据：");
        log("⚠   - " + PREFIX_ADMIN + "*");
        log("⚠   - " + PREFIX_TEACHER + "*");
        log("⚠   - " + PREFIX_STUDENT + "*");
        log("⚠ 开发数据（admin / luweizhong / taozhan）将完全保留");
        log("====================================================");
        long perfUsers = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_ALL_USERS));
        log("待清理 perf_ 用户：" + perfUsers);
        stepEnd("step00_preCleanupSummary", t, (int) perfUsers);
    }

    // ====================================================================
    // STEP 1: 找到所有 perf 用户的 ID（一次查询，后续复用）
    // ====================================================================
    private List<Long> getPerfUserIds() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_ALL_USERS))
                .stream().map(SysUser::getId).collect(Collectors.toList());
    }

    // ====================================================================
    // STEP 2: 清理答题记录 exam_answer
    // ====================================================================
    @Test
    @Order(2)
    void step02_cleanAnswers() {
        long t = stepBegin("step02_cleanAnswers");
        List<Long> perfUserIds = getPerfUserIds();
        if (perfUserIds.isEmpty()) {
            stepEnd("step02_cleanAnswers", t, 0);
            return;
        }

        // 找到 perf 用户的 record_id 列表
        List<Long> recordIds = recordMapper.selectList(new QueryWrapper<com.exam.entity.ExamRecord>()
                .in("user_id", perfUserIds))
                .stream().map(com.exam.entity.ExamRecord::getId).collect(Collectors.toList());

        if (recordIds.isEmpty()) {
            stepEnd("step02_cleanAnswers", t, 0);
            return;
        }

        int deleted = answerMapper.delete(new QueryWrapper<com.exam.entity.ExamAnswer>()
                .in("record_id", recordIds));
        stepEnd("step02_cleanAnswers", t, deleted);
    }

    // ====================================================================
    // STEP 3: 清理考试记录 exam_record
    // ====================================================================
    @Test
    @Order(3)
    void step03_cleanRecords() {
        long t = stepBegin("step03_cleanRecords");
        List<Long> perfUserIds = getPerfUserIds();
        if (perfUserIds.isEmpty()) { stepEnd("step03_cleanRecords", t, 0); return; }

        int deleted = recordMapper.delete(new QueryWrapper<com.exam.entity.ExamRecord>()
                .in("user_id", perfUserIds));
        stepEnd("step03_cleanRecords", t, deleted);
    }

    // ====================================================================
    // STEP 4: 清理聊天消息（perf 用户参与的全部）
    // ====================================================================
    @Test
    @Order(4)
    void step04_cleanChatMessages() {
        long t = stepBegin("step04_cleanChatMessages");
        List<Long> perfUserIds = getPerfUserIds();
        if (perfUserIds.isEmpty()) { stepEnd("step04_cleanChatMessages", t, 0); return; }

        int deleted = messageMapper.delete(new QueryWrapper<com.exam.entity.ChatMessage>()
                .in("sender_id", perfUserIds).or().in("receiver_id", perfUserIds));
        stepEnd("step04_cleanChatMessages", t, deleted);
    }

    // ====================================================================
    // STEP 5: 清理聊天会话
    // ====================================================================
    @Test
    @Order(5)
    void step05_cleanConversations() {
        long t = stepBegin("step05_cleanConversations");
        List<Long> perfUserIds = getPerfUserIds();
        if (perfUserIds.isEmpty()) { stepEnd("step05_cleanConversations", t, 0); return; }

        int deleted = conversationMapper.delete(new QueryWrapper<com.exam.entity.ChatConversation>()
                .in("user1_id", perfUserIds).or().in("user2_id", perfUserIds));
        stepEnd("step05_cleanConversations", t, deleted);
    }

    // ====================================================================
    // STEP 6: 清理通知
    // ====================================================================
    @Test
    @Order(6)
    void step06_cleanNotifications() {
        long t = stepBegin("step06_cleanNotifications");
        List<Long> perfUserIds = getPerfUserIds();
        if (perfUserIds.isEmpty()) { stepEnd("step06_cleanNotifications", t, 0); return; }

        int deleted = notificationMapper.delete(new QueryWrapper<com.exam.entity.SysNotification>()
                .in("user_id", perfUserIds));
        stepEnd("step06_cleanNotifications", t, deleted);
    }

    // ====================================================================
    // STEP 7: 清理教师-班级 / 教师-科目关联
    // ====================================================================
    @Test
    @Order(7)
    void step07_cleanTeacherRelations() {
        long t = stepBegin("step07_cleanTeacherRelations");
        List<Long> perfUserIds = getPerfUserIds();
        if (perfUserIds.isEmpty()) { stepEnd("step07_cleanTeacherRelations", t, 0); return; }

        int tcDeleted = teacherClassMapper.delete(new QueryWrapper<com.exam.entity.TeacherClass>()
                .in("teacher_id", perfUserIds));
        int tsDeleted = teacherSubjectMapper.delete(new QueryWrapper<com.exam.entity.TeacherSubject>()
                .in("teacher_id", perfUserIds));
        stepEnd("step07_cleanTeacherRelations", t, tcDeleted + tsDeleted);
        log("  teacher_class deleted = " + tcDeleted);
        log("  teacher_subject deleted = " + tsDeleted);
    }

    // ====================================================================
    // STEP 8: 清理考试-试卷-题库（perf 教师创建的）
    //   策略：先识别出所有 perf 教师的 user_id，再级联删除：
    //         exam_exam (creator_id IN ...) → exam_paper_question (paper_id IN ...) → exam_paper → exam_template_rule → exam_paper_template → exam_question
    // ====================================================================
    @Test
    @Order(8)
    void step08_cleanExamChain() {
        long t = stepBegin("step08_cleanExamChain");
        List<Long> perfTeacherIds = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_TEACHER))
                .stream().map(SysUser::getId).collect(Collectors.toList());
        if (perfTeacherIds.isEmpty()) { stepEnd("step08_cleanExamChain", t, 0); return; }

        // 先清考试
        int examDel = examMapper.delete(new QueryWrapper<com.exam.entity.ExamExam>()
                .in("creator_id", perfTeacherIds));
        // 再清试卷及关联（先 paper_question 后 paper）
        List<Long> perfPaperIds = paperMapper.selectList(new QueryWrapper<com.exam.entity.ExamPaper>()
                .in("creator_id", perfTeacherIds))
                .stream().map(com.exam.entity.ExamPaper::getId).collect(Collectors.toList());
        int pqDel = 0, paperDel = 0;
        if (!perfPaperIds.isEmpty()) {
            pqDel = paperQuestionMapper.delete(new QueryWrapper<com.exam.entity.ExamPaperQuestion>()
                    .in("paper_id", perfPaperIds));
            paperDel = paperMapper.delete(new QueryWrapper<com.exam.entity.ExamPaper>()
                    .in("id", perfPaperIds));
        }
        // 模板及规则
        List<Long> perfTplIds = templateMapper.selectList(new QueryWrapper<com.exam.entity.ExamPaperTemplate>()
                .in("creator_id", perfTeacherIds))
                .stream().map(com.exam.entity.ExamPaperTemplate::getId).collect(Collectors.toList());
        int ruleDel = 0, tplDel = 0;
        if (!perfTplIds.isEmpty()) {
            ruleDel = ruleMapper.delete(new QueryWrapper<com.exam.entity.ExamTemplateRule>()
                    .in("template_id", perfTplIds));
            tplDel = templateMapper.delete(new QueryWrapper<com.exam.entity.ExamPaperTemplate>()
                    .in("id", perfTplIds));
        }
        // 题库（perf 教师创建的）—— 必须用原生 SQL 物理删除，
        // 因为 ExamQuestion 实体有 `deleted` 字段且 application.yml 全局开启了软删除，
        // MyBatis-Plus 的 mapper.delete() 实际只是 UPDATE deleted=1，会导致后续删除 sys_user 时外键违反。
        String inClause = perfTeacherIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        int qDel = jdbcTemplate.update("DELETE FROM exam_question WHERE creator_id IN (" + inClause + ")");
        // 同时清理可能残留的"软删除"题目（deleted=1 的 perf 题目，包括上次失败 cleanup 留下的）
        int qDelLegacy = jdbcTemplate.update(
                "DELETE FROM exam_question WHERE deleted = 1 AND creator_id IN (" + inClause + ")");

        log("  exam_exam deleted = " + examDel);
        log("  exam_paper_question deleted = " + pqDel);
        log("  exam_paper deleted = " + paperDel);
        log("  exam_template_rule deleted = " + ruleDel);
        log("  exam_paper_template deleted = " + tplDel);
        log("  exam_question deleted (physical) = " + qDel);
        log("  exam_question deleted (legacy soft-deleted) = " + qDelLegacy);

        stepEnd("step08_cleanExamChain", t, examDel + pqDel + paperDel + ruleDel + tplDel + qDel + qDelLegacy);
    }

    // ====================================================================
    // STEP 9: 最后清理 sys_user（所有 perf 前缀用户）
    // ====================================================================
    @Test
    @Order(9)
    void step09_cleanUsers() {
        long t = stepBegin("step09_cleanUsers");
        int deleted = userMapper.delete(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_ALL_USERS));
        stepEnd("step09_cleanUsers", t, deleted);
    }

    // ====================================================================
    // STEP 10: 清理结束总结
    // ====================================================================
    @Test
    @Order(10)
    void step10_postCleanupSummary() {
        long t = stepBegin("step10_postCleanupSummary");
        long remainingPerf = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .likeRight(SysUser::getUsername, PREFIX_ALL_USERS));
        log("");
        log("====================================================");
        log("✅ 清理完成");
        log("剩余 perf_ 用户数：" + remainingPerf);
        log("（应该为 0，否则表示还有遗留）");
        log("");
        log("当前 sys_user 总数：" + userMapper.selectCount(null));
        log("当前 exam_question：" + questionMapper.selectCount(null));
        log("当前 exam_record：" + recordMapper.selectCount(null));
        log("当前 chat_message：" + messageMapper.selectCount(null));
        log("当前 sys_notification：" + notificationMapper.selectCount(null));
        log("====================================================");

        if (remainingPerf > 0) {
            throw new AssertionError("清理后还有 " + remainingPerf + " 个 perf_ 用户残留！");
        }
        stepEnd("step10_postCleanupSummary", t, 0);
    }
}
