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
    public Long getOrCreateFestivalId(String normalizedTitle, String originalTitle) {
        return festivalRepository.findByNormalizedTitle(normalizedTitle)
                .map(FestivalEntity::getId)
                .orElseGet(() -> {
                    FestivalEntity f = new FestivalEntity();
                    f.setNormalizedTitle(normalizedTitle);
                    f.setTitle(originalTitle);
                    return festivalRepository.save(f).getId();
                });
    }
}