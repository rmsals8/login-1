package com.example.login.mapper;

import com.example.login.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findByUserName(@Param("userName") String userName);

    User findByUserNo(@Param("userNo") Long userNo);

    User findByEmail(@Param("email") String email);

    void save(User user);

    Long getLastInsertId();
}