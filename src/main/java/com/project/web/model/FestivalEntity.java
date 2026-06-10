package com.project.web.model;

import jakarta.persistence.*;

//외부 API 행사 데이터를 내부 기능과 연결하기 위한 식별 Entity
@Entity
@Table(name = "festival", uniqueConstraints = @UniqueConstraint(name = "uk_festival_identity_key", columnNames = "identity_key"))
public class FestivalEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 제목, 날짜, 시간, 주소, 참가비 등을 조합한 fingerprint를 SHA-256으로 변환한 고유 키
	@Column(name = "identity_key", nullable = false, length = 64)
	private String identityKey;

	@Column(name = "normalized_title", nullable = false)
	private String normalizedTitle;

	@Column(nullable = false)
	private String title;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "homepage", length = 1000)
	private String homepage;

	@Column(name = "address", length = 500)
	private String address;

	@Column(name = "host_inst_nm", length = 500)
	private String hostInstNm;

	@Column(name = "begin_de", length = 200)
	private String beginDe;

	@Column(name = "end_de", length = 200)
	private String endDe;

	@Column(name = "category_nm", length = 500)
	private String categoryNm;

	@Column(name = "event_tm_info", length = 500)
	private String eventTmInfo;

	@Column(name = "partcpt_expn_info", length = 500)
	private String partcptExpnInfo;

	@Column(name = "telno_info", length = 500)
	private String telnoInfo;

	@Column(name = "hmpg_url", length = 1000)
	private String hmpgUrl;
	
	
	public String getCategoryNm() {
		return categoryNm;
	}

	public void setCategoryNm(String categoryNm) {
		this.categoryNm = categoryNm;
	}

	public String getTelnoInfo() {
		return telnoInfo;
	}

	public void setTelnoInfo(String telnoInfo) {
		this.telnoInfo = telnoInfo;
	}

	public String getHmpgUrl() {
		return hmpgUrl;
	}

	public void setHmpgUrl(String hmpgUrl) {
		this.hmpgUrl = hmpgUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getHomepage() {
		return homepage;
	}

	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getHostInstNm() {
		return hostInstNm;
	}

	public void setHostInstNm(String hostInstNm) {
		this.hostInstNm = hostInstNm;
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

	public Long getId() {
		return id;
	}

	public String getIdentityKey() {
		return identityKey;
	}

	public String getNormalizedTitle() {
		return normalizedTitle;
	}

	public String getTitle() {
		return title;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setIdentityKey(String identityKey) {
		this.identityKey = identityKey;
	}

	public void setNormalizedTitle(String normalizedTitle) {
		this.normalizedTitle = normalizedTitle;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}