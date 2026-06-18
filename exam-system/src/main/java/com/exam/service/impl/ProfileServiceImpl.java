package com.exam.service.impl;

import com.exam.common.exception.BusinessException;
import com.exam.common.utils.XssUtils;
import com.exam.dto.request.PasswordChangeRequest;
import com.exam.dto.request.ProfileUpdateRequest;
import com.exam.entity.EduClass;
import com.exam.entity.SysRole;
import com.exam.entity.SysUser;
import com.exam.mapper.EduClassMapper;
import com.exam.mapper.SysRoleMapper;
import com.exam.mapper.SysUserMapper;
import com.exam.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired private SysUserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SysRoleMapper roleMapper;
    @Autowired private EduClassMapper classMapper;

    @Value("${upload.path:./uploads/}")
    private String uploadPath;

    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif");
    private static final long MAX_SIZE = 2 * 1024 * 1024; // 2MB

    @Override
    public Map<String, Object> getProfileInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("realName", user.getRealName());
        m.put("avatar", user.getAvatar());
        m.put("email", user.getEmail());
        m.put("phone", user.getPhone());
        m.put("gender", user.getGender());
        m.put("roleId", user.getRoleId());
        SysRole role = roleMapper.selectById(user.getRoleId());
        m.put("roleName", role != null ? role.getRoleName() : "");
        if (user.getClassId() != null) {
            m.put("classId", user.getClassId());
            EduClass eduClass = classMapper.selectById(user.getClassId());
            m.put("className", eduClass != null ? eduClass.getClassName() : "");
        }
        m.put("createTime", user.getCreateTime());
        return m;
    }

    @Override
    public void updateProfile(ProfileUpdateRequest request, Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        if (request.getRealName() != null) {
            if (XssUtils.containsXss(request.getRealName())) {
                throw new BusinessException("姓名包含非法字符");
            }
            user.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getGender() != null) user.setGender(request.getGender());
        userMapper.updateById(user);
    }

    @Override
    public void changePassword(PasswordChangeRequest request, Long userId) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        if (file.isEmpty()) throw new BusinessException("文件不能为空");
        if (!ALLOWED_TYPES.contains(file.getContentType())) throw new BusinessException("仅支持jpg、png、gif格式");
        if (file.getSize() > MAX_SIZE) throw new BusinessException("文件大小不能超过2MB");

        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".") ?
                originalName.substring(originalName.lastIndexOf(".")).toLowerCase() : ".jpg";
        if (!Arrays.asList(".jpg", ".jpeg", ".png", ".gif").contains(ext)) {
            throw new BusinessException("不支持的文件扩展名");
        }
        String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + ext;

        File dir = new File(uploadPath + "avatar/").getAbsoluteFile();
        if (!dir.exists()) dir.mkdirs();

        File dest = new File(dir, fileName);
        try {
            file.transferTo(dest.getAbsoluteFile());
        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }

        String avatarUrl = "/uploads/avatar/" + fileName;
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            // 删除旧头像
            if (user.getAvatar() != null && user.getAvatar().startsWith("/uploads/avatar/")) {
                File oldFile = new File(uploadPath + user.getAvatar().replace("/uploads/", ""));
                if (oldFile.exists()) oldFile.delete();
            }
            user.setAvatar(avatarUrl);
            userMapper.updateById(user);
        }
        return avatarUrl;
    }
}
