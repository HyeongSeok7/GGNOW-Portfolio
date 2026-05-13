package com.project.web.controller;

import com.project.web.model.FavoriteEvent;
import com.project.web.model.FestivalEntity;
import com.project.web.model.FestivalResponse;
import com.project.web.repository.FavoriteEventRepository;
import com.project.web.repository.FestivalRepository;
import com.project.web.service.FestivalIdentityService;
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

//행사 상세 조회와 즐겨찾기 API를 담당하는 컨트롤러
//외부 API 데이터와 내부 DB의 festivalId를 연결해 상세 페이지, 즐겨찾기 기능에서 사용
@Controller
public class FestivalController {

	private static final Logger log = LoggerFactory.getLogger(FestivalController.class);

	private final FavoriteEventRepository favoriteEventRepository;
	private final FestivalIdentityService festivalIdentityService;
	private final FestivalService festivalService;
	private final FestivalRepository festivalRepository;

	public FestivalController(FavoriteEventRepository favoriteEventRepository,
			FestivalIdentityService festivalIdentityService, FestivalService festivalService,
			FestivalRepository festivalRepository) {
		this.favoriteEventRepository = favoriteEventRepository;
		this.festivalIdentityService = festivalIdentityService;
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
	public String festivalDetail(@PathVariable("title") String title) {
		try {
			// URL 인코딩된 제목을 복원한 뒤 검색/비교가 가능하도록 정규화한다
			String decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8.name());
			String normalizedTitle = festivalService.normalize(decodedTitle);

			FestivalResponse.Row festival = festivalService.getFestivalByNormalizedTitle(normalizedTitle);

			// 외부 API에는 안정적인 내부 PK가 없기 때문에,
			// 행사 정보를 조합한 identityKey를 기준으로 DB의 festivalId를 생성하거나 조회
			String identityKey = festivalService.createFestivalIdentityKey(festival);

			Long festivalId = festivalIdentityService.getOrCreateFestivalId(identityKey,
					festivalService.normalize(festival.getTitle()), festival.getTitle());

			return "redirect:/festival/id/" + festivalId;
		} catch (Exception e) {
			log.error("Festival detail redirect failed. title={}", title, e);
			return "error";
		}
	}

	// 내부 festivalId 기준 상세 페이지 조회
	// DB에는 identityKey만 저장, 실제 화면에 보여줄 상세 데이터는 최신 외부 API 응답에서 다시 찾는다
	@GetMapping("/festival/id/{festivalId}")
	public String festivalDetailById(@PathVariable("festivalId") Long festivalId, Model model, Principal principal) {
		try {
			// festivalId로 내부 식별 정보를 먼저 조회
			FestivalEntity entity = festivalRepository.findById(festivalId)
					.orElseThrow(() -> new IllegalArgumentException("Festival not found id=" + festivalId));

			// DB에 저장된 identityKey와 외부 API Row의 identityKey를 비교해
			// 현재 API 응답 중 같은 행사를 찾아 상세 페이지에 전달
			FestivalResponse.Row festival = festivalService.getFestivalData().getRow().stream()
					.filter(row -> festivalService.createFestivalIdentityKey(row).equals(entity.getIdentityKey()))
					.findFirst().orElseThrow(() -> new IllegalArgumentException(
							"Festival not found by identityKey: " + entity.getIdentityKey()));

			model.addAttribute("festivalId", festivalId);
			model.addAttribute("festival", festival);
			model.addAttribute("currentUsername", principal != null ? principal.getName() : "");

			return "festivalDetail";
		} catch (Exception e) {
			log.error("Festival detail page failed. festivalId={}", festivalId, e);
			return "error";
		}
	}

	// 로그인한 사용자가 특정 행사를 즐겨찾기에 추가
	// 중복 추가를 막기 위해 username + eventId 조합을 확인
	@PostMapping("/addFavoriteEvent")
	@ResponseBody
	public ResponseEntity<?> addFavoriteEvent(@RequestBody Map<String, String> payload, Principal principal) {
		if (principal == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}

		String username = principal.getName();
		String eventId = payload.get("event_id");

		if (eventId == null || eventId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body("event_id가 필요합니다.");
		}

		eventId = eventId.trim();

		if (favoriteEventRepository.existsByUsernameAndEventId(username, eventId)) {
			return ResponseEntity.ok("이미 즐겨찾기입니다.");
		}

		try {
			favoriteEventRepository.saveAndFlush(new FavoriteEvent(username, eventId));
			return ResponseEntity.ok("즐겨찾기에 추가되었습니다.");
			// 동시에 같은 즐겨찾기 요청이 들어와도 DB unique 제약조건으로 중복 저장을 방지
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			return ResponseEntity.ok("이미 즐겨찾기입니다.");
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