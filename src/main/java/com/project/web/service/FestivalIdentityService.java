package com.project.web.service;

import com.project.web.model.FestivalEntity;
import com.project.web.repository.FestivalRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
        return festivalRepository.findByIdentityKey(identityKey)
                .map(FestivalEntity::getId)
                .orElseGet(() -> saveOrFindExisting(identityKey, normalizedTitle, originalTitle));
    }

    private Long saveOrFindExisting(String identityKey, String normalizedTitle, String originalTitle) {
        try {
            FestivalEntity festival = new FestivalEntity();
            festival.setIdentityKey(identityKey);
            festival.setNormalizedTitle(normalizedTitle);
            festival.setTitle(originalTitle);

            return festivalRepository.saveAndFlush(festival).getId();

        } catch (DataIntegrityViolationException e) {
            return festivalRepository.findByIdentityKey(identityKey)
                    .map(FestivalEntity::getId)
                    .orElseThrow(() -> e);
        }
    }
}