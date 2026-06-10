package com.project.web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FestivalResponse {

    @JsonProperty("INFO")
    private int info;

    @JsonProperty("DATA")
    private List<Row> row;

    public int getInfo() {
        return info;
    }

    public void setInfo(int info) {
        this.info = info;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {

        @JsonProperty("writer")
        private String instNm;

        @JsonProperty("subject")
        private String title;

        @JsonProperty("category")
        private String categoryNm;

        @JsonProperty("href")
        private String url;

        @JsonProperty("thumbnail")
        private String imageUrl;

        @JsonProperty("startdate")
        private String beginDe;

        @JsonProperty("intime")
        private String eventTmInfo;

        @JsonProperty("incost")
        private String partcptExpnInfo;

        @JsonProperty("inquiry")
        private String telnoInfo;

        @JsonProperty("inarea")
        private String hostInstNm;

        @JsonProperty("homepage")
        private String hmpgUrl;

        @JsonProperty("created")
        private String writngDe;

        @JsonProperty("address")
        private String addr;

        private Long festivalId;
        private String favoriteId;

        @JsonProperty("enddate:")
        private String endDe;
        
        public String getEndDe() {return endDe;}

        public void setEndDe(String endDe) {this.endDe = endDe;}
        
        

        public String getInstNm() { return instNm; }
        public void setInstNm(String instNm) { this.instNm = instNm; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getCategoryNm() { return categoryNm; }
        public void setCategoryNm(String categoryNm) { this.categoryNm = categoryNm; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getBeginDe() { return beginDe; }
        public void setBeginDe(String beginDe) { this.beginDe = beginDe; }

        public String getEventTmInfo() { return eventTmInfo; }
        public void setEventTmInfo(String eventTmInfo) { this.eventTmInfo = eventTmInfo; }

        public String getPartcptExpnInfo() { return partcptExpnInfo; }
        public void setPartcptExpnInfo(String partcptExpnInfo) { this.partcptExpnInfo = partcptExpnInfo; }

        public String getTelnoInfo() { return telnoInfo; }
        public void setTelnoInfo(String telnoInfo) { this.telnoInfo = telnoInfo; }

        public String getHostInstNm() { return hostInstNm; }
        public void setHostInstNm(String hostInstNm) { this.hostInstNm = hostInstNm; }

        public String getHmpgUrl() { return hmpgUrl; }
        public void setHmpgUrl(String hmpgUrl) { this.hmpgUrl = hmpgUrl; }

        public String getWritngDe() { return writngDe; }
        public void setWritngDe(String writngDe) { this.writngDe = writngDe; }

        public String getAddr() { return addr; }
        public void setAddr(String addr) { this.addr = addr; }

        public Long getFestivalId() { return festivalId; }
        public void setFestivalId(Long festivalId) { this.festivalId = festivalId; }

        public String getFavoriteId() { return favoriteId; }
        public void setFavoriteId(String favoriteId) { this.favoriteId = favoriteId; }
    }
}