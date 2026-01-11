package com.khchan744.smart_qr_pay.bgservice;

import androidx.annotation.NonNull;

import java.util.Base64;

public class Format {

    public static int[] twoBytesToDecimals(@NonNull byte[] byteData) {
        int len = byteData.length;
        if (len % 2 != 0){
            throw new IllegalArgumentException("Byte array length must be even.");
        }
        int count = len / 2;
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            int msByte = byteData[2 * i] & 0xFF;    // most significant byte
            int lsByte = byteData[2 * i + 1] & 0xFF;    // least significant byte
            int decimalNum = (msByte << 8) | lsByte;
            /*
            * msByte: ________ << left shifted 8 bits
            * lsbyte:          ________
            * */
            result[i] = decimalNum & 0xFFFF;
        }
        return result;
    }

    public static int fourBytesToOneDecimal(@NonNull byte[] byteData) {
        int len = byteData.length;
        if (len != 4){
            throw new IllegalArgumentException("Byte array length must be 4.");
        }
        int result = 0;
        for (int i = 0, shift = 24; i < 4; i++, shift -= 8){
            result = (byteData[i] & 0xFF) << shift | result;
        }
        return result;
    }

    public static byte[] decimalsToTwoBytes(@NonNull int[] decimals) {
        byte[] result = new byte[decimals.length * 2];
        for (int i = 0; i < decimals.length; i++) {
            byte[] twoBytes = oneDecimalToTwoBytes(decimals[i]);
            result[2 * i] = twoBytes[0];  // most significant byte
            result[2 * i + 1] = twoBytes[1]; // least significant byte
        }
        return result;
    }

    public static byte[] oneDecimalToTwoBytes(@NonNull Integer decimal){
        byte[] result = new byte[2];
        if (decimal < 0 || decimal > 0xFFFF) {
            throw new IllegalArgumentException("Decimal number: " + decimal + " out of range.");
        }
        result[0] = (byte) ((decimal >> 8) & 0xFF);  // most significant byte
        result[1] = (byte) (decimal & 0xFF); // least significant byte
        return result;
    }

    public static byte[] oneDecimalToFourBytes(@NonNull Integer decimal){
        byte[] result = new byte[4];
        if (decimal < 0 || decimal > 0x3FFFFFFF) { // support max 30 bits
            throw new IllegalArgumentException("Decimal number: " + decimal + " out of range.");
        }
        for (int i = 0, shift = 24; i < 4; i++, shift -= 8){
            result[i] = (byte) ((decimal >> shift) & 0xFF); // from most to least significant byte
        }
        return result;
    }

    public static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] base64Decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }
    private Format(){

    }
}
