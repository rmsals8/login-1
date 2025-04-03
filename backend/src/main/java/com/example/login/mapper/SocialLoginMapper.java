package com.example.login.mapper;

import com.example.login.entity.SocialLogin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SocialLoginMapper {

    SocialLogin findByExternalId(@Param("externalId") String externalId, @Param("socialCode") Integer socialCode);

    SocialLogin findByUserNo(@Param("userNo") Long userNo, @Param("socialCode") Integer socialCode);

    void save(SocialLogin socialLogin);

    void update(SocialLogin socialLogin);
}