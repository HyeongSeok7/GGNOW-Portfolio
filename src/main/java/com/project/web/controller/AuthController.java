package com.project.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
	
	// 현재 요청의 Spring Security 인증 정보를 확인하는 테스트/상태 확인용 API
	// 프론트에서 로그인 여부를 확인할 때 사용
    @GetMapping("/check-login")
    public String checkLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            return "로그인 상태입니다. 사용자: " + authentication.getName();
        } else {
            return "로그인되지 않은 상태입니다.";
        }
    }
}