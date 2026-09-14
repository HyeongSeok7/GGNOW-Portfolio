package com.project.web.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FestivalSyncScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(FestivalSyncScheduler.class);

    private final FestivalService festivalService;
    private final FestivalSyncWriter festivalSyncWriter;

    // 같은 서버 프로세스 안에서 동기화가 겹치는 것을 방지한다.
    private final AtomicBoolean running =
            new AtomicBoolean(false);

    public FestivalSyncScheduler(
            FestivalService festivalService,
            FestivalSyncWriter festivalSyncWriter
    ) {
        this.festivalService = festivalService;
        this.festivalSyncWriter = festivalSyncWriter;
    }

    // 시작 10초 후 최초 실행
    @Scheduled(
            initialDelay = 10_000,
            fixedDelay = Long.MAX_VALUE
    )

    // 이후 한국 시간 기준 매일 자정 실행
    @Scheduled(
            cron = "0 0 0 * * *",
            zone = "Asia/Seoul"
    )
    public void syncFestivalData() {
        if (!running.compareAndSet(false, true)) {
            log.warn(
                    "이미 동기화 중이므로 이번 실행을 건너뜁니다."
            );
            return;
        }

        try {
            // API 호출 실패와 관계없이 종료일 경과 처리는 수행한다.
            festivalSyncWriter.hideEndedFestivals();

            // DB 저장 트랜잭션 밖에서 전체 페이지를 수집한다.
            var snapshot = festivalService.getFestivalData();

            // 전체 수집에 성공한 경우에만 DB에 반영한다.
            int count =
                    festivalSyncWriter.applySnapshot(snapshot);

            log.info("행사 동기화 성공: {}건", count);

        } catch (Exception e) {
            log.error(
                    "행사 동기화 실패. API 결과 반영은 중단하고 기존 데이터를 유지합니다."
                            + " 종료일 경과 처리는 별도입니다.",
                    e
            );

        } finally {
            running.set(false);
        }
    }
}