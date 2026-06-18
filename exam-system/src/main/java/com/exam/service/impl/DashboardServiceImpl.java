package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.constants.RoleConstants;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired private SysUserMapper userMapper;
    @Autowired private EduClassMapper classMapper;
    @Autowired private EduSubjectMapper subjectMapper;
    @Autowired private ExamExamMapper examMapper;
    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private ExamPaperMapper paperMapper;
    @Autowired private ExamRecordMapper recordMapper;

    @Override
    public Map<String, Object> getAdminDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("adminCount", userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, RoleConstants.ADMIN_ROLE_ID)));
        data.put("teacherCount", userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, RoleConstants.TEACHER_ROLE_ID)));
        data.put("studentCount", userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID)));
        data.put("classCount", classMapper.selectCount(null));
        data.put("subjectCount", subjectMapper.selectCount(null));
        data.put("examCount", examMapper.selectCount(null));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayExamCount = examMapper.selectCount(new LambdaQueryWrapper<ExamExam>()
                .le(ExamExam::getStartTime, todayEnd).ge(ExamExam::getEndTime, todayStart));
        data.put("todayExamCount", todayExamCount);
        return data;
    }

    @Override
    public Map<String, Object> getTeacherDashboard(Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("myQuestionCount", questionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getCreatorId, userId).eq(ExamQuestion::getDeleted, 0)));
        data.put("myPaperCount", paperMapper.selectCount(new LambdaQueryWrapper<ExamPaper>().eq(ExamPaper::getCreatorId, userId)));

        LocalDateTime now = LocalDateTime.now();
        List<ExamExam> myExams = examMapper.selectList(new LambdaQueryWrapper<ExamExam>().eq(ExamExam::getCreatorId, userId));
        long ongoingCount = myExams.stream().filter(e -> !now.isBefore(e.getStartTime()) && !now.isAfter(e.getEndTime())).count();
        data.put("ongoingExamCount", ongoingCount);

        long pendingMarkCount = 0;
        if (!myExams.isEmpty()) {
            List<Long> examIds = myExams.stream().map(ExamExam::getId).collect(Collectors.toList());
            pendingMarkCount = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .in(ExamRecord::getExamId, examIds).eq(ExamRecord::getStatus, 2));
        }
        data.put("pendingMarkCount", pendingMarkCount);

        // 最近考试
        Map<Long, String> classMap = classMapper.selectList(null).stream()
                .collect(Collectors.toMap(EduClass::getId, EduClass::getClassName));
        String[] statusNames = {"未开始", "进行中", "已结束"};
        List<Map<String, Object>> recentExams = myExams.stream()
                .sorted(Comparator.comparing(ExamExam::getStartTime).reversed())
                .limit(5).map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("examId", e.getId());
                    m.put("examName", e.getExamName());
                    m.put("className", classMap.getOrDefault(e.getClassId(), ""));
                    m.put("startTime", e.getStartTime());
                    m.put("endTime", e.getEndTime());
                    int status;
                    if (now.isBefore(e.getStartTime())) status = 0;
                    else if (now.isAfter(e.getEndTime())) status = 2;
                    else status = 1;
                    m.put("status", status);
                    m.put("statusName", statusNames[status]);
                    // 参考人数统计
                    List<ExamRecord> examRecords = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                            .eq(ExamRecord::getExamId, e.getId()));
                    long submitted = examRecords.stream().filter(r -> r.getStatus() >= 2).count();
                    long pending = examRecords.stream().filter(r -> r.getStatus() == 2).count();
                    long graded = examRecords.stream().filter(r -> r.getStatus() == 3).count();
                    m.put("submittedCount", submitted);
                    m.put("pendingCount", pending);
                    m.put("gradedCount", graded);
                    // 已批改考试的平均分
                    if (graded > 0) {
                        BigDecimal scoreSum = examRecords.stream()
                                .filter(r -> r.getStatus() == 3 && r.getTotalScore() != null)
                                .map(ExamRecord::getTotalScore)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        m.put("avgScore", scoreSum.divide(new BigDecimal(graded), 1, RoundingMode.HALF_UP));
                    }
                    return m;
                }).collect(Collectors.toList());
        data.put("recentExams", recentExams);
        return data;
    }

    @Override
    public Map<String, Object> getStudentDashboard(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return doGetStudentDashboard(userId, user != null ? user.getClassId() : null, user);
    }

    @Override
    public Map<String, Object> getStudentDashboard(Long userId, Long classId) {
        SysUser currentUser = userMapper.selectById(userId);
        return doGetStudentDashboard(userId, classId, currentUser);
    }

    private Map<String, Object> doGetStudentDashboard(Long userId, Long classId, SysUser currentUser) {
        Map<String, Object> data = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        List<ExamExam> classExams;
        if (classId != null && currentUser != null) {
            LambdaQueryWrapper<ExamExam> examWrapper = new LambdaQueryWrapper<ExamExam>()
                    .eq(ExamExam::getClassId, classId);
            if (currentUser.getCreateTime() != null) {
                examWrapper.ge(ExamExam::getStartTime, currentUser.getCreateTime());
            }
            classExams = examMapper.selectList(examWrapper);
        } else {
            classExams = Collections.emptyList();
        }

        // 待考试数（进行中且未参加）
        long pendingCount = 0;
        List<Map<String, Object>> pendingExams = new ArrayList<>();
        for (ExamExam e : classExams) {
            if (!now.isBefore(e.getStartTime()) && !now.isAfter(e.getEndTime())) {
                ExamRecord record = recordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, e.getId()).eq(ExamRecord::getUserId, userId));
                if (record == null || record.getStatus() < 2) {
                    pendingCount++;
                    if (pendingExams.size() < 5) {
                        ExamPaper paper = paperMapper.selectById(e.getPaperId());
                        EduSubject subject = paper != null ? subjectMapper.selectById(paper.getSubjectId()) : null;
                        Map<String, Object> m = new HashMap<>();
                        m.put("examId", e.getId());
                        m.put("examName", e.getExamName());
                        m.put("subjectName", subject != null ? subject.getSubjectName() : "");
                        m.put("startTime", e.getStartTime());
                        m.put("endTime", e.getEndTime());
                        pendingExams.add(m);
                    }
                }
            }
        }
        data.put("pendingExamCount", pendingCount);
        data.put("pendingExams", pendingExams);

        // 已完成考试数（已批改 + 缺考），已批改的必须成绩已发布
        List<ExamRecord> allCompletedRecords = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId).in(ExamRecord::getStatus, 3, 4));
        // 获取所有相关考试的发布状态
        Set<Long> completedExamIds = allCompletedRecords.stream().map(ExamRecord::getExamId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ExamExam> completedExamMap = completedExamIds.isEmpty() ? Collections.emptyMap() :
                examMapper.selectBatchIds(completedExamIds).stream()
                        .collect(Collectors.toMap(ExamExam::getId, e -> e));
        // 过滤：已批改(3)和缺考(4)均需成绩已发布才计入统计
        List<ExamRecord> completedRecords = allCompletedRecords.stream().filter(r -> {
            ExamExam ex = completedExamMap.get(r.getExamId());
            return ex != null && Integer.valueOf(1).equals(ex.getScorePublished());
        }).collect(Collectors.toList());
        data.put("completedExamCount", completedRecords.size());
        if (!completedRecords.isEmpty()) {
            BigDecimal sum = completedRecords.stream()
                    .map(r -> r.getTotalScore() != null ? r.getTotalScore() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            data.put("averageScore", sum.divide(new BigDecimal(completedRecords.size()), 2, RoundingMode.HALF_UP));
        } else {
            data.put("averageScore", 0);
        }

        // 最高分
        if (!completedRecords.isEmpty()) {
            BigDecimal highest = completedRecords.stream()
                    .map(r -> r.getTotalScore() != null ? r.getTotalScore() : BigDecimal.ZERO)
                    .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            data.put("highestScore", highest);
        } else {
            data.put("highestScore", 0);
        }

        // 及格次数
        if (!completedRecords.isEmpty()) {
            long passedCount = completedRecords.stream().filter(r -> {
                if (r.getTotalScore() == null) return false;
                ExamPaper p = paperMapper.selectById(r.getPaperId());
                return p != null && r.getTotalScore().compareTo(p.getPassScore()) >= 0;
            }).count();
            data.put("passedCount", passedCount);
        } else {
            data.put("passedCount", 0);
        }

        // 最近成绩
        List<Map<String, Object>> recentScores = completedRecords.stream()
                .sorted(Comparator.comparing(ExamRecord::getSubmitTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5).map(r -> {
                    ExamExam exam = examMapper.selectById(r.getExamId());
                    ExamPaper paper = paperMapper.selectById(r.getPaperId());
                    Map<String, Object> m = new HashMap<>();
                    m.put("recordId", r.getId());
                    m.put("examName", exam != null ? exam.getExamName() : "");
                    m.put("totalScore", r.getTotalScore());
                    m.put("paperTotalScore", paper != null ? paper.getTotalScore() : BigDecimal.ZERO);
                    m.put("isPassed", r.getTotalScore() != null && paper != null &&
                            r.getTotalScore().compareTo(paper.getPassScore()) >= 0);
                    return m;
                }).collect(Collectors.toList());
        data.put("recentScores", recentScores);
        return data;
    }

    @Override
    public List<Map<String, Object>> getStudentScoreTrend(Long userId, Long subjectId) {
        // 查询已批改和缺考的考试记录
        List<ExamRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getUserId, userId).in(ExamRecord::getStatus, 3, 4)
                .orderByAsc(ExamRecord::getSubmitTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord r : records) {
            ExamExam exam = examMapper.selectById(r.getExamId());
            // 已批改(3)和缺考(4)的记录均需成绩已发布才显示
            if ((r.getStatus() == 3 || r.getStatus() == 4) && (exam == null || !Integer.valueOf(1).equals(exam.getScorePublished()))) continue;
            ExamPaper paper = exam != null ? paperMapper.selectById(exam.getPaperId()) : null;
            // 按科目筛选
            if (subjectId != null && paper != null && !subjectId.equals(paper.getSubjectId())) continue;
            EduSubject subject = paper != null ? subjectMapper.selectById(paper.getSubjectId()) : null;
            Map<String, Object> m = new HashMap<>();
            m.put("examName", exam != null ? exam.getExamName() : "");
            m.put("subjectId", paper != null ? paper.getSubjectId() : null);
            m.put("subjectName", subject != null ? subject.getSubjectName() : "");
            m.put("totalScore", r.getTotalScore());
            m.put("paperTotalScore", paper != null ? paper.getTotalScore() : BigDecimal.ZERO);
            m.put("passScore", paper != null ? paper.getPassScore() : BigDecimal.ZERO);
            m.put("submitTime", r.getSubmitTime());
            result.add(m);
        }
        return result;
    }
}
