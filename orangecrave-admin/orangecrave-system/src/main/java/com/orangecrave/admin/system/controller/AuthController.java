package com.orangecrave.admin.system.controller;

import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.system.model.dto.LoginDTO;
import com.orangecrave.admin.system.model.vo.LoginVO;
import com.orangecrave.admin.system.model.vo.MenuTreeVO;
import com.orangecrave.admin.system.model.vo.UserInfoVO;
import com.orangecrave.admin.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.ok(authService.login(dto, request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(authService.info());
    }

    @GetMapping("/menus")
    public Result<List<MenuTreeVO>> menus() {
        return Result.ok(authService.menus());
    }
}
