package com.example.login.mapper;

import com.example.login.entity.Password;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PasswordMapper {
    Password findByUserNo(@Param("userNo") Long userNo);

    void save(Password password);

    void update(Password password);
}