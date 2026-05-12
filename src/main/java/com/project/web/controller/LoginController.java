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

//로그인 페이지, 회원가입 페이지, 아이디 중복 확인을 처리하는 컨트롤러
//실제 로그인 인증은 Spring Security가 담당, 컨트롤러는 페이지 이동과 회원가입 요청을 담당!
@Controller
public class LoginController {

    private final UserService userService;
    public LoginController(UserService userService){
        this.userService = userService;
    }

    // 로그인 페이지를 반환한다.
    // 로그인 실패, 로그아웃, 회원가입 완료 여부를 query parameter로 받아 화면 메시지로 전달한다.
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

    // 회원가입 요청 처리
    // 아이디 중복 확인 결과를 세션에 저장해두고,
    // 사용자가 중복 확인한 아이디와 실제 가입 요청 아이디가 같은지 검증
    @PostMapping("/register")
    public String register(@RequestParam("username") String username,
                           @RequestParam("password") String password,
                           @RequestParam("email") String email,
                           HttpSession session,
                           Model model) {
        try {
            String u = username == null ? "" : username.trim();

            // 프론트에서만 중복 확인을 하면 우회될 수 있으므로,
            // 서버 세션에 저장된 중복 확인 결과를 기준으로 최종 가입 가능 여부를 판단
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

            // 아이디/이메일 중복, 비밀번호 규칙 검증, 비밀번호 암호화는 UserService에서 처리
            userService.registerUser(u, password, email);

            // 가입 성공 후 세션 값 정리
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

        if (u.length() < 5 || u.length() > 20) {
            body.put("available", false);
            body.put("message", "아이디는 5~20자 사이로 입력해주세요.");
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