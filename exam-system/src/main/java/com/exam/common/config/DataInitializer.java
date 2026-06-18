package com.exam.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.SysUser;
import com.exam.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化：确保admin账号密码为有效的BCrypt加密
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        SysUser admin = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));
        if (admin == null) {
            // 仅在admin账号不存在时创建，不会重置已修改的密码
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setRoleId(1L);
            admin.setStatus(1);
            userMapper.insert(admin);
        }
    }
}