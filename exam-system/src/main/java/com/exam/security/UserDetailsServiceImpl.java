package com.exam.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.entity.SysRole;
import com.exam.entity.SysUser;
import com.exam.mapper.SysRoleMapper;
import com.exam.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private SysRoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        if (user.getRoleId() == null) {
            throw new UsernameNotFoundException("用户角色未分配");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null) {
            throw new UsernameNotFoundException("用户角色数据异常");
        }
        return new LoginUser(user, role.getRoleCode());
    }
}
