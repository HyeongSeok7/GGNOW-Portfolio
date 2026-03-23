package com.project.web.controller;



import com.project.web.service.FestivalIdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.project.web.model.FestivalResponse;
import com.project.web.service.FestivalService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
public class PageMoveController {

	private final FestivalService festivalService;
	private final FestivalIdentityService festivalIdentityService;

    public PageMoveController(FestivalService festivalService,
							  FestivalIdentityService festivalIdentityService) {
        this.festivalService = festivalService;
		this.festivalIdentityService = festivalIdentityService;
    }
	
	  @GetMapping("/culture")	//HTTP GET요청을 "/culture" URL로 매핑
	    public String moveCulturePage(Model model) {
		  FestivalResponse festivalResponse = festivalService.getFestivalData();

		  Map<String, Long> festivalIdMap = festivalResponse.getRow().stream()
				  .collect(Collectors.toMap(
						  FestivalResponse.Row::getTitle,
						  row -> festivalIdentityService.getOrCreateFestivalId(
								  festivalService.normalize(row.getTitle()),
								  row.getTitle()
						  ),
						  (a, b) -> a,
						  LinkedHashMap::new
				  ));

		  model.addAttribute("festivalData", festivalResponse);
		  model.addAttribute("festivalIdMap", festivalIdMap);
		  return "culture";  // "culture.html" 이라는 뷰를 렌더링함
	    }
	  
	  @GetMapping("/education")
	    public String moveEducationPage(Model model) {
		  FestivalResponse festivalResponse = festivalService.getFestivalData();

		  Map<String, Long> festivalIdMap = festivalResponse.getRow().stream()
				  .collect(Collectors.toMap(
						  FestivalResponse.Row::getTitle,
						  row -> festivalIdentityService.getOrCreateFestivalId(
								  festivalService.normalize(row.getTitle()),
								  row.getTitle()
						  ),
						  (a, b) -> a,
						  LinkedHashMap::new
				  ));

		  model.addAttribute("festivalData", festivalResponse);
		  model.addAttribute("festivalIdMap", festivalIdMap);
		  return "education";  // "education.html"이라는 뷰를 렌더링함	    }
	    }
	  
	  @GetMapping("/exhibition")
	    public String moveExhibitionPage(Model model) {
		  FestivalResponse festivalResponse = festivalService.getFestivalData();

		  Map<String, Long> festivalIdMap = festivalResponse.getRow().stream()
				  .collect(Collectors.toMap(
						  FestivalResponse.Row::getTitle,
						  row -> festivalIdentityService.getOrCreateFestivalId(
								  festivalService.normalize(row.getTitle()),
								  row.getTitle()
						  ),
						  (a, b) -> a,
						  LinkedHashMap::new
				  ));

		  model.addAttribute("festivalData", festivalResponse);
		  model.addAttribute("festivalIdMap", festivalIdMap);
		  return "exhibition";  // "exhibition.html"이라는 뷰를 렌더링함	    }
	    }
	  
	  @GetMapping("/performance")
	    public String movePerformancePage(Model model) {
		  FestivalResponse festivalResponse = festivalService.getFestivalData();

		  Map<String, Long> festivalIdMap = festivalResponse.getRow().stream()
				  .collect(Collectors.toMap(
						  FestivalResponse.Row::getTitle,
						  row -> festivalIdentityService.getOrCreateFestivalId(
								  festivalService.normalize(row.getTitle()),
								  row.getTitle()
						  ),
						  (a, b) -> a,
						  LinkedHashMap::new
				  ));

		  model.addAttribute("festivalData", festivalResponse);
		  model.addAttribute("festivalIdMap", festivalIdMap);
		  return "performance";  // "performance.html"이라는 뷰를 렌더링함	    }
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
