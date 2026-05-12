package com.project.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

	// 존재하지 않는 URL 접근 시 기본 에러 페이지 대신 사용자 정의 error 페이지로 이동
	// 단, CSS/JS/이미지 같은 정적 리소스 요청은 예외 처리 대상에서 제외
	
    @ExceptionHandler(NoHandlerFoundException.class) 
    public ModelAndView handleException(NoHandlerFoundException ex, 
                                        HttpServletRequest request) {  
        String requestURI = request.getRequestURI();   
 
        if (requestURI.startsWith("/assets/") || requestURI.startsWith("/images/") || requestURI.startsWith("/favicon.ico") || requestURI.startsWith("/static/")) {
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();

   
        modelAndView.addObject("errorMessage", ex.getMessage());   
        modelAndView.setViewName("error"); 
        return modelAndView;       
    }
}