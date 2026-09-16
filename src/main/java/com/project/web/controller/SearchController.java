package com.project.web.controller;

import com.project.web.service.FestivalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SearchController {

    private final FestivalService festivalService;

    public SearchController(FestivalService festivalService) {
        this.festivalService = festivalService;
    }

    // 같은 검색어로 진행 중·예정 행사와 종료 행사를 각각 검색한다.
    @GetMapping("/search")
    public String searchFestivals(
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            Model model
    ) {
        String searchKeyword = keyword.trim();

        FestivalService.SearchResult result =
                festivalService.searchFestivals(searchKeyword);

        // 검색창에 입력한 검색어를 유지한다.
        model.addAttribute("keyword", searchKeyword);

        // 위쪽에 표시할 진행 중·예정 행사
        model.addAttribute("festivalData", result.activeFestivals());

        // 아래쪽에 표시할 종료 행사
        model.addAttribute("endedFestivalData", result.endedFestivals());

        return "searchlist";
    }
}