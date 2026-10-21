package com.orangecrave.admin.system.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.system.entity.SysRole;
import com.orangecrave.admin.system.entity.SysUser;
import com.orangecrave.admin.system.entity.SysUserRole;
import com.orangecrave.admin.system.mapper.SysRoleMapper;
import com.orangecrave.admin.system.mapper.SysUserMapper;
import com.orangecrave.admin.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时保证存在一个超级管理员（BCrypt 哈希在运行时生成，避免 SQL 里硬编码）。
 * 账号 admin / 密码 admin123（首次启动后请修改）。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, DEFAULT_USERNAME));
        if (count != null && count > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername(DEFAULT_USERNAME);
        admin.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        admin.setNickname("超级管理员");
        admin.setStatus(1);
        userMapper.insert(admin);

        SysRole superRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, "super_admin"));
        if (superRole != null) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(admin.getId());
            relation.setRoleId(superRole.getId());
            userRoleMapper.insert(relation);
        }
        log.info("已创建默认超级管理员 admin / {}（请及时修改密码）", DEFAULT_PASSWORD);
    }
}
