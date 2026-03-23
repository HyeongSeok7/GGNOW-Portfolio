package com.project.web.controller;

import com.project.web.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LoginController {

    private final UserService userService;
    public LoginController(UserService userService){
        this.userService = userService;
    }

    // 로그인 페이지를 반환하고, 실패 또는 로그아웃 메시지 추가
    @GetMapping("/login")
    public String moveLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "registered", required = false) String registered,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호를 확인해주세요.");
        }
        if (logout != null) {
            model.addAttribute("message", "로그아웃 되었습니다.");
        }
        if (registered != null) {
            model.addAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요.");
        }
        return "login";
    }


    // 회원가입 창
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // 회원가입 설정
    @PostMapping("/register")
    public String register(@RequestParam("username") String username,
                           @RequestParam("password") String password,
                           @RequestParam("email") String email,
                           HttpSession session,
                           Model model) {
        try {
            String u = username == null ? "" : username.trim();

            // 중복확인 강제: 세션에 저장된 값과 일치 + available=true 여야만 가입 가능
            String checkedUsername = (String) session.getAttribute("checkedUsername");
            Boolean usernameAvailable = (Boolean) session.getAttribute("usernameAvailable");

            if (checkedUsername == null || usernameAvailable == null) {
                throw new IllegalArgumentException("아이디 중복확인을 먼저 해주세요.");
            }
            if (!u.equals(checkedUsername)) {
                throw new IllegalArgumentException("중복확인한 아이디와 현재 입력한 아이디가 다릅니다. 다시 중복확인 해주세요.");
            }
            if (!usernameAvailable) {
                throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
            }

            userService.registerUser(u, password, email);

            // 가입 성공 후 세션 값 정리(선택)
            session.removeAttribute("checkedUsername");
            session.removeAttribute("usernameAvailable");

            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("username", username);
            model.addAttribute("email",email);

            String msg = e.getMessage();
            if( msg != null && msg.contains("비밀번호는 6~12자")) {
                model.addAttribute("passwordRuleError",msg);
            } else{
                model.addAttribute("error", msg);
            }
            return "register";
        }
    }

    @GetMapping("/register/check-username")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam("username") String username, HttpSession session) {
        Map<String, Object> body = new HashMap<>();

        String u = username == null ? "" : username.trim();
        if (u.isEmpty()) {
            body.put("available", false);
            body.put("message", "아이디를 입력해주세요.");
            return ResponseEntity.ok(body);
        }

        boolean available = userService.isUsernameAvailable(u);

        // 서버에서 강제하기 위해 세션에 저장
        session.setAttribute("checkedUsername", u);
        session.setAttribute("usernameAvailable", available);

        body.put("available", available);
        body.put("message", available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.");
        return ResponseEntity.ok(body);
    }
}