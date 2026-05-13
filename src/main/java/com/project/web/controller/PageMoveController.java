package com.project.web.controller;

import com.project.web.model.FestivalResponse;
import com.project.web.service.FestivalIdentityService;
import com.project.web.service.FestivalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

//카테고리별 행사 목록 페이지 이동을 담당하는 컨트롤러
//각 페이지에 외부 API 행사 데이터와 내부 festivalId를 함께 전달
@Controller
public class PageMoveController {

	private final FestivalService festivalService;
	private final FestivalIdentityService festivalIdentityService;

	public PageMoveController(FestivalService festivalService, FestivalIdentityService festivalIdentityService) {
		this.festivalService = festivalService;
		this.festivalIdentityService = festivalIdentityService;
	}

	// 문화 행사 페이지로 이동, 화면에서 상세 페이지 이동이 가능하도록 festivalId를 함께 세팅
	@GetMapping("/culture")
	public String moveCulturePage(Model model) {
		FestivalResponse festivalResponse = festivalService.getFestivalData();
		attachFestivalIds(festivalResponse);
		model.addAttribute("festivalData", festivalResponse);
		return "culture";
	}

	// 교육 행사 페이지로 이동, 화면에서 상세 페이지 이동이 가능하도록 festivalId를 함께 세팅
	@GetMapping("/education")
	public String moveEducationPage(Model model) {
		FestivalResponse festivalResponse = festivalService.getFestivalData();
		attachFestivalIds(festivalResponse);
		model.addAttribute("festivalData", festivalResponse);
		return "education";
	}

	// 전시 행사 페이지로 이동, 화면에서 상세 페이지 이동이 가능하도록 festivalId를 함께 세팅
	@GetMapping("/exhibition")
	public String moveExhibitionPage(Model model) {
		FestivalResponse festivalResponse = festivalService.getFestivalData();
		attachFestivalIds(festivalResponse);
		model.addAttribute("festivalData", festivalResponse);
		return "exhibition";
	}

	// 교육 행사 페이지로 이동, 화면에서 상세 페이지 이동이 가능하도록 festivalId를 함께 세팅
	@GetMapping("/performance")
	public String movePerformancePage(Model model) {
		FestivalResponse festivalResponse = festivalService.getFestivalData();
		attachFestivalIds(festivalResponse);
		model.addAttribute("festivalData", festivalResponse);
		return "performance";
	}

	// 외부 API에서 받은 행사 데이터에는 내부 DB id가 없으므로,
	// 각 Row에 festivalId를 생성/조회해 화면 이동과 리뷰/즐겨찾기 기능에서 사용할 수 있게 한다
	private void attachFestivalIds(FestivalResponse festivalResponse) {
		if (festivalResponse == null || festivalResponse.getRow() == null) {
			return;
		}

		festivalResponse.getRow().forEach(row -> {
			Long festivalId = festivalIdentityService.getOrCreateFestivalId(
					festivalService.createFestivalIdentityKey(row), festivalService.normalize(row.getTitle()),
					row.getTitle());
			row.setFestivalId(festivalId);
		});
	}

	@GetMapping("/customer")
	public String moveCustomerPage(Model model) {
		return "customer";
	}

	@GetMapping("/suggestions")
	public String suggestionsNotReady() {
		return "error";
	}
}