package com.project.web.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.web.model.FestivalEntity;
import com.project.web.model.Review;
import com.project.web.repository.FestivalRepository;
import com.project.web.repository.ReviewRepository;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

	/**
	 * 리뷰 서비스 단위 테스트
	 *
	 * 검증 내용
	 * - 리뷰 작성
	 * - 리뷰 수정
	 * - 작성자 권한 검증
	 * - 리뷰 삭제
	 */
	
	@Mock
	private ReviewRepository reviewRepository;
	
	@Mock
	private FestivalRepository festivalRepository;
	
	@InjectMocks
	private ReviewService reviewService;
	
	@Test
	void 리뷰작성_성공() {
		
		// 축제 데이터 생성
		FestivalEntity festival = new FestivalEntity();
		 
		 festival.setId(1L);
	     festival.setTitle("벚꽃축제");

	        // 축제가 존재한다는 가정
	        when(festivalRepository.findById(1L))
	                .thenReturn(Optional.of(festival));

	        // 리뷰등록 실행
	        reviewService.addReviewByFestivalId(
	                1L,
	                "재밌어요",
	                "tester"
	        );
	        // 리뷰 저장이 호출되었는지 검증
	        verify(reviewRepository)
	                .save(any(Review.class));
	    }
	
	@Test
	void 리뷰수정_성공() {

		//기존 리뷰 생성
	    Review review = new Review();

	    review.setId(1L);
	    review.setFestivalId(1L);
	    review.setUsername("tester");
	    review.setContent("원본");

	    // 리뷰 조회 결과 반환 설정
	    when(reviewRepository.findById(1L))
	            .thenReturn(Optional.of(review));

	    // 리뷰 수정 실행
	    reviewService.updateReview(
	            1L,
	            1L,
	            "수정내용",
	            "tester"
	    );

	    // 내용이 정상 변경됐는지 검증
	    assertEquals(
	            "수정내용",
	            review.getContent()
	    );

	    // 수정된 리뷰 저장 여부 검증
	    verify(reviewRepository)
	            .save(review);
	}
	
	@Test
	void 리뷰수정_작성자아닐때() {

		//작성자가 kim인 리뷰 생성
	    Review review = new Review();

	    review.setFestivalId(1L);
	    review.setUsername("kim");


	    when(reviewRepository.findById(1L))
	            .thenReturn(Optional.of(review));
	    
	    //다른 사용자가 수정 시도하면 예외 발생 검증
	    assertThrows(
	            AccessDeniedException.class,
	            () -> reviewService.updateReview(
	                    1L,
	                    1L,
	                    "수정",
	                    "park"
	            )
	    );
	}
	
	@Test
	void 리뷰삭제_성공() {

		//삭제 대상 리뷰 생성
	    Review review = new Review();

	    review.setFestivalId(1L);
	    review.setUsername("tester");

	    when(reviewRepository.findById(1L))
	            .thenReturn(Optional.of(review));

	    //리뷰 삭제 실행
	    reviewService.deleteReview(
	            1L,
	            1L,
	            "tester"
	    );

	    //삭제 메소드 호출 여부 검증
	    verify(reviewRepository)
	            .delete(review);
	}
	
}

