package com.project.web.service;

import com.project.web.model.FestivalEntity;
import com.project.web.model.FestivalResponse;
import com.project.web.repository.FestivalRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 외부 행사 API와 내부 행사 DB 사이의 변환·조회 규칙을 담당하는 핵심 서비스
 * API 호출은 동기화 작업에서만 수행하고, 화면 목록·검색은 저장된 DB를 조회한다.
 */
@Service
public class FestivalService {

	
	private static final Logger log = LoggerFactory.getLogger(FestivalService.class);

	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
	
	@Value("${api.base-url}")
	private String apiBaseUrl;

	@Value("${api.max-pages:100}")
	private int maxPages;

	@Value("${api.page-size}")
	private int pageSize;

	private final RestTemplate restTemplate;
	private final FestivalRepository festivalRepository;
	
	public FestivalService(
	        RestTemplate restTemplate,
	        FestivalRepository festivalRepository) {

	    this.restTemplate = restTemplate;
	    this.festivalRepository = festivalRepository;
	}

	/**
	 * 모든 페이지 수집에 성공한 경우에만 DB 저장용 결과를 반환한다.
	 * 중간 실패 시 일부 수집 결과를 반환하지 않는다.
	 */
	public FestivalResponse getFestivalData() {
	    if (pageSize < 1 || pageSize > 1000 || maxPages < 1) {
	        throw new IllegalStateException("API 페이지 설정을 확인하세요.");
	    }

	    ObjectMapper mapper = new ObjectMapper();
	    Map<String, FestivalResponse.Row> collected = new LinkedHashMap<>();

	    try {
	        for (int page = 0; page < maxPages; page++) {
	            String url = UriComponentsBuilder.fromHttpUrl(apiBaseUrl)
	                    .replaceQueryParam("page", page)
	                    .replaceQueryParam("perpage", pageSize)
	                    .build(true)
	                    .toUriString();

	            String body = restTemplate.getForObject(url, String.class);

	            if (body == null || body.isBlank()) {
	                throw new IllegalStateException(
	                        "빈 HTTP 응답: page=" + page
	                );
	            }

	            JsonNode root = mapper.readTree(body);
	            JsonNode info = root == null ? null : root.get("INFO");

	            if (info == null
	                    || !info.isIntegralNumber()
	                    || !info.canConvertToInt()) {
	                throw new IllegalStateException(
	                        "잘못된 INFO: page=" + page
	                );
	            }

	            int code = info.intValue();
	            JsonNode data = root.get("DATA");

	            // API 명세의 '데이터 없음' 응답
	            if (code == 200) {
	                if (data != null
	                        && !data.isNull()
	                        && (!data.isArray() || !data.isEmpty())) {
	                    throw new IllegalStateException(
	                            "종료 응답과 DATA가 불일치합니다."
	                    );
	                }

	                return completedResponse(collected);
	            }

	            if (code != 0 || data == null || !data.isArray()) {
	                throw new IllegalStateException(
	                        "비정상 API 응답: page=" + page
	                                + ", INFO=" + code
	                );
	            }

	            // 실제 API는 INFO=0, DATA=[]로 마지막을 알리기도 한다.
	            if (data.isEmpty()) {
	                return completedResponse(collected);
	            }

	            int before = collected.size();

	            FestivalResponse pageResponse =
	                    mapper.treeToValue(root, FestivalResponse.class);

	            for (FestivalResponse.Row row : pageResponse.getRow()) {
	                String key = createFestivalIdentityKey(row);

	                if (row.getTitle() == null || row.getTitle().isBlank()) {
	                    throw new IllegalStateException(
	                            "제목이 없는 행사입니다."
	                    );
	                }

	                row.setBeginDe(normalizeApiDate(row.getBeginDe()));
	                row.setEndDe(normalizeApiDate(row.getEndDe()));

	                // 같은 원본 문서가 중복되면 기존 선택 규칙을 사용한다.
	                collected.merge(key, row, this::chooseBetter);
	            }

	            // 페이지 번호를 무시하고 같은 결과만 반환하는 상황 방지
	            if (collected.size() == before) {
	                throw new IllegalStateException(
	                        "새 행사가 없는 반복 페이지: " + page
	                );
	            }

	            /*
	             * 요청 크기보다 적게 반환돼도 다음 페이지를 요청한다.
	             * API 서버가 페이지당 반환 개수를 제한할 수 있기 때문이다.
	             */
	        }

	        throw new IllegalStateException(
	                "API 최대 페이지 수 초과: " + maxPages
	        );

	    } catch (Exception e) {
	        throw new IllegalStateException(
	                "전체 행사 수집 실패. DB 반영을 중단합니다.",
	                e
	        );
	    }
	}

	private FestivalResponse completedResponse(
	        Map<String, FestivalResponse.Row> collected
	) {
	    // 전체가 빈 경우에는 대량 비활성화하지 않고 기존 데이터를 유지한다.
	    if (collected.isEmpty()) {
	        throw new IllegalStateException(
	                "첫 페이지부터 비어 있어 기존 DB를 유지합니다."
	        );
	    }

	    FestivalResponse result = new FestivalResponse();
	    result.setInfo(0);
	    result.setRow(new ArrayList<>(collected.values()));

	    return result;
	}

	private String normalizeApiDate(String value) {
	    return value == null || value.isBlank()
	            ? null
	            : LocalDate.parse(value.trim()).toString();
	}

	// 검색어와 행사 데이터를 정규화한 뒤 제목, 기관명, 주소를 기준으로 검색
	// 공백/특수문자 차이로 검색이 실패하지 않도록 normalize 결과를 비교
	public List<FestivalResponse.Row> searchFestivals(String keyword) {

		String normalizedKeyword = normalize(keyword);
		
		if (normalizedKeyword.isBlank()) {
			return List.of();
		}
		
		return festivalRepository
				.searchActiveFestivals(normalizedKeyword)
				.stream()
				.map(this::toRow)
				.collect(Collectors.toList());
	}

	public FestivalEntity getActiveFestivalByNormalizedTitle(
	        String normalizedTitle
	) {
	    return festivalRepository
	            .findFirstByNormalizedTitleAndActiveTrueOrderByIdDesc(
	                    normalizedTitle
	            )
	            .orElseThrow(() -> new IllegalArgumentException(
	                    "활성 행사 정보를 찾을 수 없습니다: "
	                            + normalizedTitle
	            ));
	}

	// 외부 API 행사 데이터를 내부 DB 식별자로 연결하기 위한 identityKey를 생성
	// 변경 가능한 제목/기간/참가비 대신 원본 상세 문서 식별자를 사용한다.
	public String createFestivalIdentityKey(
	        FestivalResponse.Row festival
	) {
	    if (festival == null) {
	        throw new IllegalArgumentException("행사 데이터가 없습니다.");
	    }

	    return createFestivalIdentityKeyFromUrl(festival.getUrl());
	}

	private static final Pattern SOURCE_PATH =
	        Pattern.compile(
	                "^/cultureEvents/view/([A-Za-z0-9_-]+)/?$"
	        );

	public String createFestivalIdentityKeyFromUrl(String sourceUrl) {
	    if (sourceUrl == null || sourceUrl.isBlank()) {
	        throw new IllegalArgumentException(
	                "원본 행사 URL이 없습니다."
	        );
	    }

	    URI uri = URI.create(sourceUrl.trim());
	    String host = uri.getHost();

	    boolean allowedHost =
	            "ggc.ggcf.kr".equalsIgnoreCase(host)
	                    || "www.ggc.ggcf.kr".equalsIgnoreCase(host);

	    boolean allowedScheme =
	            "https".equalsIgnoreCase(uri.getScheme())
	                    || "http".equalsIgnoreCase(uri.getScheme());

	    if (!allowedHost
	            || !allowedScheme
	            || uri.getUserInfo() != null) {
	        throw new IllegalArgumentException(
	                "지원하지 않는 원본 행사 URL입니다."
	        );
	    }

	    var matcher = SOURCE_PATH.matcher(uri.getPath());

	    if (!matcher.matches()) {
	        throw new IllegalArgumentException(
	                "원본 행사 ID를 확인할 수 없습니다."
	        );
	    }

	    return sha256(
	            "ggc:cultureEvents:" + matcher.group(1)
	    );
	}

	// 검색과 제목 비교를 위해 문자열을 표준화
	// 영문/숫자/한글 일부 특수문자만 남기고 공백 제거 및 소문자 변환을 수행
	public String normalize(String input) {
		// 입력 값이 null 이면 빈 문자열 ("") 를 반환
		if (input == null)
			return "";

		// 문자열에서 허용된 문자만 남기고 제거한 후 표준화
		return input.replaceAll("[^a-zA-Z0-9가-힣\\[\\]']", "") // 알파벳, 숫자, 한글, 특정 특수문자('[]'를 제외한 나머지 제거
				.replace(" ", "") // 공백 제거
				.toLowerCase(); // 모든 알파벳 문자들을 소문자로 변환
	}


	// 행사 제목 비교를 안정적으로 하기 위해 장식 문자와 영문 부제를 정리
	private String canonicalTitle(String raw) {
		if (raw == null)
			return "";

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
		if (lenA != lenB)
			return (lenA < lenB) ? a : b;

		// 이미지, 홈페이지, 주소, 시간 등 화면에 유용한 정보가 많을수록 높은 점수를 부여
		return score(b) > score(a) ? b : a;
	}

	private int score(FestivalResponse.Row r) {
		int s = 0;
		if (hasText(r.getImageUrl()))
			s += 5;
		if (hasText(r.getHmpgUrl()))
			s += 3;
		if (hasText(r.getUrl()))
			s += 2;
		if (hasText(r.getTelnoInfo()))
			s += 1;
		if (hasText(r.getAddr()))
			s += 1;
		if (hasText(r.getEventTmInfo()))
			s += 1;
		if (hasText(r.getPartcptExpnInfo()))
			s += 1;
		return s;
	}

	private boolean hasText(String v) {
		return v != null && !v.trim().isEmpty();
	}

	private String safe(String v) {
		return v == null ? "" : v.trim();
	}

	private String norm(String v) {
		if (v == null)
			return "";
		return v.toLowerCase().replaceAll("\\s+", " ").trim();
	}

	// 원본 행사 식별 문자열을 고정 길이의 SHA-256 해시값으로 변환
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
	
	// 현재 노출 가능한 전체 행사를 시작일 내림차순으로 조회한다.
	public List<FestivalEntity> getFestivalsFromDb() {

	    return festivalRepository
	            .findByActiveTrueOrderByBeginDeDesc();
	}
	
	// 기존 목록 화면과 호환하기 위한 페이징 없는 카테고리 조회
	public List<FestivalEntity> getByCategory(
	        String category) {

	    return festivalRepository
	    		.findByCategoryNmAndActiveTrueOrderByBeginDeDesc(category);
	}
	
	/**
	 * 카테고리 목록에 서버 DB 페이징과 정렬을 적용한다.
	 * 한 번에 모든 행사를 브라우저로 보내지 않아 목록이 많아져도 응답 크기를 제한한다.
	 */
	public Page<FestivalEntity> getByCategory(
	        String category,
	        Pageable pageable
	) {
	    return festivalRepository.findByCategoryNmAndActiveTrue(
	            category,
	            pageable
	    );
	}

	// 종료된 행사의 카테고리 필터와 서버 DB 페이징을 적용한다.
	public Page<FestivalEntity> getEndedFestivals(
	        String category,
	        Pageable pageable
	) {
	    return festivalRepository.findEndedFestivals(
	            todayInKorea(),
	            category,
	            pageable
	    );
	}
	
	// 종료일이 오늘보다 이전인 행사만 종료 행사 보관함에 전달한다.
	public List<FestivalEntity> getEndedFestivals() {
	    return festivalRepository.findEndedFestivals(todayInKorea());
	}

	// 종료 행사 보관함의 상세 페이지용 조회
	public FestivalEntity getEndedFestivalById(Long festivalId) {
	    return festivalRepository
	            .findEndedFestivalById(festivalId, todayInKorea())
	            .orElseThrow(() -> new IllegalArgumentException(
	                    "종료 행사 정보를 찾을 수 없습니다. id=" + festivalId));
	}

	// 행사 종료 판단 기준을 한국 시간으로 고정한다.
	private String todayInKorea() {
	    return LocalDate.now(KOREA_ZONE).toString();
	}
	
	
	// DB 엔티티를 기존 화면과 호환되는 API 응답 Row 형태로 변환한다.
	private FestivalResponse.Row toRow(FestivalEntity entity) {

	    FestivalResponse.Row row = new FestivalResponse.Row();

	    row.setFestivalId(entity.getId());

	    row.setTitle(entity.getTitle());

	    row.setAddr(entity.getAddress());

	    row.setCategoryNm(entity.getCategoryNm());

	    row.setImageUrl(entity.getImageUrl());

	    row.setHostInstNm(entity.getHostInstNm());

	    row.setBeginDe(entity.getBeginDe());

	    row.setEndDe(entity.getEndDe());

	    row.setEventTmInfo(entity.getEventTmInfo());

	    row.setPartcptExpnInfo(entity.getPartcptExpnInfo());

	    row.setTelnoInfo(entity.getTelnoInfo());
	    
	    row.setUrl(
	            entity.getHmpgUrl() != null
	                    ? entity.getHmpgUrl()
	                    : entity.getHomepage()
	    );

	    row.setHmpgUrl(entity.getHmpgUrl());

	    return row;
	}
}
