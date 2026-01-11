package com.khchan744.smart_qr_pay.network.dto;

public class TopUpReqDto {
    private final String topUpAmount;

    public TopUpReqDto(String topUpAmount) {
        this.topUpAmount = topUpAmount;
    }

    public String getTopUpAmount() {
        return topUpAmount;
    }
}
