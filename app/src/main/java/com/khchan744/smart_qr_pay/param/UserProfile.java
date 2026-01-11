package com.khchan744.smart_qr_pay.param;

public class UserProfile {
    private static UserProfile userProfile;
    private String username;
    private String jwt;
    private String secret;
    private byte[] secretBytes;

    /*private boolean isUsernameNotSet = true;
    private boolean isJwtNotSet = true;
    private boolean isSecretNotSet = true;*/

    public static UserProfile getInstance(){
        if (userProfile == null){
            userProfile = new UserProfile();
        }
        return userProfile;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public byte[] getSecretBytes() {
        return secretBytes;
    }

    public void setSecretBytes(byte[] secretBytes) {
        this.secretBytes = secretBytes;
    }

    public void reset(){
        this.username = null;
        this.jwt = null;
        this.secret = null;
    }
}
