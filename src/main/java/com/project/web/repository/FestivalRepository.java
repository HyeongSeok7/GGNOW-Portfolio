package com.project.web.repository;

import com.project.web.model.FestivalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

//내부 행사 식별자 관리를 위한 Repository
//identityKey 기준 조회와 중복 저장 방지를 위한 INSERT IGNORE를 제공
public interface FestivalRepository extends JpaRepository<FestivalEntity, Long> {

	 Optional<FestivalEntity> findByIdentityKey(String identityKey);
	 Optional<FestivalEntity> findByIdAndActiveTrue(Long id);
	 Optional<FestivalEntity> findFirstByNormalizedTitleAndActiveTrueOrderByIdDesc(String nomalizedTitle);
	 
	    List<FestivalEntity> findAllByOrderByIdDesc();

	    List<FestivalEntity> findAllByOrderByBeginDeDesc();

	    List<FestivalEntity> findByCategoryNmOrderByBeginDeDesc(String categoryNm);

	    List<FestivalEntity> findByIdInAndActiveTrueOrderByBeginDeDesc(Collection<Long> ids);
	    
		// 마이페이지에서는 종료·비활성 행사도 사용자 기록으로 조회한다.
		// 행사 시작일이 같으면 ID 내림차순으로 정렬한다.
		List<FestivalEntity> findByIdInOrderByBeginDeDescIdDesc(Collection<Long> ids);
		
		
		List<FestivalEntity> findByIdInAndCategoryNmOrderByBeginDeDescIdDesc(
		        Collection<Long> ids,
		        String categoryNm
		);
		
		
	    @Modifying
	    @Query(value = """
	            INSERT IGNORE INTO festival (identity_key, normalized_title, title)
	            VALUES (:identityKey, :normalizedTitle, :title)
	            """, nativeQuery = true)
	    void insertIgnore(
	            @Param("identityKey") String identityKey,
	            @Param("normalizedTitle") String normalizedTitle,
	            @Param("title") String title);

	    @Modifying
	    @Query("""
	    UPDATE FestivalEntity f
	    SET f.active = false
	    WHERE f.identityKey NOT IN :keys
	    """)
	    void deactivateNotIn(@Param("keys") Collection<String> keys);

	    List<FestivalEntity> findByActiveTrueOrderByBeginDeDesc();

	    
	    List<FestivalEntity> findByCategoryNmAndActiveTrueOrderByBeginDeDesc(
	            String categoryNm);
	    
		 // 진행 중·예정 행사 검색
		 // 종료일이 지난 행사는 active=true로 남아 있어도 제외한다.
		 @Query("""
		         SELECT f
		         FROM FestivalEntity f
		         WHERE f.active = true
		           AND (
		               f.endDe IS NULL
		               OR TRIM(f.endDe) = ''
		               OR f.endDe >= :today
		           )
		           AND (
		               f.normalizedTitle LIKE CONCAT('%', :keyword, '%')
		               OR LOWER(f.hostInstNm) LIKE LOWER(CONCAT('%', :keyword, '%'))
		               OR LOWER(f.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
		           )
		         ORDER BY f.beginDe DESC, f.id DESC
		         """)
		 List<FestivalEntity> searchActiveFestivals(
		         @Param("keyword") String keyword,
		         @Param("today") String today
		 );
	
		 // 종료 행사 검색
		 // active=false 전체가 아니라, 실제 종료일이 지난 행사만 조회한다.
		 @Query("""
		         SELECT f
		         FROM FestivalEntity f
		         WHERE f.endDe IS NOT NULL
		           AND TRIM(f.endDe) <> ''
		           AND f.endDe < :today
		           AND (
		               f.normalizedTitle LIKE CONCAT('%', :keyword, '%')
		               OR LOWER(f.hostInstNm) LIKE LOWER(CONCAT('%', :keyword, '%'))
		               OR LOWER(f.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
		           )
		         ORDER BY f.endDe DESC, f.id DESC
		         """)
		 List<FestivalEntity> searchEndedFestivals(
		         @Param("keyword") String keyword,
		         @Param("today") String today
		 );
	    @Modifying
	    @Query("""
	            UPDATE FestivalEntity f
	            SET f.active = false
	            WHERE f.active = true
	              AND f.endDe IS NOT NULL
	              AND TRIM(f.endDe) <> ''
	              AND f.endDe < :today
	            """)
	    int deactivateEndedBefore(@Param("today") String today);
	    
	    Page<FestivalEntity> findByCategoryNmAndActiveTrue(
	            String categoryNm,
	            Pageable pageable
	    );
	    
	    
	 // 종료일 기준 오늘보다 이전인 행사만 조회한다.
	 // active=false 전체를 조회하지 않으므로 취소된 미래 행사는 포함되지 않는다.
	 @Query("""
	         SELECT f
	         FROM FestivalEntity f
	         WHERE f.endDe IS NOT NULL
	           AND TRIM(f.endDe) <> ''
	           AND f.endDe < :today
	         ORDER BY f.endDe DESC, f.id DESC
	         """)
	 List<FestivalEntity> findEndedFestivals(
	         @Param("today") String today);

	 @Query(
		        value = """
		                SELECT f
		                FROM FestivalEntity f
		                WHERE f.endDe IS NOT NULL
		                  AND TRIM(f.endDe) <> ''
		                  AND f.endDe < :today
		                  AND (
		                      :category = 'all'
		                      OR f.categoryNm = :category
		                  )
		                """,
		        countQuery = """
		                SELECT COUNT(f)
		                FROM FestivalEntity f
		                WHERE f.endDe IS NOT NULL
		                  AND TRIM(f.endDe) <> ''
		                  AND f.endDe < :today
		                  AND (
		                      :category = 'all'
		                      OR f.categoryNm = :category
		                  )
		                """
		)
		Page<FestivalEntity> findEndedFestivals(
		        @Param("today") String today,
		        @Param("category") String category,
		        Pageable pageable
		);
	 // 종료 행사 보관함에서 상세 페이지를 열 때도 종료일을 다시 확인한다.
	 @Query("""
	         SELECT f
	         FROM FestivalEntity f
	         WHERE f.id = :festivalId
	           AND f.endDe IS NOT NULL
	           AND TRIM(f.endDe) <> ''
	           AND f.endDe < :today
	         """)
	 Optional<FestivalEntity> findEndedFestivalById(
	         @Param("festivalId") Long festivalId,
	         @Param("today") String today);
}