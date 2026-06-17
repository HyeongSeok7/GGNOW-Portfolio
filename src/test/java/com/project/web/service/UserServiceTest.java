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

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void 회원가입_성공() {

        when(userRepository.existsByUsername("tester"))
                .thenReturn(false);

        when(userRepository.existsByEmail("test@test.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("abc123"))
                .thenReturn("encrypted");

        userService.registerUser(
                "tester",
                "abc123",
                "test@test.com"
        );

        verify(userRepository)
                .saveAndFlush(any(User.class));
    }
    
    @Test
    void 회원가입_아이디중복() {

        when(userRepository.existsByUsername("tester"))
                .thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.registerUser(
                                "tester",
                                "abc123",
                                "test@test.com"
                        )
                );

        assertEquals(
                "이미 사용 중인 아이디입니다.",
                exception.getMessage()
        );
    }
    
}