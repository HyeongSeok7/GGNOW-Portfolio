package com.project.web.controller;

import com.project.web.dto.ReviewCreateRequest;
import com.project.web.dto.ReviewUpdateRequest;
import com.project.web.model.Review;
import com.project.web.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

//행사 상세 페이지에서 사용하는 리뷰 REST API 컨트롤러
//리뷰 조회는 비회원도 가능하지만, 작성/수정/삭제는 로그인 사용자만 가능
@RestController
@RequestMapping("/festivals/{festivalId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // 특정 행사에 작성된 리뷰 목록 조회
    @GetMapping
    public ResponseEntity<List<Review>> getReviews(@PathVariable("festivalId") Long festivalId) {
        return ResponseEntity.ok(reviewService.getReviewsByFestivalId(festivalId));
    }

    // 로그인한 사용자가 특정 행사에 리뷰를 작성
    // festivalId를 기준으로 어떤 행사에 작성된 리뷰인지 연결한다
    @PostMapping
    public ResponseEntity<Map<String, String>> addReview(
            @PathVariable("festivalId") Long festivalId,
            @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다!"));
        }

        String content = request.getContent();
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "리뷰 내용을 입력해주세요!"));
        }

        reviewService.addReviewByFestivalId(festivalId, content.trim(), userDetails.getUsername());
        return ResponseEntity.status(201).body(Map.of("message", "리뷰를 등록했습니다!"));
    }

    // 리뷰 내용 수정
    // ReviewService에서 festivalId 일치 여부와 작성자 본인 여부를 검증
    @PatchMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> updateReview(
            @PathVariable Long festivalId,
            @PathVariable Long reviewId,
            @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다!"));
        }
        String content = request.getContent();
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "리뷰 내용을 입력해주세요!"));
        }

        try {
            reviewService.updateReview(festivalId, reviewId, content.trim(), userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "리뷰를 수정했습니다!"));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() == null ? "잘못된 요청입니다." : e.getMessage();
            if (msg.contains("review not found")) {
                return ResponseEntity.status(404).body(Map.of("message", "리뷰를 찾을 수 없습니다."));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "잘못된 요청입니다."));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("message", "작성자만 수정할 수 있습니다."));
        }
    }

    // 리뷰를 삭제
    // ReviewService에서 작성자 본인인지 확인한 뒤 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(
            @PathVariable("festivalId") Long festivalId,
            @PathVariable("reviewId") Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다!"));
        }

        try {
            reviewService.deleteReview(festivalId, reviewId, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "리뷰를 삭제했습니다!"));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() == null ? "잘못된 요청입니다." : e.getMessage();
            if (msg.contains("review not found")) {
                return ResponseEntity.status(404).body(Map.of("message", "리뷰를 찾을 수 없습니다."));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "잘못된 요청입니다."));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("message", "작성자만 삭제할 수 있습니다."));
        }
    }
}