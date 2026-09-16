package com.project.web.repository;

import com.project.web.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
	List<Review> findByUsername(String username);

	// 특정 행사 ID로 리뷰 필터링
	List<Review> findByFestivalId(Long festivalId);

	List<Review> findByUsernameOrderByCreatedAtDesc(String username);

	List<Review> findByFestivalIdOrderByCreatedAtDesc(Long festivalId);
	
	// 본인이 작성한 리뷰 중 연결된 행사의 종류가 일치하는 리뷰를 조회한다.
	// 종료 행사의 리뷰도 포함한다.
	@Query("""
	        SELECT r
	        FROM Review r
	        WHERE r.username = :username
	          AND EXISTS (
	              SELECT f.id
	              FROM FestivalEntity f
	              WHERE f.id = r.festivalId
	                AND f.categoryNm = :category
	          )
	        ORDER BY r.createdAt DESC, r.id DESC
	        """)
	List<Review> findByUsernameAndFestivalCategory(
	        @Param("username") String username,
	        @Param("category") String category
	);
}
