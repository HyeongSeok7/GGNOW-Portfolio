package com.project.web.controller;

import com.project.web.dto.ChangePasswordRequest;
import com.project.web.model.FavoriteEvent;
import com.project.web.model.User;
import com.project.web.repository.FavoriteEventRepository;
import com.project.web.service.ReviewService;
import com.project.web.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import com.project.web.model.FestivalEntity;
import com.project.web.repository.FestivalRepository;

import java.util.Objects;

import java.security.Principal;
import java.util.List;

import java.time.LocalDate;
import java.time.ZoneId;

//마이페이지 관련 화면을 담당하는 컨트롤러
//사용자 정보, 즐겨찾기 행사, 내가 작성한 리뷰, 비밀번호 변경 기능을 처리
@Controller
public class MyPageController {

	private final UserService userService;
	private final FavoriteEventRepository favoriteEventRepository;
	private final ReviewService reviewService;
	private final FestivalRepository festivalRepository;

	public MyPageController(
	        UserService userService,
	        FavoriteEventRepository favoriteEventRepository,
	        ReviewService reviewService,
	        FestivalRepository festivalRepository
	) {
	    this.userService = userService;
	    this.favoriteEventRepository = favoriteEventRepository;
	    this.reviewService = reviewService;
	    this.festivalRepository = festivalRepository;
	}

	// 로그인한 사용자의 기본 정보와 즐겨찾기 행사 목록을 조회해 마이페이지에 전달
	@GetMapping("/mypage")
	public String showMyPage(Model model, Principal principal) {

		// 현재 로그인한 사용자의 이름 가져오기
		String username = principal.getName();
		User user = userService.getUserByUsername(username); // 사용자 이름을 기반으로 사용자 정보를 가져온다
		model.addAttribute("user", user); // 사용자 정보를 모델에 추가하여 뷰로 전달

		// 즐겨찾기 이벤트의 ID 목록 추출
		List<Long> favoriteFestivalIds =
		        favoriteEventRepository.findAllByUsername(username)
		                .stream()
		                .map(FavoriteEvent::getEventId)
		                .map(this::parseFestivalId)
		                .filter(Objects::nonNull)
		                .toList();

		// 즐겨찾기는 개인 기록이므로 종료·비활성 행사도 함께 조회한다.
		List<FestivalEntity> favoriteEventDetails =
		        favoriteFestivalIds.isEmpty()
		                ? List.of()
		                : festivalRepository
		                        .findByIdInOrderByBeginDeDescIdDesc(
		                                favoriteFestivalIds
		                        );

		// 즐겨찾기 이벤트 상세 정보를 모델에 추가
		model.addAttribute("favoriteEvents", favoriteEventDetails);
		
		// 화면에서 종료 여부를 한국 날짜 기준으로 표시한다.
		model.addAttribute(
		        "today",
		        LocalDate.now(ZoneId.of("Asia/Seoul")).toString()
		);

		// mypage 뷰 이름 반환
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
	public String changePasswordSubmit(@ModelAttribute("changePasswordRequest") ChangePasswordRequest form, Model model,
			Principal principal, HttpServletRequest request, HttpServletResponse response) {

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
	private Long parseFestivalId(String eventId) {
	    try {
	        return Long.valueOf(eventId);
	    } catch (NumberFormatException e) {
	        return null;
	    }
	}
}
