package com.project.web.controller;

import com.project.web.model.FestivalResponse;
import com.project.web.service.FestivalService;  // FestivalService 임포트 추가

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    private final FestivalService festivalService;

    public SearchController(FestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @GetMapping("/search")  //HTTP GET 요청을 "/search" URL에 매핑
    public String searchFestivals(@RequestParam("keyword") String keyword, Model model) {
        //FestivalService를 사용해 검색 키워드에 해당하는 축제 데이터를 필터링 하여 가져온다
        List<FestivalResponse.Row> filterFestivals = festivalService.searchFestivals(keyword);  // 반환 타입 수정

        //검색 결과 데이터를 모델에 추가
        model.addAttribute("festivalData", filterFestivals);  // "festivalData"라는 이름으로 필터링된 데이터를 뷰에 전달

        //"searchlist"라는 뷰 이름을 반환하여 해당 뷰를 렌더링
        return "searchlist";    //클라이언트에게 "searchlist.html" 페이지를 렌더링 하도록 지정
    }
}
