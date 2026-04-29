package com.project.web.controller;


import com.project.web.dto.ChangePasswordRequest;
import com.project.web.model.FavoriteEvent;
import com.project.web.model.FestivalResponse;
import com.project.web.model.User;
import com.project.web.repository.FavoriteEventRepository;
import com.project.web.service.FestivalIdentityService;
import com.project.web.service.FestivalService;
import com.project.web.service.ReviewService;
import com.project.web.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;


@Controller
public class MyPageController {

    private final UserService userService;
    private final FavoriteEventRepository favoriteEventRepository;
    private final FestivalService festivalService;
    private final ReviewService reviewService;
    private final FestivalIdentityService festivalIdentityService;

    public MyPageController(UserService userService,
                            FavoriteEventRepository favoriteEventRepository,
                            FestivalService festivalService,
                            ReviewService reviewService,
                            FestivalIdentityService festivalIdentityService) {
        this.userService = userService;
        this.favoriteEventRepository = favoriteEventRepository;
        this.festivalService = festivalService;
        this.reviewService = reviewService;
        this.festivalIdentityService = festivalIdentityService;
    }

    @GetMapping("/mypage")
    public String showMyPage(Model model, Principal principal) {

        //현재 로그인한 사용자의 이름 가져오기
        String username = principal.getName();
        User user = userService.getUserByUsername(username);  //사용자 이름을 기반으로 사용자 정보를 가져온다
        model.addAttribute("user", user);       // 사용자 정보를 모델에 추가하여 뷰로 전달

        //사용자의 즐겨찾기 이벤트 조회
        List<FavoriteEvent> favoriteEvents = favoriteEventRepository.findAllByUsername(username);  //사용자의 즐겨찾기 이벤트를 데이터베이스에서 조회

        //즐겨찾기 이벤트의 ID 목록 추출
        List<String> favoriteEventIds = favoriteEvents.stream()
                .map(FavoriteEvent::getEventId) // FavoriteEvent 객체에서 eventID 필드 값 추출
                .collect(Collectors.toList());  // 스트림 결과를 리스트로 변환

        //즐겨찾기 이벤트의 상세 정보를 조회
        List<FestivalResponse.Row> favoriteEventDetails = festivalService.getFestivalData()
                .getRow()
                .stream()
                .map(event -> {
                	Long festivalId = festivalIdentityService.getOrCreateFestivalId(
                	        festivalService.createFestivalIdentityKey(event),
                	        festivalService.normalize(event.getTitle()),
                	        event.getTitle()
                	);
                    event.setFestivalId(festivalId);

                    String fidKey = String.valueOf(festivalId);
                    String favoriteId = null;

                    if (favoriteEventIds.contains(fidKey)) favoriteId = fidKey;
                    else if (favoriteEventIds.contains(event.getTitle())) favoriteId = event.getTitle();

                    event.setFavoriteId(favoriteId);
                    return event;
                })
                .filter(event -> event.getFavoriteId() != null)
                .collect(Collectors.toList());//필터링 결과를 리스트로 변환

        //즐겨찾기 이벤트 상세 정보를 모델에 추가
        model.addAttribute("favoriteEvents", favoriteEventDetails);

        //mypage 뷰 이름 반환
        return "mypage";
    }

    @GetMapping("/my-reviews")
    public String myReviews(Model model, Principal principal) {
        String username = principal.getName();
        model.addAttribute("reviews", reviewService.getReviewsByUsername(username));
        return "my-reviews";
    }


    @GetMapping("/change-password")
    public String showChangePasswordPage(Model model) {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePasswordSubmit(
            @ModelAttribute("changePasswordRequest") ChangePasswordRequest form,
            Model model,
            Principal principal,
            HttpServletRequest request,
            HttpServletResponse response) {

        String username = principal.getName();

        if (form.getNewPassword() == null || form.getNewPassword().trim().isEmpty()) {
            model.addAttribute("error", "새 비밀번호를 입력해주세요.");
            model.addAttribute("changePasswordRequest", form);
            return "change-password";
        }

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "새 비밀번호 확인이 일치하지 않습니다.");
            model.addAttribute("changePasswordRequest", form);
            return "change-password";
        }

        try {
            userService.changePassword(username, form.getCurrentPassword(), form.getNewPassword());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                new SecurityContextLogoutHandler().logout(request, response, auth);
            }

            return "redirect:/login?passwordChanged";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("changePasswordRequest", form);
            return "change-password";
        }
    }
}
