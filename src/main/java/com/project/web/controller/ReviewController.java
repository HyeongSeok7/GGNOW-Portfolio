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

@RestController
@RequestMapping("/festivals/{festivalId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<Review>> getReviews(@PathVariable("festivalId") Long festivalId) {
        return ResponseEntity.ok(reviewService.getReviewsByFestivalId(festivalId));
    }

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

    // ✅ 리뷰 수정 (작성자만)
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

    // ✅ 리뷰 삭제 (작성자만)
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