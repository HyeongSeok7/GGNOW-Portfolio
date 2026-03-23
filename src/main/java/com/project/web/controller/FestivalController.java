package com.project.web.controller;


import com.project.web.model.FavoriteEvent;
import com.project.web.model.FestivalEntity;
import com.project.web.model.FestivalResponse;

import com.project.web.repository.FavoriteEventRepository;
import com.project.web.repository.FestivalRepository;
import com.project.web.service.FestivalIdentityService;
import com.project.web.service.FestivalService;
import com.project.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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

@Controller
public class FestivalController {

    private final FavoriteEventRepository favoriteEventRepository;
    private final FestivalIdentityService festivalIdentityService;
    private final FestivalService festivalService;
    private final FestivalRepository festivalRepository;

    public FestivalController(FavoriteEventRepository favoriteEventRepository,
                              FestivalIdentityService festivalIdentityService,
                              FestivalService festivalService,
                              FestivalRepository festivalRepository){
        this.favoriteEventRepository = favoriteEventRepository;
        this.festivalIdentityService = festivalIdentityService;
        this.festivalService = festivalService;
        this.festivalRepository = festivalRepository;
    }

    @GetMapping("/main")
    public String getMainPage() {
        return "main";
    }


        //상제페이지 제목을 기반으로 상세페이지 생성 로직
    @GetMapping("/festival/{title:.+}")
    public String festivalDetail(@PathVariable("title") String title, Model model, Principal principal) {
            try {
                // URL 디코딩을 통해 UTF-8 형식으로 제목을 디코딩
                String decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8.name());

                // 제목을 정규화하여 일관된 형식으로 변환
                String normalizedTitle = festivalService.normalize(decodedTitle);

                // 정규화된 제목을 기반으로 Festival 데이터를 가져옴
                FestivalResponse.Row festival = festivalService.getFestivalByTitle(normalizedTitle);
                if (festival == null) {
                    //제목에 해당하는 축제가 없을 경우 예외를 발생시킴
                    throw new IllegalArgumentException("Festival not found for title: " + normalizedTitle);
                }

                Long festivalId = festivalIdentityService.getOrCreateFestivalId(normalizedTitle, festival.getTitle());
                model.addAttribute("festivalId", festivalId);

                model.addAttribute("currentUsername", principal != null ? principal.getName() : "");

                //모델에 festival 속성을 추가하여 뷰에서 사용할 수 있도록 설정
                model.addAttribute("festival", festival);
                return "festivalDetail";    // "festivalDetail.html"뷰를 렌더링 함
            } catch (Exception e) {
                return "errorpage"; // 에러 발생 시 errorpage.html로 이동
            }
        }


        // 즐겨찾기 이벤트 추가
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
    // 사용자의 즐겨찾기 이벤트 조회
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

    //즐겨찾기 삭제
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

    @GetMapping("/festival/id/{festivalId}")
    public String festivalDetailById(@PathVariable("festivalId") Long festivalId, Model model, Principal principal) {
        FestivalEntity entity = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new IllegalArgumentException("Festival not found id=" + festivalId));

        FestivalResponse.Row festival = festivalService.getFestivalByTitle(entity.getTitle());

        model.addAttribute("festivalId", festivalId);
        model.addAttribute("festival", festival);

        model.addAttribute("currentUsername", principal != null ? principal.getName() : "");
        return "festivalDetail";
    }
}
