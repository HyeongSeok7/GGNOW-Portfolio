package com.project.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice      // 이 어노테이션은 Controller와 handler에서 발생하는 에러들을 모두 잡아준다. 이 안에서 @ExceptionHandler 를 사용하여 에러를 잡는다.
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class) // ExceptionHandler = Controller계층에서 발생하는 에어를 잡아 메소드로 처리해주는 기능
    public ModelAndView handleException(NoHandlerFoundException ex,  // NoHandlerFoundException ex = Spring MVC에서 특정 URL에 대한 핸들러가 없을 때 발생하는 예외 (사용자가 잘못된 Url에 접근하면 이 예외가 발생
                                        HttpServletRequest request) {  // HttpServletRequest request = 요청에 대한 정보를 제공하는 객체, HTTP 요청의 URL, 헤더, 파라미더, 세션 등 다양한 정보를 가져올 수 있다.
        String requestURI = request.getRequestURI();    // request.getRequestURI = 클라이언트가 요청한 URI를 반환, 반환된 URI를 requestURI변수에 저장, 이정보는 예외 처리 로직에서 로그를 남기거나 에러 페이지로 리디렉션 하는데 사용
        // 정적 리소스 요청은 예외 처리하지 않음
        if (requestURI.startsWith("/assets/") || requestURI.startsWith("/images/") || requestURI.startsWith("/favicon.ico") || requestURI.startsWith("/static/")) {
            return null;
        }
        ModelAndView modelAndView = new ModelAndView(); // 뷰와 데이터를 함께 반환하는 Spring 객체

        //에러페이지로 이동
        modelAndView.addObject("errorMessage", ex.getMessage());    // 예외 메시지를 errorMessage 라는 이름으로 추가
        modelAndView.setViewName("error");  //에러 처리용 뷰 이름을 error페이지로 설정
        return modelAndView;        // 클라이언트에게 반환할 "ModelAndView" 객체
    }
}