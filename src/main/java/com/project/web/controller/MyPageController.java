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


//마이페이지 관련 화면을 담당하는 컨트롤러
//사용자 정보, 즐겨찾기 행사, 내가 작성한 리뷰, 비밀번호 변경 기능을 처리
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

    // 로그인한 사용자의 기본 정보와 즐겨찾기 행사 목록을 조회해 마이페이지에 전달
    @GetMapping("/mypage")
    public String showMyPage(Model model, Principal principal) {

        //현재 로그인한 사용자의 이름 가져오기
        String username = principal.getName();
        User user = userService.getUserByUsername(username);  //사용자 이름을 기반으로 사용자 정보를 가져온다
        model.addAttribute("user", user);       // 사용자 정보를 모델에 추가하여 뷰로 전달

        // favorite_event 테이블에는 사용자가 저장한 행사 식별값만 있으므로,
        // 외부 API 행사 목록과 다시 매칭해 화면에 표시할 상세 정보를 구성
        List<FavoriteEvent> favoriteEvents = favoriteEventRepository.findAllByUsername(username); 

        //즐겨찾기 이벤트의 ID 목록 추출
        List<String> favoriteEventIds = favoriteEvents.stream()
                .map(FavoriteEvent::getEventId) // FavoriteEvent 객체에서 eventID 필드 값 추출
                .collect(Collectors.toList());  // 스트림 결과를 리스트로 변환

        //즐겨찾기 이벤트의 상세 정보를 조회
        List<FestivalResponse.Row> favoriteEventDetails = festivalService.getFestivalData()
                .getRow()
                .stream()
                .map(event -> {
                	// 외부 API Row마다 내부 festivalId를 붙여 상세 페이지 이동과 즐겨찾기 비교에 사용
                	Long festivalId = festivalIdentityService.getOrCreateFestivalId(
                	        festivalService.createFestivalIdentityKey(event),
                	        festivalService.normalize(event.getTitle()),
                	        event.getTitle()
                	);
                    event.setFestivalId(festivalId);

                    String fidKey = String.valueOf(festivalId);
                    String favoriteId = null;

                    // 현재 구조에서는 festivalId를 기준으로 즐겨찾기를 비교
                    // 기존에 제목 기반으로 저장된 데이터가 있을 수 있어 title 비교도 보조적으로 유지
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

    // 현재 로그인한 사용자가 작성한 리뷰를 최신순으로 조회해 내 리뷰 페이지에 전달
    @GetMapping("/my-reviews")
    public String myReviews(Model model, Principal principal) {
        String username = principal.getName();
        model.addAttribute("reviews", reviewService.getReviewsByUsername(username));
        return "my-reviews";
    }


    // 비밀번호 변경 폼을 보여주기 위해 빈 DTO 객체를 모델에 담아 전달
    @GetMapping("/change-password")
    public String showChangePasswordPage(Model model) {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "change-password";
    }

    // 현재 비밀번호 확인 후 새 비밀번호로 변경
    // 변경 성공 시 기존 세션을 로그아웃 처리해 다시 로그인하도록 한다
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
            // 비밀번호 변경 후 기존 인증 세션을 종료
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
