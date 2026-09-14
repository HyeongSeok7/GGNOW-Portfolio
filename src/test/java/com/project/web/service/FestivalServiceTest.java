package com.project.web.service;

import com.project.web.model.FestivalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FestivalServiceTest {

    private RestTemplate rest;
    private FestivalService service;

    @BeforeEach
    void setUp() {
        rest = mock(RestTemplate.class);
        service = new FestivalService(rest, null);

        ReflectionTestUtils.setField(
                service,
                "apiBaseUrl",
                "https://example.test/events"
        );

        ReflectionTestUtils.setField(service, "pageSize", 200);
        ReflectionTestUtils.setField(service, "maxPages", 10);
    }

    @Test
    void normalize_removesSpaces() {
        assertEquals(
                "gyeonggi봄축제2026",
                service.normalize(" Gyeonggi!! 봄 축제 @2026 ")
        );
    }

    @Test
    void normalize_acceptsNull() {
        assertEquals("", service.normalize(null));
    }

    @Test
    void sameSource_keepsKeyAfterContentChange() {
        var row = row("event-a");
        String before = service.createFestivalIdentityKey(row);

        row.setTitle("수정된 제목");
        row.setPartcptExpnInfo("10000원");
        row.setEndDe("2099-12-30");
        row.setAddr("다른 장소");

        assertEquals(
                before,
                service.createFestivalIdentityKey(row)
        );

        assertEquals(64, before.length());
    }

    @Test
    void differentSource_hasDifferentKey() {
        assertNotEquals(
                service.createFestivalIdentityKey(row("event-a")),
                service.createFestivalIdentityKey(row("event-b"))
        );
    }

    @Test
    void invalidSource_isRejected() {
        var row = row("event-a");
        row.setUrl("https://example.com/not-a-ggc-event");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createFestivalIdentityKey(row)
        );
    }

    @Test
    void shortPage_doesNotStopCollection() throws Exception {
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(
                        page(row("event-a")),
                        page(row("event-b")),
                        page()
                );

        assertEquals(
                2,
                service.getFestivalData().getRow().size()
        );

        verify(rest).getForObject(
                "https://example.test/events?page=2&perpage=200",
                String.class
        );
    }

    @Test
    void failureOnNextPage_doesNotReturnPartialResult()
            throws Exception {

        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(page(row("event-a")))
                .thenThrow(new IllegalStateException("API failure"));

        assertThrows(
                IllegalStateException.class,
                service::getFestivalData
        );

        verify(rest, times(2))
                .getForObject(anyString(), eq(String.class));
    }

    @Test
    void repeatedPage_isRejected() throws Exception {
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(
                        page(row("event-a")),
                        page(row("event-a"))
                );

        assertThrows(
                IllegalStateException.class,
                service::getFestivalData
        );

        verify(rest, times(2))
                .getForObject(anyString(), eq(String.class));
    }

    @Test
    void emptyFirstPage_isRejected() throws Exception {
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(page());

        assertThrows(
                IllegalStateException.class,
                service::getFestivalData
        );
    }

    @Test
    void missingInfo_isRejected() {
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"DATA\":[]}");

        assertThrows(
                IllegalStateException.class,
                service::getFestivalData
        );
    }

    @Test
    void info200_canEndCollection() throws Exception {
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(
                        page(row("event-a")),
                        "{\"INFO\":200}"
                );

        assertEquals(
                1,
                service.getFestivalData().getRow().size()
        );
    }

    private FestivalResponse.Row row(String id) {
        var row = new FestivalResponse.Row();

        row.setUrl(
                "https://ggc.ggcf.kr/cultureEvents/view/" + id
        );

        row.setTitle("테스트 행사");
        row.setEndDe("2099-12-31");

        return row;
    }

    private String page(FestivalResponse.Row... rows)
            throws Exception {

        return new ObjectMapper().writeValueAsString(
                Map.of(
                        "INFO", 0,
                        "DATA", List.of(rows)
                )
        );
    }
}