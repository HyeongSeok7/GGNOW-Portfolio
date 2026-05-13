package com.project.web.service;

import com.project.web.model.FestivalEntity;
import com.project.web.repository.FestivalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//외부 API 행사 데이터를 서비스 내부 festivalId와 연결하는 서비스
//identityKey 기준으로 이미 존재하는 행사는 재사용하고, 없으면 새로 저장
@Service
public class FestivalIdentityService {

	private final FestivalRepository festivalRepository;

	public FestivalIdentityService(FestivalRepository festivalRepository) {
		this.festivalRepository = festivalRepository;
	}

	// identityKey가 이미 존재하면 기존 festivalId를 반환하고,
	// 없으면 insertIgnore로 새 행사 식별 정보를 저장한 뒤 festivalId를 조회
	@Transactional
	public Long getOrCreateFestivalId(String identityKey, String normalizedTitle, String originalTitle) {
		// 중복 요청이 동시에 들어와도 unique key와 INSERT IGNORE로 중복 저장을 방지
		festivalRepository.insertIgnore(identityKey, normalizedTitle, originalTitle);

		return festivalRepository.findByIdentityKey(identityKey).map(FestivalEntity::getId)
				.orElseThrow(() -> new IllegalStateException("Festival 저장 또는 조회 실패: " + identityKey));
	}
}