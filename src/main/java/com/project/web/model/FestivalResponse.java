package com.project.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

//경기도 공공데이터 API의 XML 응답을 매핑하기 위한 DTO
//Jackson XmlMapper가 XML 태그명을 Java 필드에 매핑
public class FestivalResponse {

	private Head head;

	// XML 응답에서 row 태그가 별도 wrapper 없이 반복되므로 wrapping을 사용 X
	@JacksonXmlElementWrapper(useWrapping = false)
	private List<Row> row;

	public Head getHead() {
		return head;
	}

	public void setHead(Head head) {
		this.head = head;
	}

	public List<Row> getRow() {
		return row;
	}

	public void setRow(List<Row> row) {
		this.row = row;
	}

	public List<Row> getAllFestivals() {
		return row;
	}

	public static class Head {
		@JsonProperty("list_total_count")
		private int list_total_count;

		@JsonProperty("RESULT")
		private Result result;

		@JsonProperty("api_version")
		private String apiVersion;

		public int getList_total_count() {
			return list_total_count;
		}

		public void setList_total_count(int list_total_count) {
			this.list_total_count = list_total_count;
		}

		public Result getResult() {
			return result;
		}

		public void setResult(Result result) {
			this.result = result;
		}

		public String getApiVersion() {
			return apiVersion;
		}

		public void setApiVersion(String apiVersion) {
			this.apiVersion = apiVersion;
		}
	}

	public static class Result {
		@JsonProperty("CODE")
		private String code;

		@JsonProperty("MESSAGE")
		private String message;

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	// API 응답의 개별 행사 정보를 나타낸다
	// festivalId와 favoriteId는 API 원본 필드가 아니라 화면 표시와 내부 기능 연결을 위해 추가한 값이다
	public static class Row {
		@JsonProperty("ID")
		private String id;

		@JsonProperty("INST_NM")
		private String instNm;

		@JsonProperty("TITLE")
		private String title;

		@JsonProperty("CATEGORY_NM")
		private String categoryNm;

		@JsonProperty("URL")
		private String url;

		@JsonProperty("IMAGE_URL")
		private String imageUrl;

		@JsonProperty("BEGIN_DE")
		private String beginDe;

		@JsonProperty("END_DE")
		private String endDe;

		@JsonProperty("EVENT_TM_INFO")
		private String eventTmInfo;

		@JsonProperty("PARTCPT_EXPN_INFO")
		private String partcptExpnInfo;

		@JsonProperty("TELNO_INFO")
		private String telnoInfo;

		@JsonProperty("HOST_INST_NM")
		private String hostInstNm;

		@JsonProperty("HMPG_URL")
		private String hmpgUrl;

		@JsonProperty("WRITNG_DE")
		private String writngDe;

		@JsonProperty("ADDR")
		private String addr;

		private Long festivalId;
		private String favoriteId;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getInstNm() {
			return instNm;
		}

		public void setInstNm(String instNm) {
			this.instNm = instNm;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getCategoryNm() {
			return categoryNm;
		}

		public void setCategoryNm(String categoryNm) {
			this.categoryNm = categoryNm;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getImageUrl() {
			return imageUrl;
		}

		public void setImageUrl(String imageUrl) {
			this.imageUrl = imageUrl;
		}

		public String getBeginDe() {
			return beginDe;
		}

		public void setBeginDe(String beginDe) {
			this.beginDe = beginDe;
		}

		public String getEndDe() {
			return endDe;
		}

		public void setEndDe(String endDe) {
			this.endDe = endDe;
		}

		public String getEventTmInfo() {
			return eventTmInfo;
		}

		public void setEventTmInfo(String eventTmInfo) {
			this.eventTmInfo = eventTmInfo;
		}

		public String getPartcptExpnInfo() {
			return partcptExpnInfo;
		}

		public void setPartcptExpnInfo(String partcptExpnInfo) {
			this.partcptExpnInfo = partcptExpnInfo;
		}

		public String getTelnoInfo() {
			return telnoInfo;
		}

		public void setTelnoInfo(String telnoInfo) {
			this.telnoInfo = telnoInfo;
		}

		public String getHostInstNm() {
			return hostInstNm;
		}

		public void setHostInstNm(String hostInstNm) {
			this.hostInstNm = hostInstNm;
		}

		public String getHmpgUrl() {
			return hmpgUrl;
		}

		public void setHmpgUrl(String hmpgUrl) {
			this.hmpgUrl = hmpgUrl;
		}

		public String getWritngDe() {
			return writngDe;
		}

		public void setWritngDe(String writngDe) {
			this.writngDe = writngDe;
		}

		public String getAddr() {
			return addr;
		}

		public void setAddr(String addr) {
			this.addr = addr;
		}

		public Long getFestivalId() {
			return festivalId;
		}

		public void setFestivalId(Long festivalId) {
			this.festivalId = festivalId;
		}

		public String getFavoriteId() {
			return favoriteId;
		}

		public void setFavoriteId(String favoriteId) {
			this.favoriteId = favoriteId;
		}
	}
}