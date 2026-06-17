package com.project.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.project.web.model.FavoriteEvent;
import com.project.web.repository.FavoriteEventRepository;

class FavoriteServiceTest {

    private final FavoriteEventRepository favoriteEventRepository =
            mock(FavoriteEventRepository.class);

    @Test
    void 즐겨찾기_등록성공() {

        when(
                favoriteEventRepository
                        .existsByUsernameAndEventId(
                                "tester",
                                "100"
                        )
        ).thenReturn(false);

        FavoriteEvent favorite =
                new FavoriteEvent(
                        "tester",
                        "100"
                );

        favoriteEventRepository.saveAndFlush(favorite);

        verify(favoriteEventRepository)
                .saveAndFlush(any(FavoriteEvent.class));
    }

    @Test
    void 즐겨찾기_중복확인() {

        when(
                favoriteEventRepository
                        .existsByUsernameAndEventId(
                                "tester",
                                "100"
                        )
        ).thenReturn(true);

        boolean result =
                favoriteEventRepository
                        .existsByUsernameAndEventId(
                                "tester",
                                "100"
                        );

        assertTrue(result);
    }

    @Test
    void 즐겨찾기_삭제성공() {

        favoriteEventRepository
                .deleteByUsernameAndEventId(
                        "tester",
                        "100"
                );

        verify(favoriteEventRepository)
                .deleteByUsernameAndEventId(
                        "tester",
                        "100"
                );
    }
}