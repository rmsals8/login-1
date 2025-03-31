package com.example.login.service;

import com.example.login.dto.AddUserRequest;
import com.example.login.entity.Password;
import com.example.login.entity.User;
import com.example.login.mapper.PasswordMapper;
import com.example.login.mapper.UserMapper;
import com.example.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordMapper passwordMapper;
    private final UserMapper userMapper;

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    @Transactional
    public Long save(AddUserRequest dto) {
        // 1. User 저장
        User user = User.builder()
                .userName(dto.getUsername())
                .loginType(0) // 일반 로그인
                .build();

        userMapper.save(user);
        Long userNo = userMapper.getLastInsertId();

        // 2. Password 저장
        String salt = generateSalt(); // 솔트 생성 메서드 필요
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        Password password = Password.builder()
                .userNo(userNo)
                .salt(salt)
                .password(encodedPassword)
                .build();

        passwordMapper.save(password);

        return userNo;
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Unexpected user"));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unexpected user"));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Unexpected user"));
    }
}