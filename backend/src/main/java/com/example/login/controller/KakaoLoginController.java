package com.example.login.controller;

import com.example.login.dto.LoginResponseDto;
import com.example.login.service.KakaoLoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoLoginController {

    private final KakaoLoginService kakaoLoginService;

    /**
     * 카카오 로그인 페이지로 리다이렉트
     */
    @GetMapping("/login")
    public RedirectView kakaoLogin() {
        String kakaoAuthUrl = kakaoLoginService.getKakaoAuthUrl();
        return new RedirectView(kakaoAuthUrl);
    }

    /**
     * 카카오 인증 콜백 처리
     */
    @GetMapping("/callback")
    public ResponseEntity<LoginResponseDto> kakaoCallback(
            @RequestParam("code") String code,
            HttpServletRequest request,
            HttpServletResponse response) {

        LoginResponseDto loginResponse = kakaoLoginService.kakaoLogin(code, request, response);

        return ResponseEntity.ok(loginResponse);
    }
}