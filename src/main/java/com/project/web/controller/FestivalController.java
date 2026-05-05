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

@Controller
public class FestivalController {

	private static final Logger log = LoggerFactory.getLogger(FestivalController.class);
	
    private final FavoriteEventRepository favoriteEventRepository;
    private final FestivalIdentityService festivalIdentityService;
    private final FestivalService festivalService;
    private final FestivalRepository festivalRepository;

    
    public FestivalController(FavoriteEventRepository favoriteEventRepository,
                              FestivalIdentityService festivalIdentityService,
                              FestivalService festivalService,
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

    // 제목 기반 접근은 유지하되, 실제 상세 진입은 id 기반으로 리다이렉트
    @GetMapping("/festival/{title:.+}")
    public String festivalDetail(@PathVariable("title") String title) {
        try {
            String decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8.name());
            String normalizedTitle = festivalService.normalize(decodedTitle);

            FestivalResponse.Row festival = festivalService.getFestivalByNormalizedTitle(normalizedTitle);

            String identityKey = festivalService.createFestivalIdentityKey(festival);

            Long festivalId = festivalIdentityService.getOrCreateFestivalId(
                    identityKey,
                    festivalService.normalize(festival.getTitle()),
                    festival.getTitle()
            );

            return "redirect:/festival/id/" + festivalId;
        } catch (Exception e) {
            log.error("Festival detail redirect failed. title={}", title, e);
            return "error";
        }
    }

    // 실제 상세페이지는 id로 조회
    @GetMapping("/festival/id/{festivalId}")
    public String festivalDetailById(@PathVariable("festivalId") Long festivalId,
                                     Model model,
                                     Principal principal) {
        try {
            FestivalEntity entity = festivalRepository.findById(festivalId)
                    .orElseThrow(() -> new IllegalArgumentException("Festival not found id=" + festivalId));

            FestivalResponse.Row festival = festivalService.getFestivalData().getRow().stream()
                    .filter(row -> festivalService.createFestivalIdentityKey(row).equals(entity.getIdentityKey()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Festival not found by identityKey: " + entity.getIdentityKey()
                    ));

            model.addAttribute("festivalId", festivalId);
            model.addAttribute("festival", festival);
            model.addAttribute("currentUsername", principal != null ? principal.getName() : "");

            return "festivalDetail";
        } catch (Exception e) {
            log.error("Festival detail page failed. festivalId={}", festivalId, e);
            return "error";
        }
    }

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
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.ok("이미 즐겨찾기입니다.");
        }
    }

    @GetMapping("/getFavoriteEvents")
    @ResponseBody
    public ResponseEntity<?> getFavoriteEvents(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(List.of());
        }

        String username = principal.getName();

        List<String> favoriteEventIds = favoriteEventRepository.findAllByUsername(username)
                .stream()
                .map(FavoriteEvent::getEventId)
                .collect(Collectors.toList());

        return ResponseEntity.ok(favoriteEventIds);
    }

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