package com.example.login.service;

import com.example.login.entity.User;
import com.example.login.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserDetailService implements UserDetailsService {
    private final UserMapper userMapper;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUserName(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return user;
    }
}