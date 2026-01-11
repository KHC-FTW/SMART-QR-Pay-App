package com.khchan744.smart_qr_pay.network.dto;

public class LoginRespDto extends StandardRespDto{

    private final String jwt;
    private final String secret;

    public LoginRespDto(String status,
                        String response,
                        String jwt,
                        String secret) {
        super(status, response);
        this.jwt = jwt == null ? "" : jwt;
        this.secret = secret == null ? "" : secret;
    }

    public String getJwt() {
        return jwt;
    }

    public String getSecret() {
        return secret;
    }
}
