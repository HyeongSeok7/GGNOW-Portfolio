package com.project.web.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FestivalCacheRefresher {
    private final FestivalService festivalService;

    public FestivalCacheRefresher(FestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    @CacheEvict(value = "festivals", allEntries = true)
    public void refresh() {
        // 캐시 비운 뒤, 한 번 호출해서 캐시 재적재
        festivalService.getFestivalData();
    }
}
