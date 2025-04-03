package com.example.login.dto;

import lombok.Data;
import java.util.Map;

@Data
public class KakaoUserInfoResponse {
    private Long id;
    private String connected_at;
    private Map<String, Object> properties;
    private Map<String, Object> kakao_account;

    public String getNickname() {
        if (properties != null && properties.containsKey("nickname")) {
            return (String) properties.get("nickname");
        }
        return null;
    }

    public String getProfileImage() {
        if (properties != null && properties.containsKey("profile_image")) {
            return (String) properties.get("profile_image");
        }
        return null;
    }

    public String getEmail() {
        if (kakao_account != null && kakao_account.containsKey("email")) {
            return (String) kakao_account.get("email");
        }
        return null;
    }
}