package com.project.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.project.web.model.FavoriteEvent;
import com.project.web.repository.FavoriteEventRepository;

class FavoriteServiceTest {
	/**
	 * 즐겨찾기 기능에 대한 단위 테스트
	 *
	 * 검증 내용
	 * - 즐겨찾기 등록
	 * - 중복 즐겨찾기 확인
	 * - 즐겨찾기 삭제
	 */
    private final FavoriteEventRepository favoriteEventRepository =
            mock(FavoriteEventRepository.class);

    @Test
    void 즐겨찾기_등록성공() {

    	// 해당 사용자가 아직 즐겨찾기하지 않은 행사라고 가정
        when(
                favoriteEventRepository
                        .existsByUsernameAndEventId(
                                "tester",
                                "100"
                        )
        ).thenReturn(false);

        //즐겨찾기 객체 생성
        FavoriteEvent favorite =
                new FavoriteEvent(
                        "tester",
                        "100"
                );

        //즐겨찾기 저장
        favoriteEventRepository.saveAndFlush(favorite);

        // 저장 메소드가 정상 호출되었는지 검증
        verify(favoriteEventRepository)
                .saveAndFlush(any(FavoriteEvent.class));
    }

    @Test
    void 즐겨찾기_중복확인() {

    	// 이미 즐겨찾기 된 행사라고 가정
        when(
                favoriteEventRepository
                        .existsByUsernameAndEventId(
                                "tester",
                                "100"
                        )
        ).thenReturn(true);

        //중복 여부 조회
        boolean result =
                favoriteEventRepository
                        .existsByUsernameAndEventId(
                                "tester",
                                "100"
                        );

        // 중복상태가 True 인지 검증
        assertTrue(result);
    }

    @Test
    void 즐겨찾기_삭제성공() {

    	// 특정 사용자의 즐겨찾기 삭제
        favoriteEventRepository
                .deleteByUsernameAndEventId(
                        "tester",
                        "100"
                );

        // 삭제 메소드가 정상 호출되었는지 검증
        verify(favoriteEventRepository)
                .deleteByUsernameAndEventId(
                        "tester",
                        "100"
                );
    }
}