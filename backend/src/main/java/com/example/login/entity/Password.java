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
public class Password {
    private Long passwordId;
    private Long userNo;
    private String salt;
    private String password;
    private LocalDateTime updateDate;
}