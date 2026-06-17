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

	@Mock
	private ReviewRepository reviewRepository;
	
	@Mock
	private FestivalRepository festivalRepository;
	
	@InjectMocks
	private ReviewService reviewService;
	
	@Test
	void 리뷰작성_성공() {
		FestivalEntity festival = new FestivalEntity();
		
		 festival.setId(1L);
	        festival.setTitle("벚꽃축제");

	        when(festivalRepository.findById(1L))
	                .thenReturn(Optional.of(festival));

	        reviewService.addReviewByFestivalId(
	                1L,
	                "재밌어요",
	                "tester"
	        );

	        verify(reviewRepository)
	                .save(any(Review.class));
	    }
	
	@Test
	void 리뷰수정_성공() {

	    Review review = new Review();

	    review.setId(1L);
	    review.setFestivalId(1L);
	    review.setUsername("tester");
	    review.setContent("원본");

	    when(reviewRepository.findById(1L))
	            .thenReturn(Optional.of(review));

	    reviewService.updateReview(
	            1L,
	            1L,
	            "수정내용",
	            "tester"
	    );

	    assertEquals(
	            "수정내용",
	            review.getContent()
	    );

	    verify(reviewRepository)
	            .save(review);
	}
	
	@Test
	void 리뷰수정_작성자아님() {

	    Review review = new Review();

	    review.setFestivalId(1L);
	    review.setUsername("kim");

	    when(reviewRepository.findById(1L))
	            .thenReturn(Optional.of(review));

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

	    Review review = new Review();

	    review.setFestivalId(1L);
	    review.setUsername("tester");

	    when(reviewRepository.findById(1L))
	            .thenReturn(Optional.of(review));

	    reviewService.deleteReview(
	            1L,
	            1L,
	            "tester"
	    );

	    verify(reviewRepository)
	            .delete(review);
	}
	
	}

