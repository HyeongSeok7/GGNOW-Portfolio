package com.project.web.service;

import com.project.web.model.FestivalEntity;
import com.project.web.model.Review;
import com.project.web.repository.FestivalRepository;
import com.project.web.repository.ReviewRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

//행사 리뷰 조회, 작성, 수정, 삭제 비즈니스 로직을 담당
//수정/삭제 시 컨트롤러가 아닌 서비스 계층에서 작성자 권한을 검증
@Service
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final FestivalRepository festivalRepository;

	public ReviewService(ReviewRepository reviewRepository, FestivalRepository festivalRepository) {
		this.reviewRepository = reviewRepository;
		this.festivalRepository = festivalRepository;
	}

	// 특정 행사에 작성된 리뷰를 최신순으로 조회
	public List<Review> getReviewsByFestivalId(Long festivalId) {
		return reviewRepository.findByFestivalIdOrderByCreatedAtDesc(festivalId);
	}

	// 리뷰 작성 전 festivalId가 실제 내부 행사 테이블에 존재하는지 확인
	// 리뷰에는 행사 제목을 스냅샷으로 저장해 마이페이지에서 바로 표시할 수 있게 한다
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

	// 마이페이지의 '내 리뷰' 페이지에서 사용할 사용자별 리뷰 목록을 조회
	public List<Review> getReviewsByUsername(String username) {
		return reviewRepository.findByUsernameOrderByCreatedAtDesc(username);
	}

	// 리뷰 수정 로직
	// 요청된 festivalId와 리뷰의 festivalId가 일치하는지 확인하고,
	// 현재 로그인한 사용자가 작성자인 경우에만 수정
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

	// 리뷰 삭제 로직
	// 행사 일치 여부와 작성자 본인 여부를 검증한 뒤 삭제
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