package com.bootcamp.flowcheck.domain.auth.controller;

import com.bootcamp.flowcheck.domain.auth.dto.InviteRequest;
import com.bootcamp.flowcheck.domain.auth.service.AuthService;
import com.bootcamp.flowcheck.global.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Void>> invite(@RequestBody @Valid InviteRequest request) {
        authService.invite(request.getName(), request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "PM으로 초대되었습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃 되었습니다."));
    }
}
