package com.example.login.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class KakaoOAuthConfig {

    @Value("${kakao.client.id}")
    private String clientId;

    @Value("${kakao.redirect.url}")
    private String redirectUrl;

    // 카카오 인증 URL
    public String getAuthorizationUrl() {
        return "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUrl
                + "&response_type=code";
    }

    // 카카오 토큰 요청 URL
    public String getTokenUrl() {
        return "https://kauth.kakao.com/oauth/token";
    }

    // 카카오 사용자 정보 요청 URL
    public String getUserInfoUrl() {
        return "https://kapi.kakao.com/v2/user/me";
    }
}