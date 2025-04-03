package com.example.login.dto;

import lombok.Data;

@Data
public class KakaoTokenResponse {
    private String token_type;
    private String access_token;
    private String refresh_token;
    private String id_token;
    private Integer expires_in;
    private Integer refresh_token_expires_in;
    private String scope;
}