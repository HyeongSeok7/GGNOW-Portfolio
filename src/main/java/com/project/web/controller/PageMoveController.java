package com.project.web.controller;

import com.project.web.model.FestivalResponse;
import com.project.web.service.FestivalIdentityService;
import com.project.web.service.FestivalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageMoveController {

    private final FestivalService festivalService;
    private final FestivalIdentityService festivalIdentityService;

    public PageMoveController(FestivalService festivalService,
                              FestivalIdentityService festivalIdentityService) {
        this.festivalService = festivalService;
        this.festivalIdentityService = festivalIdentityService;
    }

    @GetMapping("/culture")
    public String moveCulturePage(Model model) {
        FestivalResponse festivalResponse = festivalService.getFestivalData();
        attachFestivalIds(festivalResponse);
        model.addAttribute("festivalData", festivalResponse);
        return "culture";
    }

    @GetMapping("/education")
    public String moveEducationPage(Model model) {
        FestivalResponse festivalResponse = festivalService.getFestivalData();
        attachFestivalIds(festivalResponse);
        model.addAttribute("festivalData", festivalResponse);
        return "education";
    }

    @GetMapping("/exhibition")
    public String moveExhibitionPage(Model model) {
        FestivalResponse festivalResponse = festivalService.getFestivalData();
        attachFestivalIds(festivalResponse);
        model.addAttribute("festivalData", festivalResponse);
        return "exhibition";
    }

    @GetMapping("/performance")
    public String movePerformancePage(Model model) {
        FestivalResponse festivalResponse = festivalService.getFestivalData();
        attachFestivalIds(festivalResponse);
        model.addAttribute("festivalData", festivalResponse);
        return "performance";
    }

    private void attachFestivalIds(FestivalResponse festivalResponse) {
        if (festivalResponse == null || festivalResponse.getRow() == null) {
            return;
        }

        festivalResponse.getRow().forEach(row -> {
            Long festivalId = festivalIdentityService.getOrCreateFestivalId(
                    festivalService.normalize(row.getTitle()),
                    row.getTitle()
            );
            row.setFestivalId(festivalId);
        });
    }

    @GetMapping("/customer")
    public String moveCustomerPage(Model model) {
        return "customer";
    }

    @GetMapping("/suggestions")
    public String suggestionsNotReady() {
        return "errorpage";
    }
}