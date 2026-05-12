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

	// 회원 비밀번호를 평문으로 저장하지 않기 위해 BCrypt 방식으로 암호화
	// 회원가입, 로그인 인증, 비밀번호 변경 시 UserService에서 사용
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    
    // Spring Security 인증/인가 설정
    // 행사 조회, 검색, 로그인/회원가입 페이지는 비회원도 접근 가능하게 두고,
    // 마이페이지, 리뷰 작성, 즐겨찾기 같은 사용자 기능은 로그인 후 접근하도록 제한!
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
                        
                        // 리뷰 목록 조회는 비회원도 가능하지만,
                        // 리뷰 작성/수정/삭제는 인증된 사용자만 가능하도록 GET 요청만 허용
                        .requestMatchers(HttpMethod.GET, "/festivals/*/reviews/**").permitAll()
                        
                        .anyRequest().authenticated()
                )
                
                // 기본 로그인 페이지 대신 직접 만든 /login 페이지를 사용하고,
                // 로그인 성공/실패 시 이동할 경로를 지정
                .formLogin(form -> form
                        .loginPage("/login")    // 사용자 정의 로그인 페이지
                        .defaultSuccessUrl("/main", true)   //로그인 성공 시 리다이렉트 할 URL
                        .failureUrl("/login?error=true")         //로그인 실패 시 리다이렉트 할 URL
                        .permitAll()        //로그인 페이지는 인증 없이 접근 가능
                )
                // 로그아웃 시 세션과 JSESSIONID 쿠키를 정리해 인증 상태를 완전히 해제한다.
                .logout(logout -> logout
                        .logoutUrl("/logout")   // 로그아웃 엔드포인트
                        .logoutSuccessUrl("/main") //로그아웃 성공 후 리다이렉트 할 URL
                        .invalidateHttpSession(true)            //로그아웃 시 세션 무효화
                        .deleteCookies("JSESSIONID")    //로그아웃 시 JSESSIONID 쿠키 삭제
                );


        return http.build();        //설정된 SecurityFilterChain을 반환
    }
}