package com.example.login.controller;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.login.dto.CaptchaVerifyDto;
import com.google.code.kaptcha.impl.DefaultKaptcha;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired
    private DefaultKaptcha captchaProducer;

    @GetMapping("/image")
    public void getCaptchaImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 캡차 텍스트 생성
        String captchaText = captchaProducer.createText();

        // 세션에 캡차 텍스트 저장 (검증용)
        request.getSession().setAttribute("captchaText", captchaText);

        // 캡차 이미지 생성
        BufferedImage image = captchaProducer.createImage(captchaText);

        // 이미지 출력
        response.setContentType("image/jpeg");
        ImageIO.write(image, "jpg", response.getOutputStream());
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCaptcha(HttpServletRequest request, @RequestBody CaptchaVerifyDto dto) {
        String sessionCaptcha = (String) request.getSession().getAttribute("captchaText");

        if (sessionCaptcha == null) {
            return ResponseEntity.badRequest().body("캡차가 만료되었습니다.");
        }

        if (sessionCaptcha.equals(dto.getUserInput())) {
            // 검증 성공 후 세션의 캡차 정보 삭제 (재사용 방지)
            request.getSession().removeAttribute("captchaText");
            return ResponseEntity.ok().body("캡차 검증 성공");
        } else {
            return ResponseEntity.badRequest().body("캡차 검증 실패");
        }
    }
}