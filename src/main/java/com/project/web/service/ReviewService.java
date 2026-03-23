package com.project.web.service;

import com.project.web.model.FestivalEntity;
import com.project.web.model.Review;
import com.project.web.repository.FestivalRepository;
import com.project.web.repository.ReviewRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final FestivalRepository festivalRepository;

    public ReviewService(ReviewRepository reviewRepository, FestivalRepository festivalRepository) {
        this.reviewRepository = reviewRepository;
        this.festivalRepository = festivalRepository;
    }

    public List<Review> getReviewsByFestivalId(Long festivalId) {
        return reviewRepository.findByFestivalIdOrderByCreatedAtDesc(festivalId);
    }

    public void addReviewByFestivalId(Long festivalId, String content, String username) {
        FestivalEntity festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new IllegalArgumentException("festival not found: " + festivalId));

        Review review = new Review();
        review.setFestivalId(festivalId);
        review.setFestivalTitle(festival.getTitle());
        review.setContent(content);
        review.setUsername(username);
        review.setCreatedAt(LocalDateTime.now());

        reviewRepository.save(review);
    }

    public List<Review> getReviewsByUsername(String username) {
        return reviewRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    /**
     * 리뷰 수정 (작성자만 가능)
     */
    public void updateReview(Long festivalId, Long reviewId, String newContent, String currentUsername) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("review not found: " + reviewId));

        if (!festivalId.equals(review.getFestivalId())) {
            throw new IllegalArgumentException("festivalId mismatch");
        }

        if (!review.getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("not review owner");
        }

        review.setContent(newContent);
        reviewRepository.save(review);
    }

    /**
     * 리뷰 삭제 (작성자만 가능)
     */
    public void deleteReview(Long festivalId, Long reviewId, String currentUsername) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("review not found: " + reviewId));

        if (!festivalId.equals(review.getFestivalId())) {
            throw new IllegalArgumentException("festivalId mismatch");
        }

        if (!review.getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("not review owner");
        }

        reviewRepository.delete(review);
    }
}