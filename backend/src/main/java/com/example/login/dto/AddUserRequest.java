package com.example.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddUserRequest {
    @NotBlank(message = "사용자명은 필수 입력값입니다")
    @Size(min = 4, max = 20, message = "사용자명은 4~20자 사이어야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자명은 영문자, 숫자, 언더스코어만 포함할 수 있습니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력값입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이어야 합니다")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$", message = "비밀번호는 숫자, 소문자, 대문자, 특수문자를 모두 포함해야 합니다")
    private String password;

    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String email;
}