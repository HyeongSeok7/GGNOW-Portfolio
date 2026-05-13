package com.project.web.repository;

import com.project.web.model.FavoriteEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//즐겨찾기 Entity에 대한 DB 접근 레파지토리
//사용자별 즐겨찾기 조회, 삭제, 중복 확인에 사용
@Repository
public interface FavoriteEventRepository extends JpaRepository<FavoriteEvent, Long> {
	List<FavoriteEvent> findAllByUsername(String username);

	@Transactional
	@Modifying
	void deleteByUsernameAndEventId(String username, String eventId);

	boolean existsByUsernameAndEventId(String username, String eventId);
}