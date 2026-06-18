package com.project.web.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.project.web.model.User;
import com.project.web.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	/**
	 * 회원 서비스 단위 테스트
	 *
	 * 검증 내용
	 * - 회원가입 성공
	 * - 아이디 중복 검증
	 */
	
    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void 회원가입_성공() {

    	// 동일한 ID가 존재하지 않는다고 가정
        when(userRepository.existsByUsername("tester"))
                .thenReturn(false);
        
        // 동일한 Email이 존재하지 않는다고 가정
        when(userRepository.existsByEmail("test@test.com"))
                .thenReturn(false);

        // 비밀번호 암호화 결과 설정
        when(passwordEncoder.encode("abc123"))
                .thenReturn("encrypted");

        // 회원가입 실행
        userService.registerUser(
                "tester",
                "abc123",
                "test@test.com"
        );

        // 사용자 저장 여부 검증
        verify(userRepository)
                .saveAndFlush(any(User.class));
    }
    
    @Test
    void 회원가입_아이디중복() {

    	// 이미 사용중인 ID라고 가정
        when(userRepository.existsByUsername("tester"))
                .thenReturn(true);

        // 회원가입 시 예외 발생 검증
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.registerUser(
                                "tester",
                                "abc123",
                                "test@test.com"
                        )
                );

        // 예외 메시지 검증
        assertEquals(
                "이미 사용 중인 아이디입니다.",
                exception.getMessage()
        );
    }
    
}