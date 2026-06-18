package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.constants.RoleConstants;
import com.exam.common.exception.BusinessException;
import com.exam.common.result.PageResult;
import com.exam.dto.request.ClassAddRequest;
import com.exam.entity.EduClass;
import com.exam.entity.ExamExam;
import com.exam.entity.SysUser;
import com.exam.entity.TeacherClass;
import com.exam.entity.EduMajor;
import com.exam.mapper.EduClassMapper;
import com.exam.mapper.EduMajorMapper;
import com.exam.mapper.ExamExamMapper;
import com.exam.mapper.SysUserMapper;
import com.exam.mapper.TeacherClassMapper;
import com.exam.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassServiceImpl implements ClassService {

    @Autowired private EduClassMapper classMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private ExamExamMapper examMapper;
    @Autowired private TeacherClassMapper teacherClassMapper;
    @Autowired private EduMajorMapper majorMapper;

    @Override
    public PageResult<Map<String, Object>> listClasses(Integer page, Integer size, String className, String grade, Long majorId) {
        LambdaQueryWrapper<EduClass> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(className)) wrapper.like(EduClass::getClassName, className);
        if (StringUtils.hasText(grade)) wrapper.eq(EduClass::getGrade, grade);
        if (majorId != null) wrapper.eq(EduClass::getMajorId, majorId);
        wrapper.orderByDesc(EduClass::getCreateTime);

        Page<EduClass> p = classMapper.selectPage(new Page<>(page, size), wrapper);
        // 预加载所有教师-班级关联，用于显示每个班级的负责教师
        List<TeacherClass> allTcList = teacherClassMapper.selectList(null);
        Map<Long, List<Long>> classTeacherMap = allTcList.stream()
                .collect(Collectors.groupingBy(TeacherClass::getClassId,
                        Collectors.mapping(TeacherClass::getTeacherId, Collectors.toList())));
        // 预加载所有教师信息
        List<SysUser> allTeachers = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, RoleConstants.TEACHER_ROLE_ID));
        Map<Long, String> teacherNameMap = allTeachers.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        List<EduClass> classes = p.getRecords();

        // 批量预加载专业名称
        Set<Long> majorIds = classes.stream().map(EduClass::getMajorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> majorNameMap = majorIds.isEmpty() ? Collections.emptyMap() :
                majorMapper.selectBatchIds(majorIds).stream()
                        .collect(Collectors.toMap(EduMajor::getId, EduMajor::getMajorName));

        // 批量查询每个班级的学生数
        List<Long> classIds = classes.stream().map(EduClass::getId).collect(Collectors.toList());
        Map<Long, Long> studentCountMap = new HashMap<>();
        if (!classIds.isEmpty()) {
            userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .in(SysUser::getClassId, classIds)
                            .eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID)
                            .select(SysUser::getClassId))
                    .forEach(u -> studentCountMap.merge(u.getClassId(), 1L, Long::sum));
        }

        List<Map<String, Object>> records = classes.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("className", c.getClassName());
            m.put("grade", c.getGrade());
            m.put("description", c.getDescription());
            m.put("majorId", c.getMajorId());
            m.put("majorName", c.getMajorId() != null ? majorNameMap.get(c.getMajorId()) : null);
            m.put("studentCount", studentCountMap.getOrDefault(c.getId(), 0L));
            // 负责教师
            List<Long> teacherIds = classTeacherMap.getOrDefault(c.getId(), Collections.emptyList());
            String teacherNames = teacherIds.stream()
                    .map(tid -> teacherNameMap.getOrDefault(tid, ""))
                    .filter(n -> !n.isEmpty())
                    .collect(Collectors.joining("、"));
            m.put("teacherNames", teacherNames);
            m.put("createTime", c.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public void addClass(ClassAddRequest request) {
        EduClass eduClass = new EduClass();
        eduClass.setClassName(request.getClassName());
        eduClass.setGrade(request.getGrade());
        eduClass.setMajorId(request.getMajorId());
        eduClass.setDescription(request.getDescription());
        classMapper.insert(eduClass);
    }

    @Override
    public void updateClass(Long id, ClassAddRequest request) {
        EduClass eduClass = classMapper.selectById(id);
        if (eduClass == null) throw new BusinessException("班级不存在");
        eduClass.setClassName(request.getClassName());
        eduClass.setGrade(request.getGrade());
        eduClass.setMajorId(request.getMajorId());
        eduClass.setDescription(request.getDescription());
        classMapper.updateById(eduClass);
    }

    @Override
    public void deleteClass(Long id) {
        long studentCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, id).eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID));
        if (studentCount > 0) throw new BusinessException("该班级下有学生，无法删除");

        long examCount = examMapper.selectCount(new LambdaQueryWrapper<ExamExam>().eq(ExamExam::getClassId, id));
        if (examCount > 0) throw new BusinessException("该班级有关联考试，无法删除");

        long teacherCount = teacherClassMapper.selectCount(new LambdaQueryWrapper<TeacherClass>()
                .eq(TeacherClass::getClassId, id));
        if (teacherCount > 0) throw new BusinessException("该班级已分配给教师，无法删除");

        classMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getStudentsByClassId(Long classId) {
        List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, classId).eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID));
        return students.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("username", s.getUsername());
            m.put("realName", s.getRealName());
            m.put("gender", s.getGender());
            m.put("phone", s.getPhone());
            m.put("status", s.getStatus());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getClassDetail(Long id) {
        EduClass c = classMapper.selectById(id);
        if (c == null) throw new BusinessException("班级不存在");
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("className", c.getClassName());
        m.put("grade", c.getGrade());
        m.put("description", c.getDescription());
        m.put("majorId", c.getMajorId());
        if (c.getMajorId() != null) {
            EduMajor major = majorMapper.selectById(c.getMajorId());
            m.put("majorName", major != null ? major.getMajorName() : null);
        } else {
            m.put("majorName", null);
        }
        m.put("createTime", c.getCreateTime());
        // 一次查询学生列表，同时用 size() 得到学生数量，避免重复查询
        List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, id).eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID));
        m.put("studentCount", (long) students.size());
        m.put("students", students.stream().map(s -> {
            Map<String, Object> sm = new HashMap<>();
            sm.put("id", s.getId());
            sm.put("username", s.getUsername());
            sm.put("realName", s.getRealName());
            sm.put("gender", s.getGender());
            sm.put("phone", s.getPhone());
            sm.put("status", s.getStatus());
            return sm;
        }).collect(Collectors.toList()));
        return m;
    }

    @Override
    public List<Map<String, Object>> getAllClasses() {
        return classMapper.selectList(null).stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("className", c.getClassName());
            m.put("majorId", c.getMajorId());
            m.put("grade", c.getGrade());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTeacherClasses(Long teacherId) {
        List<TeacherClass> tcList = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getTeacherId, teacherId));
        if (tcList.isEmpty()) return Collections.emptyList();
        List<Long> classIds = tcList.stream().map(TeacherClass::getClassId).collect(Collectors.toList());
        return classMapper.selectBatchIds(classIds).stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("className", c.getClassName());
            // 与 getAllClasses() / listClasses() 字段对齐：返回班级所属专业 ID，
            // 供前端筛选场景（如考试管理 exam-manage）按班级动态过滤科目下拉。
            m.put("majorId", c.getMajorId());
            return m;
        }).collect(Collectors.toList());
    }
}
