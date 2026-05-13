package com.project.web.repository;

import com.project.web.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
	List<Review> findByUsername(String username);

	// 특정 행사 ID로 리뷰 필터링
	List<Review> findByFestivalId(Long festivalId);

	List<Review> findByUsernameOrderByCreatedAtDesc(String username);

	List<Review> findByFestivalIdOrderByCreatedAtDesc(Long festivalId);
}
