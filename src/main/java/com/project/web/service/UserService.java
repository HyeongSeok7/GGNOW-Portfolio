package com.project.web.service;

import com.project.web.model.User;
import com.project.web.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.regex.Pattern;

//회원가입, 로그인 인증 정보 조회, 비밀번호 변경 등 사용자 관련 비즈니스 로직을 담당
//Spring Security의 UserDetailsService를 구현해 로그인 인증 과정에서도 사용
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입 처리
    // 아이디/이메일 중복과 비밀번호 규칙을 검증한 뒤
    // 비밀번호를 BCrypt로 암호화해 저장
    @Transactional
    public void registerUser(String username, String password, String email) {
    	String normalizedUsername = normalizeRequired(username, "아이디를 입력해주세요.");
    	validateUsernameOrThrow(normalizedUsername);

    	String normalizedEmail = normalizeRequired(email, "이메일을 입력해주세요.").toLowerCase();

    	// 사용자에게 빠르게 중복 메시지를 보여주기 위해 저장 전 중복 여부를 먼저 확인
    	if (userRepository.existsByUsername(normalizedUsername)) {
    	    throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
    	}
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        validatePasswordOrThrow(password);

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(password.trim()));
        user.setEmail(normalizedEmail);
        user.setJoinDate(LocalDate.now());

        // 동시에 같은 아이디나 이메일로 가입 요청이 들어올 수 있으므로,
        // DB unique 제약조건 위반도 한 번 더 처리
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (userRepository.existsByUsername(normalizedUsername)) {
                throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
            }
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            }
            throw new IllegalStateException("회원가입 처리 중 데이터 무결성 오류가 발생했습니다.", e);
        }
    }

    // Spring Security가 로그인 인증 시 호출하는 메서드
    // DB에서 사용자 정보를 조회해 Security가 사용할 UserDetails 객체로 변환
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), Collections.emptyList());
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    // 비밀번호 변경 처리
    // 현재 비밀번호가 맞는지 확인하고, 새 비밀번호 규칙과 기존 비밀번호와의 중복 여부를 검증
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = getUserByUsername(username);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        validatePasswordOrThrow(newPassword);

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호가 기존 비밀번호와 같습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
    }

    // 회원가입 중복 확인 API에서 사용하는 메서드
    // 아이디 형식이 올바르지 않으면 사용 불가로 처리
    public boolean isUsernameAvailable(String username) {
        String normalizedUsername = username == null ? "" : username.trim();

        try {
            validateUsernameOrThrow(normalizedUsername);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return !userRepository.existsByUsername(normalizedUsername);
    }

    // 비밀번호는 6~12자이며, 영문과 숫자를 각각 최소 1개 이상 포함해야 함
    private static final Pattern PASSWORD_RULE =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,12}$");

    private void validatePasswordOrThrow(String password) {
        if (password == null) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요");
        }

        String p = password.trim();
        if (!PASSWORD_RULE.matcher(p).matches()) {
            throw new IllegalArgumentException("비밀번호는 6~12자이며 영문과 숫자를 각각 한 개 이상 포함해야 합니다.");
        }
    }

    // 필수 입력값의 null/blank 여부를 확인하고 앞뒤 공백을 제거
    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
    
    // 아이디 입력 여부와 길이 조건을 검증
    private void validateUsernameOrThrow(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }

        String u = username.trim();
        if (u.length() < 5 || u.length() > 20) {
            throw new IllegalArgumentException("아이디는 5~20자 사이로 입력해주세요.");
        }
    }
}