package com.project.web.service;

import com.project.web.model.FestivalResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FestivalService의 행사 식별 로직을 검증하는 단위 테스트입니다.
 *
 * 이 프로젝트에서는 공공데이터 API에서 받아온 행사 데이터를 DB에 저장할 때,
 * 단순히 제목만으로 같은 행사인지 판단하지 않고
 * 제목, 시작일, 종료일, 시간, 참가비, 주소 등을 조합해 identityKey를 생성합니다.
 *
 * 이 테스트는 그 핵심 로직이 의도대로 동작하는지 확인합니다.
 */
class FestivalServiceTest {

    /**
     * @SpringBootTest를 사용하지 않고 FestivalService 객체만 직접 생성합니다.
     *
     * 이유:
     * - 이 테스트는 DB, 서버 실행, Spring 전체 환경이 필요하지 않습니다.
     * - normalize(), createFestivalIdentityKey() 같은 순수 로직만 검증하면 됩니다.
     * - 그래서 더 빠르고 가볍게 실행할 수 있는 단위 테스트 형태로 작성했습니다.
     */
    private final FestivalService festivalService = new FestivalService(new RestTemplate());

    @Test
    @DisplayName("normalize는 공백과 대부분의 특수문자를 제거하고 영문을 소문자로 변환한다")
    void normalize_removesSpacesAndSpecialCharacters() {
        // 테스트에 사용할 문자열을 준비합니다.
        // 공백, 특수문자, 대문자 영문, 한글, 숫자가 섞인 상태입니다.
        String input = "  Gyeonggi!! 봄 축제 @2026  ";

        // normalize() 메서드를 실행합니다.
        // 이 메서드는 행사명을 비교하기 쉽도록 불필요한 문자를 정리하는 역할을 합니다.
        String result = festivalService.normalize(input);

        // 실행 결과가 기대한 값과 같은지 확인합니다.
        // 공백과 특수문자는 제거되고, 영문 대문자는 소문자로 바뀌어야 합니다.
        assertThat(result).isEqualTo("gyeonggi봄축제2026");
    }

    @Test
    @DisplayName("normalize는 null이 들어오면 빈 문자열을 반환한다")
    void normalize_returnsEmptyStringWhenInputIsNull() {
        // normalize() 메서드에 null 값을 넣어 실행합니다.
        // null이 들어와도 오류가 발생하지 않아야 합니다.
        String result = festivalService.normalize(null);

        // null 값은 비교 가능한 빈 문자열로 변환되어야 합니다.
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("같은 행사 정보는 같은 identityKey를 생성한다")
    void createFestivalIdentityKey_returnsSameKeyForSameFestivalInfo() {
        // 모든 필드 값이 같은 두 개의 행사 데이터를 준비합니다.
        // 실제 공공데이터 API에서 같은 행사가 다시 들어오는 상황을 가정한 테스트입니다.
        FestivalResponse.Row festival1 = festival(
                "경기 봄 축제",
                "2026-04-01",
                "2026-04-10",
                "10:00 ~ 18:00",
                "무료",
                "경기도 수원시 팔달구"
        );

        FestivalResponse.Row festival2 = festival(
                "경기 봄 축제",
                "2026-04-01",
                "2026-04-10",
                "10:00 ~ 18:00",
                "무료",
                "경기도 수원시 팔달구"
        );

        // 두 행사 각각에 대해 identityKey를 생성합니다.
        String key1 = festivalService.createFestivalIdentityKey(festival1);
        String key2 = festivalService.createFestivalIdentityKey(festival2);

        // 같은 행사 정보라면 같은 identityKey가 생성되어야 합니다.
        assertThat(key1).isEqualTo(key2);

        // SHA-256 해시를 사용하면 64자리 문자열이 생성됩니다.
        // 이 검증은 identityKey가 예상한 해시 형식으로 생성되는지도 함께 확인합니다.
        assertThat(key1).hasSize(64);
    }

    @Test
    @DisplayName("제목이 같아도 날짜가 다르면 다른 identityKey를 생성한다")
    void createFestivalIdentityKey_returnsDifferentKeyWhenDateIsDifferent() {
        // 제목은 같지만 시작일과 종료일이 다른 두 행사를 준비합니다.
        // 같은 이름의 축제가 다른 기간에 열리는 상황을 가정합니다.
        FestivalResponse.Row springFestival = festival(
                "경기 봄 축제",
                "2026-04-01",
                "2026-04-10",
                "10:00 ~ 18:00",
                "무료",
                "경기도 수원시 팔달구"
        );

        FestivalResponse.Row summerFestival = festival(
                "경기 봄 축제",
                "2026-08-01",
                "2026-08-10",
                "10:00 ~ 18:00",
                "무료",
                "경기도 수원시 팔달구"
        );

        // 두 행사 각각에 대해 identityKey를 생성합니다.
        String springKey = festivalService.createFestivalIdentityKey(springFestival);
        String summerKey = festivalService.createFestivalIdentityKey(summerFestival);

        // 제목이 같더라도 날짜가 다르면 서로 다른 행사로 판단해야 합니다.
        assertThat(springKey).isNotEqualTo(summerKey);
    }

    @Test
    @DisplayName("제목이 같아도 장소가 다르면 다른 identityKey를 생성한다")
    void createFestivalIdentityKey_returnsDifferentKeyWhenAddressIsDifferent() {
        // 제목과 날짜는 같지만 장소가 다른 두 행사를 준비합니다.
        // 같은 이름의 행사가 서로 다른 지역에서 열리는 상황을 가정합니다.
        FestivalResponse.Row suwonFestival = festival(
                "경기 봄 축제",
                "2026-04-01",
                "2026-04-10",
                "10:00 ~ 18:00",
                "무료",
                "경기도 수원시 팔달구"
        );

        FestivalResponse.Row yonginFestival = festival(
                "경기 봄 축제",
                "2026-04-01",
                "2026-04-10",
                "10:00 ~ 18:00",
                "무료",
                "경기도 용인시 기흥구"
        );

        // 두 행사 각각에 대해 identityKey를 생성합니다.
        String suwonKey = festivalService.createFestivalIdentityKey(suwonFestival);
        String yonginKey = festivalService.createFestivalIdentityKey(yonginFestival);

        // 장소가 다르면 서로 다른 행사로 판단해야 합니다.
        assertThat(suwonKey).isNotEqualTo(yonginKey);
    }

    /**
     * 테스트용 FestivalResponse.Row 객체를 만드는 보조 메서드입니다.
     *
     * 테스트마다 setter를 반복해서 작성하면 코드가 길어지고 읽기 어려워지므로,
     * 행사 데이터를 간단히 만들 수 있도록 별도 메서드로 분리했습니다.
     */
    private FestivalResponse.Row festival(
            String title,
            String beginDe,
            String endDe,
            String eventTmInfo,
            String partcptExpnInfo,
            String addr
    ) {
        FestivalResponse.Row row = new FestivalResponse.Row();

        row.setTitle(title);
        row.setBeginDe(beginDe);
        row.setEndDe(endDe);
        row.setEventTmInfo(eventTmInfo);
        row.setPartcptExpnInfo(partcptExpnInfo);
        row.setAddr(addr);

        return row;
    }
}