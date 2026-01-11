package com.khchan744.smart_qr_pay.network.dto;

public class PaymentReqDto {
    private final String paymentToken;
    private final String payerUsername;
    private final String paymentAmount;
    private final String payeeMetadataFingerprint;

    public PaymentReqDto(String paymentToken, String payerUsername, String paymentAmount, String payeeMetadataFingerprint) {
        this.paymentToken = paymentToken;
        this.payerUsername = payerUsername;
        this.paymentAmount = paymentAmount;
        this.payeeMetadataFingerprint = payeeMetadataFingerprint;
    }

    public String getPaymentToken() {
        return paymentToken;
    }

    public String getPayerUsername() {
        return payerUsername;
    }

    public String getPaymentAmount() {
        return paymentAmount;
    }

    public String getPayeeMetadataFingerprint() {
        return payeeMetadataFingerprint;
    }
}
