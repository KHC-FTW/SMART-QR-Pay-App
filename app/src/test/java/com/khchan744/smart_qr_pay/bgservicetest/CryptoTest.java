package com.khchan744.smart_qr_pay.bgservicetest;

import com.khchan744.smart_qr_pay.bgservice.Crypto;
import com.khchan744.smart_qr_pay.bgservice.Format;
import com.khchan744.smart_qr_pay.bgservice.FuzzyVault;
import com.khchan744.smart_qr_pay.bgservice.Payload;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.SecretKey;

public class CryptoTest {

    @Test
    public void testComputeMetadataFingerprint() throws Exception {
        final long timestamp = 1766397561789L;
        final double latitude = 22.2780783;
        final double longitude = 114.2261503;
        final double accelerometerX = 0.7636095;
        final double accelerometerY = 4.9725847;
        final double accelerometerZ = 7.579864;

        byte[] mf = Crypto.computeMetadataFingerprint(latitude, longitude,
                accelerometerX, accelerometerY, accelerometerZ, timestamp);

        String mfHex = Crypto.rawByteToHex(mf);
        int[] mfDec = Format.twoBytesToDecimals(mf);
        System.out.println("Metadata fingerprint decimal: " + Arrays.toString(mfDec));

        System.out.println("Metadata fingerprint bytes: " + Arrays.toString(mf));
        System.out.println("Metadata fingerprint (hex): " + mfHex);

        Assert.assertEquals(32, mf.length);
    }

    @Test
    public void testComputePaymentToken() throws Exception {
        final byte[] uid = "khchan744".getBytes(StandardCharsets.UTF_8);
        final byte[] hashedPw = Crypto.sha256Hash("fffsd78asd@bf".getBytes(StandardCharsets.UTF_8));
        byte[] paBytes = Payload.paymentAmountToBytes("100", "5");

        final long timestamp = 1766397561789L;
        final double latitude = 22.2780783;
        final double longitude = 114.2261503;
        final double accelerometerX = 0.7636095;
        final double accelerometerY = 4.9725847;
        final double accelerometerZ = 7.579864;
        final byte[] mf = Crypto.computeMetadataFingerprint(latitude, longitude,
                accelerometerX, accelerometerY, accelerometerZ, timestamp);

        final byte[] pt = Crypto.computePaymentToken(uid, hashedPw, paBytes, mf);
        String ptHex = Crypto.rawByteToHex(pt);

        System.out.println("Payment token bytes: " + Arrays.toString(pt));
        System.out.println("Payment token (hex): " + ptHex);

        Assert.assertEquals(32, pt.length);
    }

    @Test
    public void testSecretKeyConversion() throws Exception {
        SecretKey key = Crypto.generateAesKey();
        byte[] keyBytes = key.getEncoded();
        Assert.assertEquals(16, keyBytes.length);
        SecretKey recoveredKey = Crypto.secretKeyFromBytes(keyBytes);
        Assert.assertEquals(key, recoveredKey);
    }

    @Test
    public void testEncryptDecrypt() throws Exception {
        final byte[] uid = "khchan744".getBytes(StandardCharsets.UTF_8);
        final byte[] hashedPw = Crypto.sha256Hash("fffsd78asd@bf".getBytes(StandardCharsets.UTF_8));
        final byte[] paBytes = Payload.paymentAmountToBytes("100", "5");

        final long timestamp = 1766397561789L;
        final double latitude = 22.2780783;
        final double longitude = 114.2261503;
        final double accelerometerX = 0.7636095;
        final double accelerometerY = 4.9725847;
        final double accelerometerZ = 7.579864;
        final byte[] mf = Crypto.computeMetadataFingerprint(latitude, longitude,
                accelerometerX, accelerometerY, accelerometerZ, timestamp);

        final byte[] pt = Crypto.computePaymentToken(uid, hashedPw, paBytes, mf);

        SecretKey key = Crypto.generateAesKey();
        final byte[] ivAndCiphertext = Crypto.encryptAesGcm(key, pt);

        Assert.assertEquals(pt.length + Crypto.GCM_IV_LENGTH_BYTES + Crypto.GCM_TAG_LENGTH_BYTES, ivAndCiphertext.length);

        final byte[] decryptedPt = Crypto.decryptAesGcm(key, ivAndCiphertext);

        Assert.assertArrayEquals(pt, decryptedPt);
    }

}
