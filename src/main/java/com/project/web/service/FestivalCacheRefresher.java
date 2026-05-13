package com.project.web.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//캐시된 행사 데이터를 주기적으로 갱신하는 컴포넌트
//외부 API 데이터 변경 사항이 서비스에 반영되도록 일정 시간마다 캐시를 비운 뒤 다시 조회
@Component
public class FestivalCacheRefresher {
	private final FestivalService festivalService;

	public FestivalCacheRefresher(FestivalService festivalService) {
		this.festivalService = festivalService;
	}

	// ●30분마다● festivals 캐시를 제거하고 FestivalService를 호출해 최신 데이터를 다시 캐싱
	@Scheduled(fixedDelay = 30 * 60 * 1000)
	@CacheEvict(value = "festivals", allEntries = true, beforeInvocation = true)
	public void refresh() {
		festivalService.getFestivalData();
	}
}
