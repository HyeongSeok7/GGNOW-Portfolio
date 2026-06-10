package com.project.web.service;

import com.project.web.model.FestivalEntity;
import com.project.web.model.FestivalResponse;
import com.project.web.model.SyncStatus;
import com.project.web.repository.FestivalRepository;
import com.project.web.repository.SyncStatusRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FestivalSyncScheduler {

	
private final FestivalService festivalService;
private final FestivalRepository festivalRepository;
private final SyncStatusRepository syncStatusRepository;
private final DataSource dataSource;
public FestivalSyncScheduler(
        FestivalService festivalService,
        FestivalRepository festivalRepository,
        SyncStatusRepository syncStatusRepository,
        DataSource dataSource) {

    this.festivalService = festivalService;
    this.festivalRepository = festivalRepository;
    this.syncStatusRepository = syncStatusRepository;
    this.dataSource = dataSource;
}
@PersistenceContext
private EntityManager em;

@Scheduled(cron = "0 0 * * * *")
@Transactional
public void syncFestivalData() {

	System.out.println(
		    "DB = " +
		    em.createNativeQuery("SELECT DATABASE()")
		      .getSingleResult()
		);
	
	try {
        System.out.println(
            "DB URL = "
            + dataSource.getConnection()
                        .getMetaData()
                        .getURL()
        );
    } catch (Exception e) {
        e.printStackTrace();
    }
	
    FestivalResponse response =
            festivalService.getFestivalData();

    if (response == null ||
            response.getRow() == null) {
        return;
    }

    for (FestivalResponse.Row row :
            response.getRow()) {

        String identityKey =
                festivalService
                        .createFestivalIdentityKey(row);

        FestivalEntity entity =
                festivalRepository
                        .findByIdentityKey(identityKey)
                        .orElse(new FestivalEntity());

        entity.setIdentityKey(identityKey);
        entity.setNormalizedTitle(
                festivalService.normalize(
                        row.getTitle()));

        entity.setTitle(row.getTitle());

        entity.setImageUrl(row.getImageUrl());
        entity.setHomepage(row.getHmpgUrl());
        entity.setAddress(row.getAddr());
        entity.setHostInstNm(row.getHostInstNm());

        entity.setBeginDe(row.getBeginDe());
        entity.setEndDe(row.getEndDe());

        entity.setEventTmInfo(
                row.getEventTmInfo());

        entity.setPartcptExpnInfo(
                row.getPartcptExpnInfo());

        entity.setCategoryNm(row.getCategoryNm());

        entity.setEventTmInfo(
                row.getEventTmInfo());

        entity.setPartcptExpnInfo(
                row.getPartcptExpnInfo());

        entity.setTelnoInfo(
                row.getTelnoInfo());

        entity.setHmpgUrl(
                row.getHmpgUrl());
        
        festivalRepository.saveAndFlush(entity);
    }
    
    SyncStatus status =
            syncStatusRepository
                    .findById(1L)
                    .orElse(new SyncStatus());

    status.setId(1L);

    status.setLastSuccessTime(
            java.time.LocalDateTime.now());

    syncStatusRepository.save(status);
}

}
