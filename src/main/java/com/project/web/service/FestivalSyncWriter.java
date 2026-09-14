package com.project.web.service;

import com.project.web.model.FestivalEntity;
import com.project.web.model.FestivalResponse;
import com.project.web.model.SyncStatus;
import com.project.web.repository.FestivalRepository;
import com.project.web.repository.SyncStatusRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API 호출이 끝난 뒤 DB 변경만 하나의 트랜잭션으로 처리한다.
 */
@Service
public class FestivalSyncWriter {

    private static final ZoneId KOREA =
            ZoneId.of("Asia/Seoul");

    private final FestivalService festivalService;
    private final FestivalRepository festivalRepository;
    private final SyncStatusRepository syncStatusRepository;

    public FestivalSyncWriter(
            FestivalService festivalService,
            FestivalRepository festivalRepository,
            SyncStatusRepository syncStatusRepository
    ) {
        this.festivalService = festivalService;
        this.festivalRepository = festivalRepository;
        this.syncStatusRepository = syncStatusRepository;
    }

    // API 장애와 무관하게 종료일이 지난 행사를 숨긴다.
    @Transactional
    public void hideEndedFestivals() {
        festivalRepository.deactivateEndedBefore(
                LocalDate.now(KOREA).toString()
        );
    }

    @Transactional
    public int applySnapshot(FestivalResponse snapshot) {
        if (snapshot == null
                || snapshot.getInfo() != 0
                || snapshot.getRow() == null
                || snapshot.getRow().isEmpty()) {
            throw new IllegalArgumentException(
                    "정상적으로 수집한 비어 있지 않은 결과가 필요합니다."
            );
        }

        /*
         * 기존 identityKey가 예전 방식이어도 원본 URL로 같은 DB 행을 찾는다.
         * 키만 갱신하고 id를 유지하므로 리뷰/즐겨찾기의 참조 값은 바뀌지 않는다.
         */
        Map<String, FestivalEntity> existingBySource =
                new HashMap<>();

        for (FestivalEntity entity : festivalRepository.findAll()) {
            String key;

            try {
                key = festivalService
                        .createFestivalIdentityKeyFromUrl(
                                entity.getHomepage()
                        );

            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "기존 행사의 원본 URL 확인 필요. festivalId="
                                + entity.getId(),
                        e
                );
            }

            FestivalEntity previous =
                    existingBySource.putIfAbsent(key, entity);

            if (previous != null) {
                throw new IllegalStateException(
                        "동일 원본에 연결된 DB 행사가 여러 개입니다. festivalIds="
                                + previous.getId()
                                + ","
                                + entity.getId()
                );
            }
        }

        LocalDate today = LocalDate.now(KOREA);
        Set<String> currentKeys = new HashSet<>();

        for (FestivalResponse.Row row : snapshot.getRow()) {
            String key =
                    festivalService.createFestivalIdentityKey(row);

            if (!currentKeys.add(key)) {
                throw new IllegalStateException(
                        "수집 결과에 중복 원본 ID가 있습니다."
                );
            }

            if (row.getTitle() == null || row.getTitle().isBlank()) {
                throw new IllegalStateException(
                        "제목이 없는 행사입니다."
                );
            }

            FestivalEntity entity = existingBySource.get(key);

            if (entity == null) {
                entity = new FestivalEntity();
            }

            String beginDe = normalizeDate(row.getBeginDe());
            String endDe = normalizeDate(row.getEndDe());

            entity.setIdentityKey(key);
            entity.setNormalizedTitle(
                    festivalService.normalize(row.getTitle())
            );

            entity.setTitle(row.getTitle());
            entity.setImageUrl(row.getImageUrl());

            // GGC 원본 상세 URL. 기관 홈페이지와 다르다.
            entity.setHomepage(row.getUrl());
            entity.setHmpgUrl(row.getHmpgUrl());

            entity.setAddress(row.getAddr());
            entity.setHostInstNm(row.getHostInstNm());
            entity.setBeginDe(beginDe);
            entity.setEndDe(endDe);
            entity.setEventTmInfo(row.getEventTmInfo());
            entity.setPartcptExpnInfo(row.getPartcptExpnInfo());
            entity.setCategoryNm(row.getCategoryNm());
            entity.setTelnoInfo(row.getTelnoInfo());

            entity.setActive(
                    endDe == null
                            || !LocalDate.parse(endDe).isBefore(today)
            );

            festivalRepository.save(entity);
        }

        // 새 키가 DB에 반영된 다음 누락된 행사를 비활성화한다.
        festivalRepository.flush();
        festivalRepository.deactivateNotIn(currentKeys);

        SyncStatus status = syncStatusRepository
                .findById(1L)
                .orElse(new SyncStatus());

        status.setId(1L);
        status.setLastSuccessTime(
                LocalDateTime.now(KOREA)
        );

        syncStatusRepository.save(status);

        return currentKeys.size();
    }

    private String normalizeDate(String value) {
        return value == null || value.isBlank()
                ? null
                : LocalDate.parse(value.trim()).toString();
    }
}