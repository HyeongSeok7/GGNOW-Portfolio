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

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerUser(String username, String password, String email) {
        String normalizedUsername = normalizeRequired(username, "아이디를 입력해주세요.");
        String normalizedEmail = normalizeRequired(email, "이메일을 입력해주세요.").toLowerCase();

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

    public boolean isUsernameAvailable(String username) {
        String normalizedUsername = username == null ? "" : username.trim();
        return !normalizedUsername.isEmpty() && !userRepository.existsByUsername(normalizedUsername);
    }

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

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}