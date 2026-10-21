package com.orangecrave.admin.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.common.concurrent.Concurrent;
import com.orangecrave.admin.common.concurrent.IoConcurrencyConfig;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.system.entity.SysLoginLog;
import com.orangecrave.admin.system.entity.SysMenu;
import com.orangecrave.admin.system.entity.SysRole;
import com.orangecrave.admin.system.entity.SysUser;
import com.orangecrave.admin.system.entity.SysUserRole;
import com.orangecrave.admin.system.mapper.SysLoginLogMapper;
import com.orangecrave.admin.system.mapper.SysMenuMapper;
import com.orangecrave.admin.system.mapper.SysRoleMapper;
import com.orangecrave.admin.system.mapper.SysUserMapper;
import com.orangecrave.admin.system.mapper.SysUserRoleMapper;
import com.orangecrave.admin.system.model.dto.LoginDTO;
import com.orangecrave.admin.system.model.vo.LoginVO;
import com.orangecrave.admin.system.model.vo.MenuTreeVO;
import com.orangecrave.admin.system.model.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 认证（见 00 篇 §4.1 / 02 篇 §2.1）。* */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final SysMenuService menuService;
    private final PasswordEncoder passwordEncoder;

    /** user / roles / perms 三个查询都只依赖已知 userId，并行化把鉴权延迟从 Σ(RTT) 压到 max(RTT)。*/
    @Qualifier(IoConcurrencyConfig.IO_EXECUTOR)
    @Autowired
    private Executor ioExecutor;

    public LoginVO login(LoginDTO dto, HttpServletRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            recordLogin(request, dto.getUsername(), 0, "用户名或密码错误");
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            recordLogin(request, dto.getUsername(), 0, "账号已停用");
            throw new BizException("账号已停用");
        }

        StpUtil.login(user.getId());
        StpUtil.getSession().set("nickname", user.getNickname());

        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp(request));
        userMapper.updateById(user);

        recordLogin(request, dto.getUsername(), 1, "登录成功");
        return new LoginVO(StpUtil.getTokenValue(), StpUtil.getTokenName());
    }

    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    public UserInfoVO info() {
        Long userId = StpUtil.getLoginIdAsLong();
        CompletableFuture<SysUser> userF = Concurrent.supply(ioExecutor, () -> userMapper.selectById(userId));
        CompletableFuture<List<String>> rolesF = Concurrent.supply(ioExecutor, () -> roleKeys(userId));
        CompletableFuture<List<String>> permsF =
                Concurrent.supply(ioExecutor, () -> menuMapper.selectPermKeysByUserId(userId));

        SysUser user = Concurrent.join(userF);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRoles(Concurrent.join(rolesF));
        vo.setPerms(Concurrent.join(permsF));
        return vo;
    }

    public List<MenuTreeVO> menus() {
        List<SysMenu> menus = menuMapper.selectMenusByUserId(StpUtil.getLoginIdAsLong());
        return menuService.toTree(menus);
    }

    private List<String> roleKeys(Long userId) {
        List<SysUserRole> relations = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        return roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getRoleKey).collect(Collectors.toList());
    }

    private void recordLogin(HttpServletRequest request, String userName, int status, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUserName(userName);
        log.setIp(request == null ? null : clientIp(request));
        log.setBrowser(request == null ? null : request.getHeader("User-Agent"));
        log.setOs(null);
        log.setStatus(status);
        log.setMsg(msg);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        return request.getRemoteAddr();
    }
}
