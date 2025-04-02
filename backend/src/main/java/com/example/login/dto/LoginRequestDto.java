package com.example.login.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LoginRequestDto {

    @NotBlank(message = "사용자명 입력 필수 ")
    private String username;

    @NotBlank(message = "비밀번호 입력 필수수")
    private String password;

    private String ipAddress;
    private String captcha;
}
