package com.project.web.controller;

import com.project.web.model.FestivalResponse;
import com.project.web.repository.FestivalRepository;
import com.project.web.repository.SyncStatusRepository;
import com.project.web.service.FestivalIdentityService;
import com.project.web.service.FestivalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.project.web.model.FestivalEntity;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 행사 카테고리·종료 행사 목록 화면을 구성하는 MVC 컨트롤러다.
 * 목록 데이터는 외부 API를 직접 호출하지 않고, 동기화된 DB 데이터만 페이지 단위로 조회한다.
 */
@Controller
public class PageMoveController {

	private final FestivalService festivalService;
	private final FestivalIdentityService festivalIdentityService;
	private final SyncStatusRepository syncStatusRepository;
	
	public PageMoveController(
			FestivalService festivalService, 
			FestivalIdentityService festivalIdentityService, 
			SyncStatusRepository syncStatusRepository) {
		this.festivalService = festivalService;
		this.festivalIdentityService = festivalIdentityService;
		this.syncStatusRepository = syncStatusRepository;
	}

	// 문화 행사 페이지로 이동
	@GetMapping("/culture")
	public String moveCulturePage(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "16") int size,
			@RequestParam(name = "sort", defaultValue = "latest") String sort,
	        Model model
	) {
	    return moveCategoryPage(
	            "행사",
	            "culture",
	            "/culture",
	            page,
	            size,
	            sort,
	            model
	    );
	}

	@GetMapping("/education")
	public String moveEducationPage(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "16") int size,
			@RequestParam(name = "sort", defaultValue = "latest") String sort,
	        Model model
	) {
	    return moveCategoryPage(
	            "교육",
	            "education",
	            "/education",
	            page,
	            size,
	            sort,
	            model
	    );
	}

	@GetMapping("/exhibition")
	public String moveExhibitionPage(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "16") int size,
			@RequestParam(name = "sort", defaultValue = "latest") String sort,
	        Model model
	) {
	    return moveCategoryPage(
	            "전시",
	            "exhibition",
	            "/exhibition",
	            page,
	            size,
	            sort,
	            model
	    );
	}

	@GetMapping("/performance")
	public String movePerformancePage(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "16") int size,
			@RequestParam(name = "sort", defaultValue = "latest") String sort,
	        Model model
	) {
	    return moveCategoryPage(
	            "공연",
	            "performance",
	            "/performance",
	            page,
	            size,
	            sort,
	            model
	    );
	}
	
	/**
	 * 종료일이 지난 행사를 카테고리와 페이지 크기 기준으로 조회한다.
	 * 종료 행사도 상세 조회는 가능하지만 새 리뷰 작성은 별도 상세 화면에서 제한한다.
	 */
	@GetMapping("/ended")
	public String moveEndedPage(
	        @RequestParam(name = "page", defaultValue = "0") int page,
	        @RequestParam(name = "size", defaultValue = "16") int size,
	        @RequestParam(name = "category", defaultValue = "all") String category,
	        Model model
	) {
	    int safeSize = normalizeSize(size);
	    int safePage = Math.max(page, 0);
	    String safeCategory = normalizeEndedCategory(category);

	    // 종료일이 같아도 페이지 간 정렬 순서가 일정하도록 ID를 함께 사용한다.
	    Sort sortOption = Sort.by(
	            Sort.Direction.DESC,
	            "endDe",
	            "id"
	    );

	    Page<FestivalEntity> festivalPage =
	            festivalService.getEndedFestivals(
	                    safeCategory,
	                    PageRequest.of(safePage, safeSize, sortOption)
	            );

	    // 존재하지 않는 큰 페이지 번호를 요청하면 마지막 페이지를 조회한다.
	    int lastPage = Math.max(festivalPage.getTotalPages() - 1, 0);

	    if (safePage > lastPage) {
	        safePage = lastPage;

	        festivalPage = festivalService.getEndedFestivals(
	                safeCategory,
	                PageRequest.of(safePage, safeSize, sortOption)
	        );
	    }

	    // 페이지 번호를 10개씩 묶어서 표시한다.
	    // 내부 번호는 0부터 시작하고, 화면에서는 1을 더해 표시한다.
	    int pageBlockSize = 10;
	    int startPage = (safePage / pageBlockSize) * pageBlockSize;
	    int endPage = Math.min(
	            startPage + pageBlockSize - 1,
	            Math.max(festivalPage.getTotalPages() - 1, 0)
	    );

	    model.addAttribute("startPage", startPage);
	    model.addAttribute("endPage", endPage);

	    model.addAttribute("festivalData", festivalPage.getContent());
	    model.addAttribute("festivalPage", festivalPage);
	    model.addAttribute("size", safeSize);
	    model.addAttribute("selectedCategory", safeCategory);
	    model.addAttribute("basePath", "/ended");
	    model.addAttribute(
	            "endedFestivalCount",
	            festivalPage.getTotalElements()
	    );

	    addLastSyncTime(model);

	    return "ended";
	}
	
	// API에서 받은 행사 데이터에는 내부 DB id가 없으므로,
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
	
	
	// 목록 화면 공통 영역에 마지막 정상 동기화 시각을 전달한다. 
	private void addLastSyncTime(Model model) {
	    syncStatusRepository
	            .findById(1L)
	            .ifPresent(status ->
	                    model.addAttribute(
	                            "lastSyncTime",
	                            status.getLastSuccessTime()
	                    )
	            );
	}
	
	/**
	 * 네개의 카테고리 목록 화면에서 공통으로 사용하는 DB 페이징 처리
	 * URL로 전달된 크기·정렬 값은 허용 목록으로 정규화해 예상하지 못한 값이 쿼리에 전달되지 않게 한다.
	 */
	private String moveCategoryPage(
	        String category,
	        String viewName,
	        String basePath,
	        int page,
	        int size,
	        String sort,
	        Model model
	) {
	    int safeSize = normalizeSize(size);
	    int safePage = Math.max(page, 0);
	    String safeSort = normalizeSort(sort);

	    // 날짜가 같은 행사도 페이지 간 정렬 순서가 일정하도록 ID를 사용한다.
	    Sort sortOption;

	    if ("deadline".equals(safeSort)) {
	        sortOption = Sort.by(
	                Sort.Direction.ASC,
	                "endDe",
	                "id"
	        );
	    } else {
	        sortOption = Sort.by(
	                Sort.Direction.DESC,
	                "beginDe",
	                "id"
	        );
	    }

	    Page<FestivalEntity> festivalPage =
	            festivalService.getByCategory(
	                    category,
	                    PageRequest.of(safePage, safeSize, sortOption)
	            );

	    // 범위를 벗어난 페이지를 요청하면 마지막 유효 페이지를 조회한다.
	    int lastPage = Math.max(festivalPage.getTotalPages() - 1, 0);

	    if (safePage > lastPage) {
	        safePage = lastPage;

	        festivalPage = festivalService.getByCategory(
	                category,
	                PageRequest.of(safePage, safeSize, sortOption)
	        );
	    }

	    // 페이지 번호를 1~10, 11~20처럼 10개씩 표시한다.
	    int pageBlockSize = 10;
	    int startPage = (safePage / pageBlockSize) * pageBlockSize;
	    int endPage = Math.min(
	            startPage + pageBlockSize - 1,
	            Math.max(festivalPage.getTotalPages() - 1, 0)
	    );

	    model.addAttribute("startPage", startPage);
	    model.addAttribute("endPage", endPage);

	    model.addAttribute("festivalData", festivalPage.getContent());
	    model.addAttribute("festivalPage", festivalPage);
	    model.addAttribute("size", safeSize);
	    model.addAttribute("sort", safeSort);
	    model.addAttribute("basePath", basePath);

	    addLastSyncTime(model);

	    return viewName;
	}

	// 카드가 4열로 표시되는 화면 구조에 맞춰 16개 또는 32개만 허용한다.
	private int normalizeSize(int size) {
	    return size == 32 ? 32 : 16;
	}

	// 지원하지 않는 정렬 값은 기본 최신순으로 되돌린다.
	private String normalizeSort(String sort) {
	    return "deadline".equals(sort) ? "deadline" : "latest";
	}

	// 종료 행사 보관함에서 허용하는 카테고리만 통과시키고 나머지는 전체 조회로 처리한다.
	private String normalizeEndedCategory(String category) {
	    return switch (category) {
	        case "행사", "전시", "공연", "교육" -> category;
	        default -> "all";
	    };
	}
}
