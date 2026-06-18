package com.exam.service;

import com.exam.common.constants.RoleConstants;

/**
 * 通知 action_url 生成器（E1 架构升级 - 前后端解耦）。
 * <p>
 * <b>设计目的</b>：把"通知点击后跳转到哪个路由"的决策从前端硬编码迁移到后端。
 * 后端生成的绝对路由（如 {@code /student/exam?id=123}）前端直接 {@code router.push(url)} 即可，
 * 彻底消除前端 60 行 {@code if-else} 硬编码。
 * <p>
 * <b>为什么是纯函数</b>：
 * 不依赖 DB 查询，避免额外 IO。调用方已知接收者角色时直接传入，效率最高。
 * <p>
 * <b>为什么返回 null 代表"兜底"</b>：
 * 前端收到 {@code actionUrl=null} 时，自动回退到旧的 {@code useNotificationHandler} 硬编码逻辑，
 * 保证渐进式迁移：老通知无 action_url 仍能跳转，新通知优先使用 action_url。
 * <p>
 * <b>参考设计</b>：
 * <ul>
 *   <li>GitHub Notifications API subject.url 字段</li>
 *   <li>Slack message.blocks[].action_url</li>
 *   <li>Firebase Cloud Messaging click_action</li>
 * </ul>
 */
public final class NotificationActionUrlResolver {

    private NotificationActionUrlResolver() {}

    /**
     * 根据通知三元组 (type + bizType + bizId) + 接收者角色，生成前端路由路径。
     *
     * @param receiverRoleCode 接收者角色 ({@code ADMIN/TEACHER/STUDENT})
     * @param type             通知类型（EXAM_PUBLISHED 等）
     * @param bizType          业务类型（exam/score/user）
     * @param bizId            业务 ID（可空）
     * @return 前端路由路径，或 {@code null}（由前端回退兜底）
     */
    public static String resolve(String receiverRoleCode, String type, String bizType, Long bizId) {
        if (receiverRoleCode == null || type == null) return null;

        // ====== 考试相关 ======
        if ("exam".equals(bizType)) {
            return resolveExamUrl(receiverRoleCode, type, bizId);
        }

        // ====== 成绩相关 ======
        if ("score".equals(bizType)) {
            return resolveScoreUrl(receiverRoleCode, type);
        }

        // ====== 用户账号相关 ======
        if ("user".equals(bizType)) {
            return resolveUserUrl(receiverRoleCode, type);
        }

        return null;
    }

    private static String resolveExamUrl(String roleCode, String type, Long bizId) {
        switch (roleCode) {
            case RoleConstants.STUDENT_CODE:
                // 学生：EXAM_PUBLISHED/UPDATED/AUTO_SUBMITTED/ABSENT 都跳考试列表
                if ("EXAM_CANCELLED".equals(type)) {
                    return "/student-home";
                }
                return "/student/exam";

            case RoleConstants.TEACHER_CODE:
                // 教师：交卷/阅卷场景跳到具体考试的批改页
                if ("EXAM_SUBMITTED".equals(type) && bizId != null) {
                    return "/exam-center/marking?examId=" + bizId;
                }
                if ("EXAM_END_SUMMARY".equals(type) && bizId != null) {
                    return "/exam-center/marking?examId=" + bizId;
                }
                return "/exam-center/exam";

            case RoleConstants.ADMIN_CODE:
                // 管理员：跳管理首页
                if ("EXAM_CREATED".equals(type)) {
                    return "/admin-home";
                }
                return null;

            default:
                return null;
        }
    }

    private static String resolveScoreUrl(String roleCode, String type) {
        // 学生收到「成绩发布(SCORE_PUBLISHED)」与「成绩更新(SCORE_UPDATED)」均跳转到我的成绩页。
        // 对齐 MarkingServiceImpl.sendPublishNotifications 中两种通知的业务语义：
        //   - SCORE_PUBLISHED：首次发布
        //   - SCORE_UPDATED  ：教师修正后重新发布（10 分钟内合并）
        // 缺少 SCORE_UPDATED 分支会导致重发成绩通知点击后无 actionUrl 跳转，
        // 而前端 fallback 也只处理 SCORE_PUBLISHED → 用户点击只标记已读不跳转（实测 BUG）。
        if (RoleConstants.STUDENT_CODE.equals(roleCode)
                && ("SCORE_PUBLISHED".equals(type) || "SCORE_UPDATED".equals(type))) {
            return "/my-study/score";
        }
        return null;
    }

    private static String resolveUserUrl(String roleCode, String type) {
        // ACCOUNT_CREATED（欢迎通知）：所有角色统一跳转到共享个人中心 /profile。
        // 注：profile 路由已统一为跨角色共享一级路由（见 router/modules/shared.ts profileRoute），
        // 旧的 /admin/profile、/teacher/profile、/student/profile 已下线，发出会落到 404。
        // 必须先于下方 ADMIN 分支判断：管理员收到的 ACCOUNT_CREATED 也走个人中心而非用户管理。
        if ("ACCOUNT_CREATED".equals(type)) {
            return "/profile";
        }
        // 管理员收到的其他用户类事件（典型：USER_CREATED 新用户创建）跳到用户管理页。
        if (RoleConstants.ADMIN_CODE.equals(roleCode)) {
            return "/admin/user";
        }
        return null;
    }
}
