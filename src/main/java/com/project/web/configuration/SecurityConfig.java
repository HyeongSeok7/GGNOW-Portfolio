package com.project.web.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/register","/register/check-username", "/login", "/main", "/error",       // 비인증 사용자도 접근가능한 엔드포인트
                                "/assets/**", "/images/**",
                                
                                "/performance", "/exhibition", "/culture", "/education",
                                
                                "/festival/**", "/search", "/customer",
                                "/check-login"
                        ).permitAll()   //위에 나열된 URL은 인증 없이 접근 가능
                        
                        .requestMatchers(HttpMethod.GET, "/festivals/*/reviews/**").permitAll()
                        
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")    // 사용자 정의 로그인 페이지
                        .defaultSuccessUrl("/main", true)   //로그인 성공 시 리다이렉트 할 URL
                        .failureUrl("/login?error=true")         //로그인 실패 시 리다이렉트 할 URL
                        .permitAll()        //로그인 페이지는 인증 없이 접근 가능
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")   // 로그아웃 엔드포인트
                        .logoutSuccessUrl("/main") //로그아웃 성공 후 리다이렉트 할 URL
                        .invalidateHttpSession(true)            //로그아웃 시 세션 무효화
                        .deleteCookies("JSESSIONID")    //로그아웃 시 JSESSIONID 쿠키 삭제
                );


        return http.build();        //설정된 SecurityFilterChain을 반환
    }
}