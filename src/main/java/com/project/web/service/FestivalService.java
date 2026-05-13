package com.project.web.service;

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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//경기도 공공데이터 API에서 행사 데이터를 가져오고,
//검색, 상세 조회, 중복 제거, 내부 식별키 생성을 담당하는 서비스
@Service
public class FestivalService {

	private static final Logger log = LoggerFactory.getLogger(FestivalService.class);
	
	// API 호출에 필요한 설정값은 application.properties 또는 배포 환경변수에서 주입
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

    // 외부 공공데이터 API에서 행사 XML 데이터를 조회해 Java 객체로 변환
    // 동일한 요청이 반복되지 않도록 결과를 캐시에 저장하고,
    // API 응답 내 중복 행사는 fingerprint 기준으로 제거
    @Cacheable(value = "festivals", key = "'all'",
    		unless = "#result == null || #result.getRow() == null || #result.getRow().isEmpty()")
    public FestivalResponse getFestivalData() {
        try {
        	// API URL에 인증키, 페이지 번호, 페이지 크기를 query parameter로 추가
            String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
                    .queryParam("KEY", apiKey)
                    .queryParam("pIndex", pageIndex)
                    .queryParam("pSize", pageSize)
                    .build(true)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);

            // 공공데이터 API의 XML 응답을 FestivalResponse 객체로 매핑한다.
            XmlMapper xmlMapper = new XmlMapper();
            FestivalResponse festivalResponse = xmlMapper.readValue(response, FestivalResponse.class);

            if (festivalResponse != null && festivalResponse.getAllFestivals() != null) {
                List<FestivalResponse.Row> distinctFestivals = festivalResponse.getAllFestivals().stream()
                		// 같은 행사로 판단되는 Row를 fingerprint 기준으로 묶고,
                		// 중복이 발생하면 더 적합한 데이터를 chooseBetter로 선택
                        .collect(Collectors.toMap(
                                this::fingerprint,
                                Function.identity(),
                                this::chooseBetter,
                                java.util.LinkedHashMap::new
                        ))
                        .values()
                        .stream()
                        .collect(Collectors.toList());

                festivalResponse.setRow(distinctFestivals);
            }

            if (festivalResponse == null) {
                FestivalResponse empty = new FestivalResponse();
                empty.setRow(List.of());
                return empty;
            }

            return festivalResponse;

         // 외부 API 오류가 발생해도 서비스 전체가 중단되지 않도록 빈 응답을 반환
        } catch (Exception e) {
        	log.error("공공데이터 API 조회 실패. apiBaseUrl={}", apiBaseUrl, e);

            FestivalResponse empty = new FestivalResponse();
            empty.setRow(List.of());
            return empty;
        }
    }

    // 검색어와 행사 데이터를 정규화한 뒤 제목, 기관명, 주소를 기준으로 검색
    // 공백/특수문자 차이로 검색이 실패하지 않도록 normalize 결과를 비교
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
                        normalize(festival.getAddr()).contains(normalizedKeyword) ||
                        normalize(festival.getHostInstNm()).contains(normalizedKeyword)
                )
                .collect(Collectors.toList());
    }


    // 원본 제목으로 들어온 상세 조회 요청을 정규화 제목 기반 조회로 변환
    public FestivalResponse.Row getFestivalByTitle(String title) {
        return getFestivalByNormalizedTitle(normalize(title));
    }

    // 정규화된 제목과 정확히 일치하는 행사를 찾고,
    // 제목 기반 상세 URL을 festivalId 기반 URL로 변환할 때 사용
    public FestivalResponse.Row getFestivalByNormalizedTitle(String normalizedTitle) {
        return getFestivalData().getRow().stream()
                .filter(festival -> normalize(festival.getTitle()).equals(normalizedTitle))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Festival not found for normalizedTitle: " + normalizedTitle
                ));
    }

    // 외부 API 행사 데이터를 내부 DB 식별자로 연결하기 위한 identityKey를 생성
    // fingerprint를 SHA-256으로 변환해 festival 테이블의 고유 키로 사용
    public String createFestivalIdentityKey(FestivalResponse.Row festival) {
        String rawKey = fingerprint(festival);
        return sha256(rawKey);
    }

    // 검색과 제목 비교를 위해 문자열을 표준화
    // 영문/숫자/한글 일부 특수문자만 남기고 공백 제거 및 소문자 변환을 수행
    public String normalize(String input) {
        // 입력 값이 null 이면 빈 문자열 ("") 를 반환
        if (input == null) return "";

        //문자열에서 허용된 문자만 남기고 제거한 후 표준화
        return input
                .replaceAll("[^a-zA-Z0-9가-힣\\[\\]']", "") // 알파벳, 숫자, 한글, 특정 특수문자('[]'를 제외한 나머지 제거
                .replace(" ", "") // 공백 제거
                .toLowerCase(); // 모든 알파벳 문자들을 소문자로 변환
    }


    // 같은 행사인지 판단하기 위한 원본 키를 만들고,
    // 제목만 사용하면 동명이 행사 문제가 생길 수 있어 날짜, 시간, 참가비, 주소까지 함께 사용
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

    // 행사 제목 비교를 안정적으로 하기 위해 장식 문자와 영문 부제를 정리
    private String canonicalTitle(String raw) {
        if (raw == null) return "";

        String t = raw.trim();

        // 1) 장식 괄호/인용부호류 제거(문자만 제거, 내용은 유지)
        t = t.replaceAll("[<>《》「」『』\\[\\]\\(\\)\\{\\}]", " ");

        // 2) 구분 기호 정리
        t = t.replaceAll("[-–—:|·•]", " ");

        // 제목 뒤에 붙은 영문 부제를 제거하기 위해 마지막 한글 위치를 찾는다
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

    // 중복 행사 중 화면에 표시하기 더 좋은 Row를 선택
    // 우선 제목이 더 깔끔한 데이터를 선택하고, 길이가 같으면 정보가 더 많은 데이터를 선택
    private FestivalResponse.Row chooseBetter(FestivalResponse.Row a, FestivalResponse.Row b) {
        String ca = canonicalTitle(a.getTitle());
        String cb = canonicalTitle(b.getTitle());
        if (!ca.equals(cb)) {
            // 원래는 같은 키로 들어오므로 보통 같지만, 혹시 몰라 방어
        }
        int lenA = a.getTitle() == null ? Integer.MAX_VALUE : a.getTitle().length();
        int lenB = b.getTitle() == null ? Integer.MAX_VALUE : b.getTitle().length();
        if (lenA != lenB) return (lenA < lenB) ? a : b;

        // 이미지, 홈페이지, 주소, 시간 등 화면에 유용한 정보가 많을수록 높은 점수를 부여
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

    // fingerprint 문자열을 고정 길이의 SHA-256 해시값으로 변환
    // DB unique key로 사용하기 위해 길이와 형식을 일정하게 만든다
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
