package com.khchan744.smart_qr_pay.network.dto;

public class StandardRespDto {
    private String status;
    private String response;

    public StandardRespDto(String status, String response) {
        this.status = status;
        this.response = response;
    }

    public String getStatus() {
        return status;
    }

    public String getResponse() {
        return response;
    }

    public boolean isSuccess() {
        return status != null && status.equalsIgnoreCase("success");
    }
}

