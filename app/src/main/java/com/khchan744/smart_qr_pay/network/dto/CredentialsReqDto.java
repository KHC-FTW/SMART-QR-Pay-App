package com.khchan744.smart_qr_pay.network.dto;

public class CredentialsReqDto {
    private final String username;
    private final String password;

    public CredentialsReqDto(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

