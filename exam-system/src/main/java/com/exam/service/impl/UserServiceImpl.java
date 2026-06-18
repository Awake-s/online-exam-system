package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.exception.BusinessException;
import com.exam.common.result.PageResult;
import com.exam.dto.request.UserAddRequest;
import com.exam.dto.request.UserUpdateRequest;
import com.exam.entity.*;
import com.exam.mapper.*;
import com.exam.service.NotificationOptions;
import com.exam.service.NotificationService;
import com.exam.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.exam.common.constants.RoleConstants;
import com.exam.common.utils.XssUtils;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired private SysUserMapper userMapper;
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private EduClassMapper classMapper;
    @Autowired private TeacherClassMapper teacherClassMapper;
    @Autowired private TeacherSubjectMapper teacherSubjectMapper;
    @Autowired private EduSubjectMapper subjectMapper;
    @Autowired private SubjectMajorMapper subjectMajorMapper;
    @Autowired private ExamQuestionMapper questionMapper;
    @Autowired private ExamPaperMapper paperMapper;
    @Autowired private ExamExamMapper examMapper;
    @Autowired private ExamRecordMapper recordMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private NotificationService notificationService;

    // 任课模型设计（「精准信息源 + 班级候选过滤」双层）：
    //   1) teacher_class 是「教师能管哪些班级」的信息源
    //   2) teacher_subject 是「教师实际任课哪些课」的信息源
    //   3) 为避免「管 2022级计算机班、却被挂上 2024级电子专业的课」这种错挂,
    //      增加约束: teacher_subject 里的 subject.(grade, major_id) 必须 ∈
    //      教师任课班级的 (grade, major_id) 集合。下面 pairKey 工具为约束校验服务。
    private static String pairKey(String grade, Long majorId) {
        return grade + "|" + majorId;
    }

    @Override
    public PageResult<Map<String, Object>> listUsers(Integer page, Integer size, String username, String realName, Long roleId, Long classId, Long majorId, Long subjectId, String grade, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) wrapper.like(SysUser::getUsername, username);
        if (StringUtils.hasText(realName)) wrapper.like(SysUser::getRealName, realName);
        if (roleId != null) wrapper.eq(SysUser::getRoleId, roleId);
        if (classId != null) wrapper.eq(SysUser::getClassId, classId);
        if (status != null) wrapper.eq(SysUser::getStatus, status);

        boolean includeStudent = roleId == null || RoleConstants.STUDENT_ROLE_ID.equals(roleId);
        boolean includeTeacher = roleId == null || RoleConstants.TEACHER_ROLE_ID.equals(roleId);

        // 年级筛选: 学生用 class.grade、教师经 teacher_class 反查所在年级
        if (StringUtils.hasText(grade)) {
            List<Long> classIdsOfGrade = classMapper.selectList(
                    new LambdaQueryWrapper<EduClass>()
                            .eq(EduClass::getGrade, grade)
                            .select(EduClass::getId))
                    .stream().map(EduClass::getId).collect(Collectors.toList());

            Set<Long> candidateIds = new HashSet<>();
            if (!classIdsOfGrade.isEmpty()) {
                if (includeStudent) {
                    userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .in(SysUser::getClassId, classIdsOfGrade)
                            .eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID)
                            .select(SysUser::getId))
                            .forEach(u -> candidateIds.add(u.getId()));
                }
                if (includeTeacher) {
                    teacherClassMapper.selectList(new LambdaQueryWrapper<TeacherClass>()
                            .in(TeacherClass::getClassId, classIdsOfGrade))
                            .forEach(tc -> candidateIds.add(tc.getTeacherId()));
                }
            }
            if (candidateIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, (long) page, (long) size);
            }
            wrapper.in(SysUser::getId, candidateIds);
        }

        // 专业筛选：学生用 classId、教师经 teacher_class 反查，按 roleId 取交集
        if (majorId != null) {
            List<Long> classIdsOfMajor = classMapper.selectList(
                    new LambdaQueryWrapper<EduClass>()
                            .eq(EduClass::getMajorId, majorId)
                            .select(EduClass::getId))
                    .stream().map(EduClass::getId).collect(Collectors.toList());

            Set<Long> candidateIds = new HashSet<>();
            if (!classIdsOfMajor.isEmpty()) {
                if (includeStudent) {
                    userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .in(SysUser::getClassId, classIdsOfMajor)
                            .eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID)
                            .select(SysUser::getId))
                            .forEach(u -> candidateIds.add(u.getId()));
                }
                if (includeTeacher) {
                    teacherClassMapper.selectList(new LambdaQueryWrapper<TeacherClass>()
                            .in(TeacherClass::getClassId, classIdsOfMajor))
                            .forEach(tc -> candidateIds.add(tc.getTeacherId()));
                }
            }
            if (candidateIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, (long) page, (long) size);
            }
            wrapper.in(SysUser::getId, candidateIds);
        }

        // 科目筛选：
        //  - 教师: 直接查 teacher_subject 精准反查 (任课该课的教师)
        //  - 学生: subject_major → class → user (反查选课该科目的学生)
        if (subjectId != null) {
            Set<Long> candidateIds = new HashSet<>();
            if (includeTeacher) {
                teacherSubjectMapper.selectList(new LambdaQueryWrapper<TeacherSubject>()
                                .eq(TeacherSubject::getSubjectId, subjectId))
                        .forEach(ts -> candidateIds.add(ts.getTeacherId()));
            }
            if (includeStudent) {
                List<Long> majorIdsOfSubject = subjectMajorMapper.selectList(
                        new LambdaQueryWrapper<SubjectMajor>()
                                .eq(SubjectMajor::getSubjectId, subjectId))
                        .stream().map(SubjectMajor::getMajorId).collect(Collectors.toList());
                if (!majorIdsOfSubject.isEmpty()) {
                    List<Long> classIdsOfSubject = classMapper.selectList(
                            new LambdaQueryWrapper<EduClass>()
                                    .in(EduClass::getMajorId, majorIdsOfSubject)
                                    .select(EduClass::getId))
                            .stream().map(EduClass::getId).collect(Collectors.toList());
                    if (!classIdsOfSubject.isEmpty()) {
                        userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                                .in(SysUser::getClassId, classIdsOfSubject)
                                .eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID)
                                .select(SysUser::getId))
                                .forEach(u -> candidateIds.add(u.getId()));
                    }
                }
            }
            if (candidateIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, (long) page, (long) size);
            }
            wrapper.in(SysUser::getId, candidateIds);
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 预加载角色和班级
        Map<Long, String> roleMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName));
        Map<Long, String> classMap = classMapper.selectList(null).stream()
                .collect(Collectors.toMap(EduClass::getId, EduClass::getClassName));

        // 预加载教师-班级关联
        List<TeacherClass> allTc = teacherClassMapper.selectList(null);
        Map<Long, List<Long>> teacherClassIdsMap = allTc.stream()
                .collect(Collectors.groupingBy(TeacherClass::getTeacherId,
                        Collectors.mapping(TeacherClass::getClassId, Collectors.toList())));

        // 预加载教师-科目关联 (精准信息源) + 科目名查表
        List<TeacherSubject> allTs = teacherSubjectMapper.selectList(null);
        Map<Long, List<Long>> teacherSubjectIdsMap = allTs.stream()
                .collect(Collectors.groupingBy(TeacherSubject::getTeacherId,
                        Collectors.mapping(TeacherSubject::getSubjectId, Collectors.toList())));
        Map<Long, String> subjectNameMap = subjectMapper.selectList(null).stream()
                .collect(Collectors.toMap(EduSubject::getId, EduSubject::getSubjectName));

        List<Map<String, Object>> records = p.getRecords().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("avatar", u.getAvatar());
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone());
            m.put("gender", u.getGender());
            m.put("roleId", u.getRoleId());
            m.put("roleName", roleMap.getOrDefault(u.getRoleId(), ""));
            m.put("classId", u.getClassId());
            m.put("className", u.getClassId() != null ? classMap.get(u.getClassId()) : null);
            // 教师的班级列表 + 精准任课科目列表
            if (RoleConstants.TEACHER_ROLE_ID.equals(u.getRoleId())) {
                List<Long> tcIds = teacherClassIdsMap.getOrDefault(u.getId(), Collections.emptyList());
                m.put("classIds", tcIds);
                String classNames = tcIds.stream()
                        .map(cid -> classMap.getOrDefault(cid, ""))
                        .filter(n -> !n.isEmpty())
                        .collect(Collectors.joining("、"));
                m.put("className", classNames.isEmpty() ? null : classNames);
                // 教师精准任课科目 (精准信息源: teacher_subject)
                List<Long> tsIds = teacherSubjectIdsMap.getOrDefault(u.getId(), Collections.emptyList());
                m.put("subjectIds", tsIds);
                String subjectNames = tsIds.stream()
                        .map(sid -> subjectNameMap.getOrDefault(sid, ""))
                        .filter(n -> !n.isEmpty())
                        .distinct()
                        .collect(Collectors.joining("、"));
                m.put("subjectNames", subjectNames.isEmpty() ? null : subjectNames);
            }
            m.put("status", u.getStatus());
            m.put("createTime", u.getCreateTime());
            return m;
        }).collect(Collectors.toList());

        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    @Transactional
    public void addUser(UserAddRequest request) {
        // 检查用户名唯一
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) throw new BusinessException("用户名已存在");

        // XSS防护：对用户名和姓名进行HTML转义
        if (XssUtils.containsXss(request.getUsername())) {
            throw new BusinessException("用户名包含非法字符");
        }
        if (XssUtils.containsXss(request.getRealName())) {
            throw new BusinessException("姓名包含非法字符");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRoleId(request.getRoleId());
        user.setClassId(request.getClassId());
        user.setGender(request.getGender());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        userMapper.insert(user);

        // 教师角色: 写入班级关联 + 精准科目关联 (两者均为信息源)
        if (RoleConstants.TEACHER_ROLE_ID.equals(request.getRoleId())) {
            // 1) 写入 teacher_class
            if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
                for (Long classId : request.getClassIds()) {
                    TeacherClass tc = new TeacherClass();
                    tc.setTeacherId(user.getId());
                    tc.setClassId(classId);
                    teacherClassMapper.insert(tc);
                }
            }
            // 2) 写入 teacher_subject (仅保留校验通过的科目, 避免跨届/跨专业错挂)
            if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
                List<Long> validSubjectIds = filterSubjectsByCandidatePool(
                        request.getSubjectIds(), request.getClassIds());
                for (Long subjectId : validSubjectIds) {
                    TeacherSubject ts = new TeacherSubject();
                    ts.setTeacherId(user.getId());
                    ts.setSubjectId(subjectId);
                    teacherSubjectMapper.insert(ts);
                }
            }
        }

        // 通知延迟到事务提交后执行，避免事务回滚后「欢迎通知/新用户通知」已外发。
        // 对齐 MarkingServiceImpl / StudentExamServiceImpl / ExamServiceImpl 已有的 afterCommit 模式。
        final String roleName = RoleConstants.TEACHER_ROLE_ID.equals(request.getRoleId()) ? "教师" :
                RoleConstants.STUDENT_ROLE_ID.equals(request.getRoleId()) ? "学生" : "管理员";
        final Long finalUserId = user.getId();
        final String finalRealName = request.getRealName() != null ? request.getRealName() : request.getUsername();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 事务已提交，通知失败不可使主业务回滚；仅记录日志
                    try {
                        doSendAddUserNotifications(finalUserId, roleName, finalRealName);
                    } catch (Exception e) {
                        log.error("新增用户后发送欢迎通知失败 userId={} realName={}", finalUserId, finalRealName, e);
                    }
                }
            });
        } else {
            try {
                doSendAddUserNotifications(finalUserId, roleName, finalRealName);
            } catch (Exception e) {
                log.error("新增用户后发送欢迎通知失败（无事务上下文）userId={}", finalUserId, e);
            }
        }
    }

    @Override
    @Transactional
    public void updateUser(UserUpdateRequest request) {
        SysUser user = userMapper.selectById(request.getId());
        if (user == null) throw new BusinessException("用户不存在");

        if (request.getRealName() != null) {
            if (XssUtils.containsXss(request.getRealName())) {
                throw new BusinessException("姓名包含非法字符");
            }
            user.setRealName(request.getRealName());
        }
        if (request.getRoleId() != null) user.setRoleId(request.getRoleId());
        if (request.getClassId() != null) user.setClassId(request.getClassId());
        // 非学生角色不需要 classId
        if (request.getRoleId() != null && !RoleConstants.STUDENT_ROLE_ID.equals(request.getRoleId())) {
            user.setClassId(null);
        }
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        userMapper.updateById(user);

        // 教师角色: 同时重建班级关联 + 精准科目关联 (事务中原子替换)
        if (RoleConstants.TEACHER_ROLE_ID.equals(request.getRoleId())) {
            // 1) 重建 teacher_class
            teacherClassMapper.delete(new LambdaQueryWrapper<TeacherClass>()
                    .eq(TeacherClass::getTeacherId, user.getId()));
            if (request.getClassIds() != null) {
                for (Long classId : request.getClassIds()) {
                    TeacherClass tc = new TeacherClass();
                    tc.setTeacherId(user.getId());
                    tc.setClassId(classId);
                    teacherClassMapper.insert(tc);
                }
            }
            // 2) 重建 teacher_subject (校验后写入)
            teacherSubjectMapper.delete(new LambdaQueryWrapper<TeacherSubject>()
                    .eq(TeacherSubject::getTeacherId, user.getId()));
            if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
                List<Long> validSubjectIds = filterSubjectsByCandidatePool(
                        request.getSubjectIds(), request.getClassIds());
                for (Long subjectId : validSubjectIds) {
                    TeacherSubject ts = new TeacherSubject();
                    ts.setTeacherId(user.getId());
                    ts.setSubjectId(subjectId);
                    teacherSubjectMapper.insert(ts);
                }
            }
        } else {
            // 非教师角色, 清除两个关联表
            teacherClassMapper.delete(new LambdaQueryWrapper<TeacherClass>()
                    .eq(TeacherClass::getTeacherId, user.getId()));
            teacherSubjectMapper.delete(new LambdaQueryWrapper<TeacherSubject>()
                    .eq(TeacherSubject::getTeacherId, user.getId()));
        }
    }

    /**
     * 「班级候选池」科目过滤器: 同时是校验与清洗。
     * 只保留 subjectId 严格处于任何所选班级的 (grade, major_id) 集合中的项，
     * 避免进程被绕过前端直接 POST 异常组合导致科目页"任课教师"列出现错挂。
     */
    private List<Long> filterSubjectsByCandidatePool(List<Long> subjectIds, List<Long> classIds) {
        if (subjectIds == null || subjectIds.isEmpty()) return Collections.emptyList();
        if (classIds == null || classIds.isEmpty()) return Collections.emptyList();
        // 收集所选班级的 (grade, major_id) 集合
        List<EduClass> classes = classMapper.selectBatchIds(classIds);
        Set<String> allowedPairs = new HashSet<>();
        for (EduClass c : classes) {
            if (c.getGrade() != null && c.getMajorId() != null) {
                allowedPairs.add(pairKey(c.getGrade(), c.getMajorId()));
            }
        }
        if (allowedPairs.isEmpty()) return Collections.emptyList();
        // 一次性查出传入 subjectIds 的元数据, 校验 (grade, major_id) 是否命中
        List<EduSubject> subjects = subjectMapper.selectBatchIds(subjectIds);
        return subjects.stream()
                .filter(s -> s.getGrade() != null && s.getMajorId() != null
                        && allowedPairs.contains(pairKey(s.getGrade(), s.getMajorId())))
                .map(EduSubject::getId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) throw new BusinessException("不能删除自己");
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");

        // 教师检查
        if (RoleConstants.TEACHER_ROLE_ID.equals(user.getRoleId())) {
            long qCount = questionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getCreatorId, id).eq(ExamQuestion::getDeleted, 0));
            long pCount = paperMapper.selectCount(new LambdaQueryWrapper<ExamPaper>().eq(ExamPaper::getCreatorId, id));
            long eCount = examMapper.selectCount(new LambdaQueryWrapper<ExamExam>().eq(ExamExam::getCreatorId, id));
            if (qCount + pCount + eCount > 0) throw new BusinessException("该教师有关联数据，无法删除");
            // 删除教师-班级关联 + 精准任课科目关联
            teacherClassMapper.delete(new LambdaQueryWrapper<TeacherClass>()
                    .eq(TeacherClass::getTeacherId, id));
            teacherSubjectMapper.delete(new LambdaQueryWrapper<TeacherSubject>()
                    .eq(TeacherSubject::getTeacherId, id));
        }
        // 学生检查
        if (RoleConstants.STUDENT_ROLE_ID.equals(user.getRoleId())) {
            long rCount = recordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getUserId, id));
            if (rCount > 0) throw new BusinessException("该学生有考试记录，无法删除");
        }
        userMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值无效，只允许0(禁用)或1(启用)");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        // 禁止禁用管理员账号（防止所有管理员被锁定）
        if (status == 0 && RoleConstants.ADMIN_ROLE_ID.equals(user.getRoleId())) {
            long adminCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRoleId, RoleConstants.ADMIN_ROLE_ID).eq(SysUser::getStatus, 1));
            if (adminCount <= 1) {
                throw new BusinessException("系统至少需要保留一个启用状态的管理员");
            }
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
    }

    /**
     * 新增用户后的通知推送。
     * <p>提取为独立方法以便在 {@code addUser} 的 afterCommit 回调中复用，
     * 避免在主事务提交前误发通知（事务回滚后通知已外发会造成数据不一致）。
     * <p>对齐 {@link ExamServiceImpl} 中 {@code doSendPublishExamNotifications} 的设计模式。
     * <p>使用的 type 已在 {@link com.exam.common.constants.NotificationTypeWhitelist} 中注册。
     *
     * @param userId   新建用户 ID（欢迎通知的接收者）
     * @param roleName 角色中文名（"教师"/"学生"/"管理员"），用于通知文案
     * @param realName 新用户真实姓名，用于通知文案
     */
    private void doSendAddUserNotifications(Long userId, String roleName, String realName) {
        // 1) 给新用户本人发账号创建欢迎通知
        notificationService.notifyUser(userId, "ACCOUNT_CREATED",
                "欢迎加入考试系统",
                "您的" + roleName + "账号已创建成功，初始密码为 123456，请尽快登录并修改密码。",
                "user", userId);

        // 2) 给所有管理员发新用户加入通知（便于运维感知账号增量）
        notificationService.notifyAdmins("USER_CREATED",
                "新" + roleName + "加入",
                realName + " 已注册为" + roleName + "，账号已激活。",
                "user", userId);
    }
}
