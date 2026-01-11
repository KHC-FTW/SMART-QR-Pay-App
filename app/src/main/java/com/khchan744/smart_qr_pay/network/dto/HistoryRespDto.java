package com.khchan744.smart_qr_pay.network.dto;

import java.util.List;

public class HistoryRespDto extends StandardRespDto{

    private List<History> history;

    public static class History{
        private String createdAt;
        private String description;

        public History(String createdAt, String description) {
            this.createdAt = createdAt;
            this.description = description;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getDescription() {
            return description;
        }
    }
    public HistoryRespDto(String status, String response, List<History> history) {
        super(status, response);
        this.history = history;
    }

    public List<History> getHistory() {
        return history;
    }
}
