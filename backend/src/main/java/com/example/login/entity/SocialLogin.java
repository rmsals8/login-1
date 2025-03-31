package com.example.login.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SocialLogin {
    private Long socialLoginId;
    private Long userNo;
    private Integer socialCode;
    private String externalId;
    private String accessToken;
    private LocalDateTime updateDate;
}