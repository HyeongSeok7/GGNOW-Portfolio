package com.project.web.service;

import com.project.web.model.FestivalEntity;
import com.project.web.repository.FestivalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FestivalIdentityService {

    private final FestivalRepository festivalRepository;

    public FestivalIdentityService(FestivalRepository festivalRepository) {
        this.festivalRepository = festivalRepository;
    }

    @Transactional
    public Long getOrCreateFestivalId(String identityKey, String normalizedTitle, String originalTitle) {
        festivalRepository.insertIgnore(identityKey, normalizedTitle, originalTitle);

        return festivalRepository.findByIdentityKey(identityKey)
                .map(FestivalEntity::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "Festival 저장 또는 조회 실패: " + identityKey
                ));
    }
}