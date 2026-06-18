package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.constants.ChatConstants;
import com.exam.common.constants.RoleConstants;
import com.exam.common.exception.BusinessException;
import com.exam.entity.ExamRecord;
import com.exam.entity.SysUser;
import com.exam.entity.TeacherClass;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.SysUserMapper;
import com.exam.mapper.TeacherClassMapper;
import com.exam.service.ChatPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * {@link ChatPermissionService} 实现 —— 聊天权限校验单一事实源。
 * <p>
 * 完整继承旧版 {@code ChatServiceImpl.sendMessage} / {@code sendMessagesBatch} 的内联
 * 权限逻辑，字节级保持行为一致（异常文案、校验顺序、边界条件）。
 *
 * <h3>校验顺序</h3>
 * <ol>
 *   <li>senderId / receiverId 非空且不相等</li>
 *   <li>sender 用户存在（status 不校验 —— 禁用用户自己可以感知发消息失败）</li>
 *   <li>receiver 用户存在</li>
 *   <li>receiver status == 1（未被禁用）</li>
 *   <li>sender 是学生时：不能处于考试中（ExamRecord status=1）</li>
 *   <li>角色矩阵校验（管理员通配；教师→学生班级约束；学生↔学生禁止；学生→教师班级约束）</li>
 * </ol>
 *
 * @author Cascade
 * @since L3-bugfix TYPING-001
 */
@Slf4j
@Service
public class ChatPermissionServiceImpl implements ChatPermissionService {

    @Autowired private SysUserMapper userMapper;
    @Autowired private TeacherClassMapper teacherClassMapper;
    @Autowired private ExamRecordMapper examRecordMapper;
    // Redis 可选注入：测试环境 / 部署无 Redis 时允许为 null，走直通非缓存路径
    @Autowired(required = false) private StringRedisTemplate redisTemplate;

    @Override
    public void assertCanChat(Long senderId, Long receiverId) {
        // ====== 1. 基础参数校验 ======
        if (senderId == null || receiverId == null) {
            throw new BusinessException("发送者或接收者 ID 不能为空");
        }
        if (senderId.equals(receiverId)) {
            throw new BusinessException("不能给自己发消息");
        }

        // ====== 2. 用户存在性 ======
        SysUser sender = userMapper.selectById(senderId);
        if (sender == null) throw new BusinessException("用户不存在");

        SysUser receiver = userMapper.selectById(receiverId);
        if (receiver == null) throw new BusinessException("用户不存在");

        // 接收方必须为正常状态（status=1），禁止给已禁用的用户发消息
        if (receiver.getStatus() == null || receiver.getStatus() != 1) {
            throw new BusinessException("对方账号已被禁用，无法发送消息");
        }

        // ====== 3. 防作弊：学生考试中禁止聊天 ======
        if (RoleConstants.STUDENT_ROLE_ID.equals(sender.getRoleId())) {
            long inExam = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getUserId, senderId).eq(ExamRecord::getStatus, 1));
            if (inExam > 0) throw new BusinessException("考试期间禁止聊天");
        }

        // ====== 4. 角色间通信权限矩阵（与 getContacts 保持一致） ======
        Long senderRole = sender.getRoleId();
        Long receiverRole = receiver.getRoleId();

        // 管理员可联系所有人
        if (RoleConstants.ADMIN_ROLE_ID.equals(senderRole)) {
            return;
        }

        if (RoleConstants.TEACHER_ROLE_ID.equals(senderRole)) {
            // 教师 → 教师/管理员：允许（无需额外校验）
            // 教师 → 学生：仅允许联系自己所教班级的学生
            if (RoleConstants.STUDENT_ROLE_ID.equals(receiverRole)) {
                List<TeacherClass> tcList = teacherClassMapper.selectList(
                        new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getTeacherId, senderId));
                Set<Long> myClassIds = tcList.stream()
                        .map(TeacherClass::getClassId)
                        .collect(Collectors.toSet());
                if (receiver.getClassId() == null || !myClassIds.contains(receiver.getClassId())) {
                    throw new BusinessException("只能联系自己所教班级的学生");
                }
            }
            return;
        }

        if (RoleConstants.STUDENT_ROLE_ID.equals(senderRole)) {
            // 学生 → 学生：禁止
            if (RoleConstants.STUDENT_ROLE_ID.equals(receiverRole)) {
                throw new BusinessException("学生之间不允许聊天");
            }
            // 学生 → 教师：仅允许联系教自己班级的教师
            if (RoleConstants.TEACHER_ROLE_ID.equals(receiverRole)) {
                Long studentClassId = sender.getClassId();
                if (studentClassId == null) {
                    throw new BusinessException("您尚未分配班级，无法联系教师");
                }
                List<TeacherClass> tcList = teacherClassMapper.selectList(
                        new LambdaQueryWrapper<TeacherClass>()
                                .eq(TeacherClass::getTeacherId, receiverId)
                                .eq(TeacherClass::getClassId, studentClassId));
                if (tcList.isEmpty()) {
                    throw new BusinessException("只能联系教授您所在班级的教师");
                }
            }
            // 学生 → 管理员：允许（无需额外校验）
        }
        // 其它未知角色组合视为隐式允许（保持与原 sendMessage 逻辑一致）
    }

    @Override
    public boolean canChatSilent(Long senderId, Long receiverId) {
        if (senderId == null || receiverId == null || senderId.equals(receiverId)) {
            return false;
        }

        // ====== Redis 缓存命中路径 ======
        String cacheKey = buildCacheKey(senderId, receiverId);
        String cached = tryGetCache(cacheKey);
        if ("1".equals(cached)) return true;
        if ("0".equals(cached)) return false;

        // ====== 缓存 miss：调用 assertCanChat 并落缓存 ======
        try {
            assertCanChat(senderId, receiverId);
            trySetCache(cacheKey, "1");
            return true;
        } catch (BusinessException e) {
            // 业务层明确的"拒绝"，缓存负结果以防刷探测
            trySetCache(cacheKey, "0");
            return false;
        } catch (Exception e) {
            // DB / 其它不可预期故障：返回 false 但不缓存，下次重试
            log.debug("canChatSilent 非业务异常，降级为 false 不缓存: sender={}, receiver={}, err={}",
                    senderId, receiverId, e.getMessage());
            return false;
        }
    }

    /**
     * 权限缓存 key 方向敏感：sender→receiver 与 receiver→sender 是两条独立规则
     * （如"教师→学生"与"学生→教师"校验路径完全不同）。
     */
    private String buildCacheKey(Long senderId, Long receiverId) {
        return ChatConstants.PERM_CACHE_KEY_PREFIX + "s" + senderId + ":r" + receiverId;
    }

    private String tryGetCache(String key) {
        if (redisTemplate == null) return null;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.debug("权限缓存读取失败，降级为实时校验: key={}, err={}", key, e.getMessage());
            return null;
        }
    }

    private void trySetCache(String key, String value) {
        if (redisTemplate == null) return;
        try {
            // ▼ TTL 随机抖动 ±5s（防缓存雪崩）
            //   背景：一节课 100 个学生同时登录 → Key 几乎同时写入 → 30s 后几乎同时过期
            //         → 瞬间 100 次 DB 查询 → 小型雪崩。加 ±5s 抖动后过期时间分散在 10s 窗口，
            //         DB 峰值 QPS 降低约 90%。
            //   权威依据：阿里云《Redis 缓存穿透/雪崩/并发问题分析》
            //           https://developer.aliyun.com/article/698980
            //   业务无感：学生/教师对 30s 还是 25-35s 完全无感知。
            long ttl = ChatConstants.PERM_CACHE_TTL_SECONDS
                    + ThreadLocalRandom.current().nextInt(-5, 6);  // -5 ~ +5 秒
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("权限缓存写入失败（非关键）: key={}, err={}", key, e.getMessage());
        }
    }
}
