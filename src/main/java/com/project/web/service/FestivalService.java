package com.project.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.project.web.model.FestivalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FestivalService {

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    @Value("${api.page-index}")
    private int pageIndex;

    @Value("${api.page-size}")
    private int pageSize;


    private final RestTemplate restTemplate;

    public FestivalService(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "festivals", key = "'all'")
    public FestivalResponse getFestivalData() {
        String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                .queryParam("KEY", apiKey)
                .queryParam("pIndex", pageIndex)
                .queryParam("pSize", pageSize)
                .build(true)
                .toUriString();

        String response = restTemplate.getForObject(url, String.class);

        XmlMapper xmlMapper = new XmlMapper();
        try {
            FestivalResponse festivalResponse = xmlMapper.readValue(response, FestivalResponse.class);

            if (festivalResponse != null && festivalResponse.getAllFestivals() != null) {
                // 제목을 기준으로 중복 제거된 리스트 생성
                List<FestivalResponse.Row> distinctFestivals = festivalResponse.getAllFestivals().stream()
                        .collect(Collectors.toMap(
                                this::fingerprint,          //  지문키로 중복 판단
                                Function.identity(),
                                this::chooseBetter,         //  중복이면 더 “좋은” 데이터 선택
                                java.util.LinkedHashMap::new //  원래 순서 유지
                        ))
                        .values()
                        .stream()
                        .collect(Collectors.toList());

                festivalResponse.setRow(distinctFestivals);
            }

            return festivalResponse;
        } catch (JsonProcessingException e) {
            FestivalResponse empty = new FestivalResponse();
            empty.setRow(List.of());
            return empty;
        }
    }

    // 검색 메서드: 제목, 기관명 을 기준으로 검색
    public List<FestivalResponse.Row> searchFestivals(String keyword) {
        FestivalResponse festivalResponse = getFestivalData();

        if (festivalResponse == null || festivalResponse.getRow() == null) {
            return List.of();
        }

        String normalizedKeyword = normalize(keyword);

        if (normalizedKeyword.isBlank()) {
            return List.of();
        }

        return festivalResponse.getRow().stream()
                .filter(festival ->
                        normalize(festival.getTitle()).contains(normalizedKeyword) ||
                        normalize(festival.getInstNm()).contains(normalizedKeyword) ||
                        normalize(festival.getAddr()).contains(normalizedKeyword)
                )
                .collect(Collectors.toList());
    }


    // 상세페이지 (더 알아보기) 설정
 // 원본 제목이든 정규화된 제목이든 받아서 조회할 수 있게 유지
    public FestivalResponse.Row getFestivalByTitle(String title) {
        return getFestivalByNormalizedTitle(normalize(title));
    }

    // 정규화된 제목으로 조회할 때 사용하는 명확한 메서드
    public FestivalResponse.Row getFestivalByNormalizedTitle(String normalizedTitle) {
        return getFestivalData().getRow().stream()
                .filter(festival -> normalize(festival.getTitle()).equals(normalizedTitle))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Festival not found for normalizedTitle: " + normalizedTitle
                ));
    }

    // FestivalEntity의 identityKey로 사용할 공개 메서드
    public String createFestivalIdentityKey(FestivalResponse.Row festival) {
        return fingerprint(festival);
    }

    // 문자열을 표준화 하는 메소드
    // 특정 문자만 남기고 제거 , 공백 제거, 소문자로 변환
    public String normalize(String input) {
        // 입력 값이 null 이면 빈 문자열 ("") 를 반환
        if (input == null) return "";

        //문자열에서 허용된 문자만 남기고 제거한 후 표준화
        return input
                .replaceAll("[^a-zA-Z0-9가-힣\\[\\]']", "") // 알파벳, 숫자, 한글, 특정 특수문자('[]'를 제외한 나머지 제거
                .replace(" ", "") // 공백 제거
                .toLowerCase(); // 모든 알파벳 문자들을 소문자로 변환
    }


    // ✅ 같은 행사 판별용 "지문키" 생성
    private String fingerprint(FestivalResponse.Row r) {
        String title = canonicalTitle(r.getTitle());
        String begin = safe(r.getBeginDe());
        String end = safe(r.getEndDe());
        String time = norm(r.getEventTmInfo());
        String fee = norm(r.getPartcptExpnInfo());
        String addr = norm(r.getAddr());

        // 제목만 쓰면 위험해서(동명이 행사) 날짜/시간/참가비/주소까지 묶음
        return String.join("|", title, begin, end, time, fee, addr);
    }

    // ✅ < > 《 》 같은 장식 + 뒤에 붙는 영문 부제 제거해서 "같은 제목"으로 통일
    private String canonicalTitle(String raw) {
        if (raw == null) return "";

        String t = raw.trim();

        // 1) 장식 괄호/인용부호류 제거(문자만 제거, 내용은 유지)
        t = t.replaceAll("[<>《》「」『』\\[\\]\\(\\)\\{\\}]", " ");

        // 2) 구분 기호 정리
        t = t.replaceAll("[-–—:|·•]", " ");

        // 3) "한국어 제목 + (뒤에 영문만 길게 붙는 경우)" → 영문부제 잘라내기
        //    예) "사라지는 감각들 TRANSITS OF SENSES" → "사라지는 감각들"
        int lastKo = lastIndexOfKorean(t);
        if (lastKo != -1 && lastKo < t.length() - 1) {
            String tail = t.substring(lastKo + 1); // 한국어 끝 이후 부분
            if (tail.matches("[\\sA-Za-z0-9]+")) { // 뒤가 영문/숫자/공백 위주면 부제로 보고 제거
                t = t.substring(0, lastKo + 1);
            }
        }

        // 4) 공백 정리 + 소문자
        t = t.toLowerCase().replaceAll("\\s+", " ").trim();

        return t;
    }

    private int lastIndexOfKorean(String s) {
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c >= 0xAC00 && c <= 0xD7A3) { // 한글 음절 범위
                return i;
            }
        }
        return -1;
    }

    // ✅ 중복일 때 어떤 Row를 남길지 결정
    private FestivalResponse.Row chooseBetter(FestivalResponse.Row a, FestivalResponse.Row b) {
        // 1) 제목이 더 "깔끔한"(짧은) 쪽 우선: 영문부제/장식 많은 쪽을 버리기 좋음
        String ca = canonicalTitle(a.getTitle());
        String cb = canonicalTitle(b.getTitle());
        if (!ca.equals(cb)) {
            // 원래는 같은 키로 들어오므로 보통 같지만, 혹시 몰라 방어
        }
        int lenA = a.getTitle() == null ? Integer.MAX_VALUE : a.getTitle().length();
        int lenB = b.getTitle() == null ? Integer.MAX_VALUE : b.getTitle().length();
        if (lenA != lenB) return (lenA < lenB) ? a : b;

        // 2) 그 다음은 "정보가 더 많은" 쪽
        return score(b) > score(a) ? b : a;
    }

    private int score(FestivalResponse.Row r) {
        int s = 0;
        if (hasText(r.getImageUrl())) s += 5;
        if (hasText(r.getHmpgUrl())) s += 3;
        if (hasText(r.getUrl())) s += 2;
        if (hasText(r.getTelnoInfo())) s += 1;
        if (hasText(r.getAddr())) s += 1;
        if (hasText(r.getEventTmInfo())) s += 1;
        if (hasText(r.getPartcptExpnInfo())) s += 1;
        return s;
    }

    private boolean hasText(String v) {
        return v != null && !v.trim().isEmpty();
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private String norm(String v) {
        if (v == null) return "";
        return v.toLowerCase().replaceAll("\\s+", " ").trim();
    }

}
