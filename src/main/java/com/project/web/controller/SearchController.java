package com.project.web.controller;

import com.project.web.model.FestivalResponse;
import com.project.web.service.FestivalIdentityService;
import com.project.web.service.FestivalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    private final FestivalService festivalService;
    private final FestivalIdentityService festivalIdentityService;

    public SearchController(FestivalService festivalService,
                            FestivalIdentityService festivalIdentityService) {
        this.festivalService = festivalService;
        this.festivalIdentityService = festivalIdentityService;
    }

    @GetMapping("/search")
    public String searchFestivals(@RequestParam("keyword") String keyword, Model model) {
        List<FestivalResponse.Row> filterFestivals = festivalService.searchFestivals(keyword);

        filterFestivals.forEach(festival -> {
            Long festivalId = festivalIdentityService.getOrCreateFestivalId(
                    festivalService.createFestivalIdentityKey(festival),
                    festivalService.normalize(festival.getTitle()),
                    festival.getTitle()
            );
            festival.setFestivalId(festivalId);
        });

        model.addAttribute("festivalData", filterFestivals);
        return "searchlist";
    }
}