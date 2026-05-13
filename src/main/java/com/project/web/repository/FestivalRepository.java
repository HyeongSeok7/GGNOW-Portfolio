package com.project.web.repository;

import com.project.web.model.FestivalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

//내부 행사 식별자 관리를 위한 Repository
//identityKey 기준 조회와 중복 저장 방지를 위한 INSERT IGNORE를 제공
public interface FestivalRepository extends JpaRepository<FestivalEntity, Long> {

	Optional<FestivalEntity> findByIdentityKey(String identityKey);

	@Modifying
	@Query(value = """
			INSERT IGNORE INTO festival (identity_key, normalized_title, title)
			VALUES (:identityKey, :normalizedTitle, :title)
			""", nativeQuery = true)

	// identityKey unique 제약조건을 이용해 이미 존재하는 행사는 무시하고,
	// 새 행사만 festival 테이블에 저장
	void insertIgnore(@Param("identityKey") String identityKey, @Param("normalizedTitle") String normalizedTitle,
			@Param("title") String title);
}