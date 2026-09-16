package com.project.web.controller;

import com.project.web.model.FavoriteEvent;
import com.project.web.model.FestivalEntity;
import com.project.web.repository.FavoriteEventRepository;
import com.project.web.repository.FestivalRepository;

import com.project.web.service.FestivalService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.project.web.service.FestivalWriteGuard;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.ZoneId;

//행사 상세 조회와 즐겨찾기 API를 담당하는 컨트롤러
//외부 API 데이터와 내부 DB의 festivalId를 연결해 상세 페이지, 즐겨찾기 기능에서 사용
@Controller
public class FestivalController {

	private static final Logger log = LoggerFactory.getLogger(FestivalController.class);

	private final FavoriteEventRepository favoriteEventRepository;
	private final FestivalService festivalService;
	private final FestivalRepository festivalRepository;

	public FestivalController(
	        FavoriteEventRepository favoriteEventRepository,
	        FestivalService festivalService,
	        FestivalRepository festivalRepository
	){
	    this.favoriteEventRepository = favoriteEventRepository;
	    this.festivalService = festivalService;
	    this.festivalRepository = festivalRepository;
	}

	@GetMapping("/main")
	public String getMainPage() {
		return "main";
	}

	// 기존 제목 기반 상세 URL을 유지하기 위한 호환용 엔드포인트
	// 제목은 특수문자/중복 문제가 있을 수 있으므로,
	// 실제 상세 페이지는 내부 festivalId 기반 URL로 리다이렉트
	@GetMapping("/festival/{title:.+}")
	public String festivalDetail(
	        @PathVariable("title") String title
	) {
	    try {
	        String decodedTitle = URLDecoder.decode(
	                title,
	                StandardCharsets.UTF_8.name()
	        );

	        String normalizedTitle =
	                festivalService.normalize(decodedTitle);

	        FestivalEntity festival =
	                festivalService.getActiveFestivalByNormalizedTitle(
	                        normalizedTitle
	                );

	        return "redirect:/festival/id/" + festival.getId();

	    } catch (Exception e) {
	        log.error("행사 상세 페이지 이동 실패. title={}", title, e);
	        return "error";
	    }
	}

	@GetMapping("/festival/id/{festivalId}")
	public String festivalDetailById(
	        @PathVariable("festivalId") Long festivalId,
	        Model model,
	        Principal principal) {

	    try {

	    	String today = LocalDate.now(
	    	        ZoneId.of("Asia/Seoul")
	    	).toString();

	    	// 일반 상세 주소로 들어와도 종료 행사라면 보관함 상세로 이동
	    	if (festivalRepository
	    	        .findEndedFestivalById(festivalId, today)
	    	        .isPresent()) {

	    	    return "redirect:/ended/festival/" + festivalId;
	    	}
	    	
	    	FestivalEntity entity =
	    	        festivalRepository
	    	                .findById(festivalId)
	                        .orElseThrow(
	                                () -> new IllegalArgumentException(
	                                        "Festival not found id=" + festivalId));

	        model.addAttribute("festivalId", festivalId);
	        model.addAttribute("festival", entity);
		     // 종료일 전이라도 API에서 사라져 비활성화된 행사는
		     // 마지막으로 저장된 정보와 기존 리뷰를 읽기 전용으로 제공한다.
		     if (!entity.isActive()) {
		         model.addAttribute("inactiveArchive", true);
		         return "endedFestivalDetail";
		     }
		     
	        model.addAttribute(
	                "currentUsername",
	                principal != null ? principal.getName() : "");

	        return "festivalDetail";

	    } catch (Exception e) {
	        log.error(
	                "Festival detail page failed. festivalId={}",
	                festivalId,
	                e);

	        return "error";
	    }
	}
	
	// 종료 행사 보관함 전용 상세 페이지
	// 종료일이 지난 행사만 열 수 있고, 기존 리뷰를 읽기 전용으로 제공한다.
	@GetMapping("/ended/festival/{festivalId}")
	public String endedFestivalDetailById(
	        @PathVariable("festivalId") Long festivalId,
	        Model model) {

	    try {
	        FestivalEntity entity =
	                festivalService.getEndedFestivalById(festivalId);

	        model.addAttribute("festival", entity);
	        model.addAttribute("inactiveArchive", false);

	        return "endedFestivalDetail";

	    } catch (Exception e) {
	        log.error("종료 행사 상세 페이지 이동 실패. festivalId={}", festivalId, e);
	        return "error";
	    }
	}
	

	// 로그인한 사용자가 특정 행사를 즐겨찾기에 추가
	// 중복 추가를 막기 위해 username + eventId 조합을 확인
	@PostMapping("/addFavoriteEvent")
	@ResponseBody
	public ResponseEntity<?> addFavoriteEvent(
	        @RequestBody Map<String, String> payload,
	        Principal principal) {

	    if (principal == null) {
	        return ResponseEntity.status(401)
	                .body("로그인이 필요합니다.");
	    }

	    String rawEventId = payload.get("event_id");

	    if (rawEventId == null || rawEventId.isBlank()) {
	        return ResponseEntity.badRequest()
	                .body("행사 ID가 필요합니다.");
	    }

	    Long festivalId;

	    try {
	        festivalId = Long.valueOf(rawEventId.trim());
	    } catch (NumberFormatException e) {
	        return ResponseEntity.badRequest()
	                .body("행사 ID는 올바른 숫자여야 합니다.");
	    }

	    if (festivalId <= 0) {
	        return ResponseEntity.badRequest()
	                .body("행사 ID는 양수여야 합니다.");
	    }

	    FestivalEntity festival = festivalRepository
	            .findById(festivalId)
	            .orElse(null);

	    if (festival == null) {
	        return ResponseEntity.status(404)
	                .body("행사를 찾을 수 없습니다.");
	    }

	    try {
	        FestivalWriteGuard.check(festival);
	    } catch (FestivalWriteGuard.WriteNotAllowedException e) {
	        return ResponseEntity.status(409)
	                .body(e.getMessage());
	    }

	    String username = principal.getName();
	    String eventId = festivalId.toString();

	    if (favoriteEventRepository
	            .existsByUsernameAndEventId(username, eventId)) {
	        return ResponseEntity.ok("이미 즐겨찾기입니다.");
	    }

	    try {
	        favoriteEventRepository.saveAndFlush(
	                new FavoriteEvent(username, eventId)
	        );

	        return ResponseEntity.ok("즐겨찾기에 추가되었습니다.");

	    } catch (DataIntegrityViolationException e) {

	        // 동시에 추가된 경우인지 실제로 확인
	        if (favoriteEventRepository
	                .existsByUsernameAndEventId(username, eventId)) {
	            return ResponseEntity.ok("이미 즐겨찾기입니다.");
	        }

	        log.error(
	                "즐겨찾기 저장 실패. festivalId={}",
	                festivalId,
	                e
	        );

	        return ResponseEntity.status(500)
	                .body("즐겨찾기 저장 중 오류가 발생했습니다.");
	    }
	}

	// 현재 로그인한 사용자의 즐겨찾기 행사 ID 목록을 반환
	// 프론트에서 즐겨찾기 버튼 상태를 표시할 때 사용
	@GetMapping("/getFavoriteEvents")
	@ResponseBody
	public ResponseEntity<?> getFavoriteEvents(Principal principal) {
		if (principal == null) {
			return ResponseEntity.status(401).body(List.of());
		}

		String username = principal.getName();

		List<String> favoriteEventIds = favoriteEventRepository.findAllByUsername(username).stream()
				.map(FavoriteEvent::getEventId).collect(Collectors.toList());

		return ResponseEntity.ok(favoriteEventIds);
	}

	// 현재 로그인한 사용자의 즐겨찾기 목록에서 선택한 행사를 제거
	@DeleteMapping("/removeFavoriteEvent")
	@ResponseBody
	public ResponseEntity<?> removeFavoriteEvent(@RequestBody Map<String, String> payload, Principal principal) {
		if (principal == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}

		String username = principal.getName();
		String eventId = payload.get("event_id");

		if (eventId == null || eventId.isBlank()) {
			return ResponseEntity.badRequest().body("event_id가 필요합니다.");
		}

		favoriteEventRepository.deleteByUsernameAndEventId(username, eventId);
		return ResponseEntity.ok("즐겨찾기에서 제거되었습니다.");
	}
}