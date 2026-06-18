package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.exception.BusinessException;
import com.exam.common.result.PageResult;
import com.exam.dto.request.SubjectAddRequest;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubjectServiceImpl implements SubjectService {

    @Autowired private EduSubjectMapper subjectMapper;
    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private EduMajorMapper majorMapper;
    @Autowired private SubjectMajorMapper subjectMajorMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private EduClassMapper classMapper;
    @Autowired private TeacherSubjectMapper teacherSubjectMapper;

    // 任课模型: teacher_subject 作为「教师实际任课哪些课」的精准信息源。
    // 科目页「任课教师」列、教师端「可管理的科目」均以此表为准；
    // 班级关联 (teacher_class) 仅作为「添加/编辑教师时的科目候选过滤」，不参与查询。

    @Override
    public PageResult<Map<String, Object>> listSubjects(Integer page, Integer size, String subjectName,
                                                          String grade, Long majorId) {
        LambdaQueryWrapper<EduSubject> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(subjectName)) wrapper.like(EduSubject::getSubjectName, subjectName);
        if (StringUtils.hasText(grade))       wrapper.eq(EduSubject::getGrade, grade);
        if (majorId != null)                  wrapper.eq(EduSubject::getMajorId, majorId);
        // v7 KISS 减肥: 不再按 courseType 筛选 / 不再按 semester 排序 (都是教务化字段)
        wrapper.orderByAsc(EduSubject::getGrade)
               .orderByAsc(EduSubject::getMajorId)
               .orderByAsc(EduSubject::getId);

        Page<EduSubject> p = subjectMapper.selectPage(new Page<>(page, size), wrapper);
        List<EduSubject> subjects = p.getRecords();
        if (subjects.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, p.getCurrent(), p.getSize());
        }

        // 批量收集本页所有科目ID
        List<Long> subjectIds = subjects.stream().map(EduSubject::getId).collect(Collectors.toList());

        // 批量查询题目数量（按 subject_id 分组）
        Map<Long, Long> questionCountMap = new HashMap<>();
        List<ExamQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .in(ExamQuestion::getSubjectId, subjectIds)
                        .eq(ExamQuestion::getDeleted, 0)
                        .select(ExamQuestion::getSubjectId));
        questions.forEach(q -> questionCountMap.merge(q.getSubjectId(), 1L, Long::sum));

        // 批量查询科目-专业关联
        List<SubjectMajor> allSmList = subjectMajorMapper.selectList(
                new LambdaQueryWrapper<SubjectMajor>().in(SubjectMajor::getSubjectId, subjectIds));
        Map<Long, List<Long>> subjectMajorIdsMap = allSmList.stream()
                .collect(Collectors.groupingBy(SubjectMajor::getSubjectId,
                        Collectors.mapping(SubjectMajor::getMajorId, Collectors.toList())));

        // 批量查询所有涉及的专业
        Set<Long> allMajorIds = allSmList.stream().map(SubjectMajor::getMajorId).collect(Collectors.toSet());
        Map<Long, String> majorNameMap = new HashMap<>();
        if (!allMajorIds.isEmpty()) {
            majorMapper.selectBatchIds(allMajorIds)
                    .forEach(m -> majorNameMap.put(m.getId(), m.getMajorName()));
        }

        // —— 任课教师查询 (精准信息源: teacher_subject) ——
        // 按本页出现的 subjectIds 精准反查所有教师 id, 避免全表扫描。
        Map<Long, List<Long>> subjectTeacherIdsMap = new HashMap<>();
        Set<Long> allTeacherIds = new HashSet<>();
        List<TeacherSubject> tsList = teacherSubjectMapper.selectList(
                new LambdaQueryWrapper<TeacherSubject>().in(TeacherSubject::getSubjectId, subjectIds));
        for (TeacherSubject ts : tsList) {
            subjectTeacherIdsMap.computeIfAbsent(ts.getSubjectId(), k -> new ArrayList<>()).add(ts.getTeacherId());
            allTeacherIds.add(ts.getTeacherId());
        }
        Map<Long, String> teacherNameMap = new HashMap<>();
        if (!allTeacherIds.isEmpty()) {
            userMapper.selectBatchIds(allTeacherIds)
                    .forEach(u -> teacherNameMap.put(u.getId(), u.getRealName()));
        }

        // 收集本页所有 edu_subject.major_id（新模型字段），批量查询专业名
        Set<Long> directMajorIds = subjects.stream()
                .map(EduSubject::getMajorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!directMajorIds.isEmpty() && !majorNameMap.keySet().containsAll(directMajorIds)) {
            majorMapper.selectBatchIds(directMajorIds)
                    .forEach(em -> majorNameMap.put(em.getId(), em.getMajorName()));
        }

        // 组装结果
        List<Map<String, Object>> records = subjects.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("subjectName", s.getSubjectName());
            m.put("description", s.getDescription());
            m.put("questionCount", questionCountMap.getOrDefault(s.getId(), 0L));
            // v7 KISS 减肥: 删 courseType / credit / semester 三个教务化字段; hours/examType 仅作历史籍贯保留
            m.put("grade", s.getGrade());
            m.put("majorId", s.getMajorId());
            m.put("hours", s.getHours());
            m.put("examType", s.getExamType());

            // —— 所属专业：优先取新字段 major_id；fallback 到旧多对多 subject_major ——
            if (s.getMajorId() != null) {
                m.put("majorIds", Collections.singletonList(s.getMajorId()));
                String mn = majorNameMap.getOrDefault(s.getMajorId(), "");
                m.put("majorNames", mn.isEmpty() ? Collections.emptyList() : Collections.singletonList(mn));
                m.put("majorName", mn.isEmpty() ? null : mn);
            } else {
                List<Long> mIds = subjectMajorIdsMap.getOrDefault(s.getId(), Collections.emptyList());
                m.put("majorIds", mIds);
                if (!mIds.isEmpty()) {
                    List<String> majorNames = mIds.stream()
                            .map(id -> majorNameMap.getOrDefault(id, ""))
                            .filter(n -> !n.isEmpty())
                            .collect(Collectors.toList());
                    m.put("majorNames", majorNames);
                    m.put("majorName", String.join("、", majorNames));
                } else {
                    m.put("majorNames", Collections.emptyList());
                    m.put("majorName", null);
                }
            }
            // —— 任课教师 (精准信息源: teacher_subject) ——
            List<Long> tIds = subjectTeacherIdsMap.getOrDefault(s.getId(), Collections.emptyList());
            if (!tIds.isEmpty()) {
                List<String> teacherNames = tIds.stream()
                        .map(id -> teacherNameMap.getOrDefault(id, ""))
                        .filter(n -> !n.isEmpty())
                        .sorted()
                        .collect(Collectors.toList());
                m.put("teacherNames", teacherNames);
                m.put("teacherName", String.join("、", teacherNames));
            } else {
                m.put("teacherNames", Collections.emptyList());
                m.put("teacherName", null);
            }
            m.put("createTime", s.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    @Transactional
    public void addSubject(SubjectAddRequest request) {
        EduSubject subject = new EduSubject();
        subject.setSubjectName(request.getSubjectName());
        subject.setDescription(request.getDescription());
        // v7 KISS 减肥: courseType / credit / semester 已删; hours/examType 允许 null 透传
        subject.setGrade(request.getGrade());
        subject.setMajorId(request.getMajorId());
        subject.setHours(request.getHours());
        subject.setExamType(request.getExamType());
        subjectMapper.insert(subject);
        // 兼容旧多对多关联（仅当 majorId 未传且 majorIds 有值时使用）
        if (request.getMajorId() == null) {
            saveSubjectMajors(subject.getId(), request.getMajorIds());
        }
    }

    @Override
    @Transactional
    public void updateSubject(Long id, SubjectAddRequest request) {
        EduSubject subject = subjectMapper.selectById(id);
        if (subject == null) throw new BusinessException("科目不存在");
        subject.setSubjectName(request.getSubjectName());
        subject.setDescription(request.getDescription());
        // v7 KISS 减肥: courseType / credit / semester 已删; hours/examType 允许 null 透传
        subject.setGrade(request.getGrade());
        subject.setMajorId(request.getMajorId());
        subject.setHours(request.getHours());
        subject.setExamType(request.getExamType());
        subjectMapper.updateById(subject);
        // 维护旧多对多关联（仅当具体课程字段未启用时）
        if (request.getMajorId() == null) {
            subjectMajorMapper.delete(new LambdaQueryWrapper<SubjectMajor>()
                    .eq(SubjectMajor::getSubjectId, id));
            saveSubjectMajors(id, request.getMajorIds());
        }
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        long qCount = questionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getSubjectId, id).eq(ExamQuestion::getDeleted, 0));
        if (qCount > 0) throw new BusinessException("该科目下有题目，无法删除");
        // 删除科目-专业关联（CASCADE会自动处理，但显式删除更安全）
        subjectMajorMapper.delete(new LambdaQueryWrapper<SubjectMajor>()
                .eq(SubjectMajor::getSubjectId, id));
        subjectMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getAllSubjects() {
        List<EduSubject> subjects = subjectMapper.selectList(null);
        if (subjects.isEmpty()) return Collections.emptyList();

        // 批量查询所有科目-专业关联，按 subjectId 分组（兼容旧抽象科目）
        List<SubjectMajor> allSmList = subjectMajorMapper.selectList(null);
        Map<Long, List<Long>> subjectMajorMap = allSmList.stream()
                .collect(Collectors.groupingBy(SubjectMajor::getSubjectId,
                        Collectors.mapping(SubjectMajor::getMajorId, Collectors.toList())));

        return subjects.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("subjectName", s.getSubjectName());
            m.put("grade", s.getGrade());
            m.put("majorId", s.getMajorId());
            // v7 KISS 减肥: 删 courseType / credit / semester
            // 优先用新字段 major_id 单值；fallback 到旧多对多
            if (s.getMajorId() != null) {
                m.put("majorIds", Collections.singletonList(s.getMajorId()));
            } else {
                m.put("majorIds", subjectMajorMap.getOrDefault(s.getId(), Collections.emptyList()));
            }
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAllSubjectsByTeacher(Long teacherId) {
        // 教师可管理的科目 = 其精准任课的科目 (teacher_subject 为准)。
        // 不再从班级衡生 —— 避免「教某班」被错估为「能出该班全部课的题」。
        List<TeacherSubject> tsList = teacherSubjectMapper.selectList(
                new LambdaQueryWrapper<TeacherSubject>().eq(TeacherSubject::getTeacherId, teacherId));
        if (tsList.isEmpty()) return Collections.emptyList();

        Set<Long> subjectIds = tsList.stream().map(TeacherSubject::getSubjectId).collect(Collectors.toSet());
        List<EduSubject> subjects = subjectMapper.selectBatchIds(subjectIds);

        // 与 getAllSubjects() 字段对齐：批量加载科目-专业多对多关联，
        // 让前端筛选场景（exam-manage 切换班级时联动过滤科目下拉）能正确处理
        // 跨专业开设的公共基础课（如「高等数学」同属计算机+软件工程多个专业）。
        List<SubjectMajor> smList = subjectMajorMapper.selectList(
                new LambdaQueryWrapper<SubjectMajor>().in(SubjectMajor::getSubjectId, subjectIds));
        Map<Long, List<Long>> subjectMajorMap = smList.stream()
                .collect(Collectors.groupingBy(SubjectMajor::getSubjectId,
                        Collectors.mapping(SubjectMajor::getMajorId, Collectors.toList())));

        // 批量加载所有涉及专业的名称（单值 majorId + 多对多 SubjectMajor 的并集），
        // 供前端「按需消歧」算法做同名科目的专业维度区分（如多个专业的「思想道德与法治」）。
        Set<Long> allMajorIds = new HashSet<>();
        smList.forEach(sm -> allMajorIds.add(sm.getMajorId()));
        subjects.stream().map(EduSubject::getMajorId).filter(Objects::nonNull).forEach(allMajorIds::add);
        Map<Long, String> majorNameMap = new HashMap<>();
        if (!allMajorIds.isEmpty()) {
            majorMapper.selectBatchIds(allMajorIds)
                    .forEach(em -> majorNameMap.put(em.getId(), em.getMajorName()));
        }

        return subjects.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("subjectName", s.getSubjectName());
            m.put("grade", s.getGrade());
            m.put("majorId", s.getMajorId());
            // 优先用新字段 major_id 单值；fallback 到旧多对多关联表（与 getAllSubjects 完全一致）
            if (s.getMajorId() != null) {
                m.put("majorIds", Collections.singletonList(s.getMajorId()));
                String mn = majorNameMap.getOrDefault(s.getMajorId(), "");
                m.put("majorNames", mn.isEmpty() ? Collections.emptyList() : Collections.singletonList(mn));
                m.put("majorName", mn.isEmpty() ? null : mn);
            } else {
                List<Long> mIds = subjectMajorMap.getOrDefault(s.getId(), Collections.emptyList());
                m.put("majorIds", mIds);
                if (!mIds.isEmpty()) {
                    List<String> majorNames = mIds.stream()
                            .map(id -> majorNameMap.getOrDefault(id, ""))
                            .filter(n -> !n.isEmpty())
                            .collect(Collectors.toList());
                    m.put("majorNames", majorNames);
                    m.put("majorName", majorNames.isEmpty() ? null : String.join("、", majorNames));
                } else {
                    m.put("majorNames", Collections.emptyList());
                    m.put("majorName", null);
                }
            }
            return m;
        }).collect(Collectors.toList());
    }

    private void saveSubjectMajors(Long subjectId, List<Long> majorIds) {
        if (majorIds == null || majorIds.isEmpty()) return;
        // 去重
        List<Long> uniqueIds = majorIds.stream().distinct().collect(Collectors.toList());
        // 校验所有 majorId 是否存在
        List<EduMajor> existMajors = majorMapper.selectBatchIds(uniqueIds);
        if (existMajors.size() != uniqueIds.size()) {
            Set<Long> existIds = existMajors.stream().map(EduMajor::getId).collect(Collectors.toSet());
            List<Long> invalidIds = uniqueIds.stream().filter(id -> !existIds.contains(id)).collect(Collectors.toList());
            throw new BusinessException("以下专业ID不存在：" + invalidIds);
        }
        for (Long majorId : uniqueIds) {
            SubjectMajor sm = new SubjectMajor();
            sm.setSubjectId(subjectId);
            sm.setMajorId(majorId);
            subjectMajorMapper.insert(sm);
        }
    }
}
