package com.example.login.service;

import com.example.login.dto.AddUserRequest;
import com.example.login.entity.Password;
import com.example.login.entity.User;
import com.example.login.mapper.PasswordMapper;
import com.example.login.mapper.UserMapper;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

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
        User user = User.builder()
                .userName(dto.getUsername())
                .loginType(0)
                .build();

        userMapper.save(user);
        Long userNo = userMapper.getLastInsertId();

        String salt = generateSalt();
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
        User user = userMapper.findByUserNo(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unexpected user");
        }
        return user;
    }

    public User findByEmail(String email) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Unexpected user");
        }
        return user;
    }

    public User findByUsername(String username) {
        User user = userMapper.findByUserName(username);
        if (user == null) {
            throw new IllegalArgumentException("Unexpected user");
        }
        return user;
    }
}