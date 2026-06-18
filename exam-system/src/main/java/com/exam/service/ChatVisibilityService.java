package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.constants.RoleConstants;
import com.exam.entity.SysUser;
import com.exam.entity.TeacherClass;
import com.exam.mapper.SysUserMapper;
import com.exam.mapper.TeacherClassMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户可见性服务 — 用于在线状态定向推送。
 * <p>
 * 设计目的：解决 M4 漏洞（在线状态全局广播导致用户枚举 + 行为追踪）。
 * <p>
 * 业务规则（与 {@code ChatService.getContacts} 的联系人可见性保持对称）：
 * <ul>
 *   <li><b>管理员上下线</b> → 所有用户可见</li>
 *   <li><b>教师上下线</b> → 管理员 + 所有教师 + 自己所教班级的学生可见</li>
 *   <li><b>学生上下线</b> → 管理员 + 教自己班级的教师可见（学生之间互不可见）</li>
 * </ul>
 * <p>
 * 独立 Service 而非放在 {@code ChatServiceImpl}：
 * 因为 {@code ChatServiceImpl} 依赖 {@code WebSocketEventListener}，
 * 若 {@code WebSocketEventListener} 再依赖 {@code ChatService} 会造成循环依赖。
 *
 * @author Cascade
 * @see <a href="https://owasp.org/Top10/A01_2021-Broken_Access_Control/">OWASP Privacy Risks</a>
 */
@Slf4j
@Service
public class ChatVisibilityService {

    @Autowired private SysUserMapper userMapper;
    @Autowired private TeacherClassMapper teacherClassMapper;

    /**
     * 计算有资格看到指定用户上下线状态的观察者 ID 列表。
     *
     * @param userId 上下线的用户 ID
     * @return 观察者 ID 集合（包含 userId 自己，用于多标签页同步）
     */
    public Set<Long> getStatusWatchers(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) return Collections.emptySet();

        Set<Long> watchers = new HashSet<>();
        // 用户自己也要收到（多标签页/多设备同步）
        watchers.add(userId);

        // 所有激活的管理员都能看到任何人的上下线
        addActiveUsersByRole(watchers, RoleConstants.ADMIN_ROLE_ID);

        Long roleId = user.getRoleId();
        if (RoleConstants.ADMIN_ROLE_ID.equals(roleId)) {
            // 管理员上下线 → 所有激活用户可见
            List<SysUser> all = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getStatus, 1)
                    .select(SysUser::getId));
            all.forEach(u -> watchers.add(u.getId()));
        } else if (RoleConstants.TEACHER_ROLE_ID.equals(roleId)) {
            // 教师上下线 → 所有教师可见
            addActiveUsersByRole(watchers, RoleConstants.TEACHER_ROLE_ID);
            // + 自己所教班级的学生可见
            addStudentsOfTeacher(watchers, userId);
        } else if (RoleConstants.STUDENT_ROLE_ID.equals(roleId)) {
            // 学生上下线 → 教自己班级的教师可见
            if (user.getClassId() != null) {
                addTeachersOfClass(watchers, user.getClassId());
            }
        }

        return watchers;
    }

    private void addActiveUsersByRole(Set<Long> watchers, Long roleId) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, roleId)
                .eq(SysUser::getStatus, 1)
                .select(SysUser::getId));
        users.forEach(u -> watchers.add(u.getId()));
    }

    private void addStudentsOfTeacher(Set<Long> watchers, Long teacherId) {
        List<TeacherClass> tcs = teacherClassMapper.selectList(new LambdaQueryWrapper<TeacherClass>()
                .eq(TeacherClass::getTeacherId, teacherId));
        Set<Long> classIds = tcs.stream().map(TeacherClass::getClassId).collect(Collectors.toSet());
        if (classIds.isEmpty()) return;
        List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getClassId, classIds)
                .eq(SysUser::getRoleId, RoleConstants.STUDENT_ROLE_ID)
                .eq(SysUser::getStatus, 1)
                .select(SysUser::getId));
        students.forEach(s -> watchers.add(s.getId()));
    }

    private void addTeachersOfClass(Set<Long> watchers, Long classId) {
        List<TeacherClass> tcs = teacherClassMapper.selectList(new LambdaQueryWrapper<TeacherClass>()
                .eq(TeacherClass::getClassId, classId));
        Set<Long> teacherIds = tcs.stream().map(TeacherClass::getTeacherId).collect(Collectors.toSet());
        if (teacherIds.isEmpty()) return;
        List<SysUser> teachers = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, teacherIds)
                .eq(SysUser::getStatus, 1)
                .select(SysUser::getId));
        teachers.forEach(t -> watchers.add(t.getId()));
    }
}
