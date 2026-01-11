package com.khchan744.smart_qr_pay.bgservice;

import androidx.annotation.NonNull;

import com.google.android.gms.common.util.ArrayUtils;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
    public static final String AES_ALGO = "AES";
    public static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    public static final int AES_KEY_SIZE_BITS = 128; // AES-128
    public static final int AES_KEY_SIZE_BYTES = AES_KEY_SIZE_BITS / 8; // 16 bytes
    public static final int GCM_IV_LENGTH_BYTES = 12; // recommended 12 bytes
    public static final int GCM_TAG_LENGTH_BITS = 96; // 12 bytes tag
    public static final int GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8;


    public static byte[] computeMetadataFingerprint(@NonNull Double quantizedLatitude,
                                                    @NonNull Double quantizedLongitude,
                                                    @NonNull Double quantizedAccelX,
                                                    @NonNull Double quantizedAccelY,
                                                    @NonNull Double quantizedAccelZ,
                                                    @NonNull Long quantizedTimestamp) throws NoSuchAlgorithmException
    {
        String concatData = "" + quantizedLatitude + quantizedLongitude
                + quantizedAccelX + quantizedAccelY + quantizedAccelZ
                + quantizedTimestamp;
        byte[] byteData = concatData.getBytes(StandardCharsets.UTF_8);
        return sha256Hash(byteData);
    }

    public static byte[] computePaymentToken(@NonNull byte[] uidBytes,
                                             @NonNull byte[] hashedPw,
                                             @NonNull byte[] paBytes,
                                             @NonNull byte[] mfBytes) throws NoSuchAlgorithmException
    {
        byte[] totalByteData = ArrayUtils.concatByteArrays(uidBytes, hashedPw, paBytes, mfBytes);
        return sha256Hash(totalByteData);
    }

    public static SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGO);
        keyGen.init(AES_KEY_SIZE_BITS, new SecureRandom());
        return keyGen.generateKey();
    }

    public static byte[] encryptAesGcm(SecretKey key, byte[] plaintext) throws Exception {
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(iv);

        // Encryption
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Prepend IV to ciphertext for transport
        return ArrayUtils.concatByteArrays(iv, ciphertext);
    }

    public static byte[] decryptAesGcm(SecretKey key, byte[] ivAndCiphertext) throws Exception {
        if (ivAndCiphertext.length <= GCM_IV_LENGTH_BYTES) throw new IllegalArgumentException("Invalid ciphertext");

        // Extract the IV from the concatenated data
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        System.arraycopy(ivAndCiphertext, 0, iv, 0, iv.length);
        // Extract ciphertext from the concatenated data
        byte[] ciphertext = new byte[ivAndCiphertext.length - iv.length];
        System.arraycopy(ivAndCiphertext, iv.length, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(ciphertext);
    }

    // Helper to make a SecretKey from raw key bytes
    public static SecretKey secretKeyFromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, AES_ALGO);
    }

    public static byte[] sha256Hash(byte[] data) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }
    public static String rawByteToHex(byte[] rawHashByte) throws Exception {
        StringBuilder sb = new StringBuilder(rawHashByte.length * 2);
        for (byte b : rawHashByte) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }


    private Crypto(){

    }
}
