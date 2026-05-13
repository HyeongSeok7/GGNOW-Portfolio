package com.project.web.controller;

import com.project.web.model.FestivalResponse;
import com.project.web.service.FestivalIdentityService;
import com.project.web.service.FestivalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

//행사 검색 결과 페이지를 담당하는 컨트롤러
//키워드로 검색한 행사 목록에 내부 festivalId를 붙여 상세 페이지 이동이 가능하게 한다
@Controller
public class SearchController {

	private final FestivalService festivalService;
	private final FestivalIdentityService festivalIdentityService;

	public SearchController(FestivalService festivalService, FestivalIdentityService festivalIdentityService) {
		this.festivalService = festivalService;
		this.festivalIdentityService = festivalIdentityService;
	}

	// 사용자가 입력한 키워드로 행사 제목, 기관명, 주소를 검색
	// 검색 결과마다 festivalId를 세팅해 상세 페이지 URL에서 사용할 수 있게 한다
	@GetMapping("/search")
	public String searchFestivals(@RequestParam("keyword") String keyword, Model model) {
		List<FestivalResponse.Row> filterFestivals = festivalService.searchFestivals(keyword);

		filterFestivals.forEach(festival -> {
			Long festivalId = festivalIdentityService.getOrCreateFestivalId(
					festivalService.createFestivalIdentityKey(festival), festivalService.normalize(festival.getTitle()),
					festival.getTitle());
			festival.setFestivalId(festivalId);
		});

		model.addAttribute("festivalData", filterFestivals);
		return "searchlist";
	}
}