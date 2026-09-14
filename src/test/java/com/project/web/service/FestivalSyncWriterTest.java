package com.project.web.service;

import com.project.web.model.FestivalEntity;
import com.project.web.model.FestivalResponse;
import com.project.web.repository.FestivalRepository;
import com.project.web.repository.SyncStatusRepository;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FestivalSyncWriterTest {

    private final FestivalRepository repository =
            mock(FestivalRepository.class);

    private final SyncStatusRepository statuses =
            mock(SyncStatusRepository.class);

    private final FestivalService service =
            new FestivalService(new RestTemplate(), repository);

    private final FestivalSyncWriter writer =
            new FestivalSyncWriter(service, repository, statuses);

    private final String source =
            "https://ggc.ggcf.kr/cultureEvents/view/event-a";

    @Test
    void migration_keepsExistingDatabaseId() {
        FestivalEntity old = existing(7L);

        when(repository.findAll()).thenReturn(List.of(old));

        var snapshot = snapshot();

        String key = service.createFestivalIdentityKey(
                snapshot.getRow().get(0)
        );

        assertEquals(1, writer.applySnapshot(snapshot));
        assertEquals(7L, old.getId());
        assertEquals(key, old.getIdentityKey());
        assertEquals("10000원", old.getPartcptExpnInfo());

        verify(repository).save(old);

        var order = inOrder(repository, statuses);

        order.verify(repository).flush();
        order.verify(repository).deactivateNotIn(Set.of(key));
        order.verify(statuses).save(any());
    }

    @Test
    void duplicateLegacySource_stopsBeforeWriting() {
        when(repository.findAll()).thenReturn(
                List.of(existing(7L), existing(8L))
        );

        assertThrows(
                IllegalStateException.class,
                () -> writer.applySnapshot(snapshot())
        );

        verify(repository, never()).save(any());
        verify(repository, never()).deactivateNotIn(any());
        verifyNoInteractions(statuses);
    }

    @Test
    void missingLegacySource_stopsBeforeWriting() {
        FestivalEntity old = existing(7L);
        old.setHomepage(null);

        when(repository.findAll()).thenReturn(List.of(old));

        assertThrows(
                IllegalStateException.class,
                () -> writer.applySnapshot(snapshot())
        );

        verify(repository, never()).save(any());
        verify(repository, never()).deactivateNotIn(any());
    }

    @Test
    void apiFailure_doesNotInvokeSnapshotWrite() {
        FestivalService api = mock(FestivalService.class);
        FestivalSyncWriter db = mock(FestivalSyncWriter.class);

        when(api.getFestivalData())
                .thenThrow(new IllegalStateException("API failure"));

        new FestivalSyncScheduler(api, db)
                .syncFestivalData();

        verify(db).hideEndedFestivals();
        verify(db, never()).applySnapshot(any());
    }

    private FestivalEntity existing(Long id) {
        FestivalEntity entity = new FestivalEntity();

        entity.setId(id);
        entity.setHomepage(source);
        entity.setIdentityKey("old-content-based-key-" + id);

        return entity;
    }

    private FestivalResponse snapshot() {
        var row = new FestivalResponse.Row();

        row.setUrl(source);
        row.setTitle("갱신된 행사");
        row.setPartcptExpnInfo("10000원");
        row.setEndDe("2099-12-31");

        var response = new FestivalResponse();

        response.setInfo(0);
        response.setRow(List.of(row));

        return response;
    }
}