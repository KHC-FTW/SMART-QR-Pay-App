package com.khchan744.smart_qr_pay.param;

import com.khchan744.smart_qr_pay.bgservice.Crypto;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

public class GlobalConst {

    public static final int UID_MAX_LEN = 15;
    public static final int HASHED_PW_BYTE_LEN = 32;    //SHA-256

    public static final String TEST_UID = "khchan744";

    public static final byte[] TEST_SECRET;

    static{
        try {
            TEST_SECRET = Crypto.sha256Hash("!123456AbC!".getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private GlobalConst(){

    }
}
