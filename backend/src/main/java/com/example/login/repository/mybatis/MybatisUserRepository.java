package com.example.login.repository.mybatis;

import com.example.login.entity.User;
import com.example.login.mapper.UserMapper;
import com.example.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Primary
@Repository
@RequiredArgsConstructor
public class MybatisUserRepository implements UserRepository {
    private final UserMapper userMapper;

    @Override
    public Optional<User> findByUsername(String username) {
        User user = userMapper.findByUserName(username);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        User user = userMapper.findByEmail(email);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findById(Long userId) {
        User user = userMapper.findByUserNo(userId);
        return Optional.ofNullable(user);
    }

    @Override
    public User save(User user) {
        userMapper.save(user);
        if (user.getUserNo() == null) {
            user.setUserNo(userMapper.getLastInsertId());
        }
        return user;
    }
}