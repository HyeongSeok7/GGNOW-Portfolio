package com.project.web.service;

import com.project.web.model.FestivalEntity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public final class FestivalWriteGuard {

    private static final ZoneId KOREA_ZONE =
            ZoneId.of("Asia/Seoul");

    private FestivalWriteGuard() {
    }

    public static void check(FestivalEntity festival) {

        // API에서 사라졌거나 이미 비활성화된 행사
        if (!festival.isActive()) {
            throw new WriteNotAllowedException(
                    "종료되었거나 비활성화된 행사에서는 사용할 수 없습니다."
            );
        }

        String endDe = festival.getEndDe();

        // 종료일이 제공되지 않은 행사는 기존 동작대로 허용
        if (endDe == null || endDe.isBlank()) {
            return;
        }

        LocalDate endDate;

        try {
            endDate = LocalDate.parse(endDe.trim());
        } catch (DateTimeParseException e) {
            throw new WriteNotAllowedException(
                    "행사 종료일을 확인할 수 없어 요청을 처리할 수 없습니다."
            );
        }

        LocalDate today = LocalDate.now(KOREA_ZONE);

        // 종료일 당일까지 허용하고, 다음 날부터 차단
        if (endDate.isBefore(today)) {
            throw new WriteNotAllowedException(
                    "종료된 행사에서는 사용할 수 없습니다."
            );
        }
    }

    // 행사 상태 때문에 차단된 요청을 다른 오류와 구분
    public static class WriteNotAllowedException
            extends RuntimeException {

        public WriteNotAllowedException(String message) {
            super(message);
        }
    }
}