package com.khchan744.smart_qr_pay.bgservice;

import androidx.annotation.NonNull;

import com.google.android.gms.common.util.ArrayUtils;
import com.khchan744.smart_qr_pay.customexception.PayloadException;
import com.khchan744.smart_qr_pay.param.GlobalConst;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import javax.crypto.SecretKey;

public class Payload {
    /*
    * This class is for compiling and finalizing payloads ready for transmission,
    * including format conversion, concatenation, assembling, etc.
    * */

    public static final int FV_BYTE_SIZE = FuzzyVault.FUZZY_VAULT_SIZE * 4;
    public static final int PAYMENT_TOKEN_BYTE_SIZE = 32; // SHA-256 token size
    public static final int ENC_KEY_QR_PAYLOAD_BYTE_SIZE = Crypto.GCM_IV_LENGTH_BYTES + Crypto.GCM_TAG_LENGTH_BYTES + 16; // AES-128 key size
    public static final int PA_BYTE_SIZE = 3; // 2 bytes for integer, 1 byte for fraction
    public static final int OFFSET_BYTE_SIZE = 4;
    public static final int PAYLOAD_SET_1_MIN_SIZE = FV_BYTE_SIZE + Crypto.GCM_IV_LENGTH_BYTES + Crypto.GCM_TAG_LENGTH_BYTES + PAYMENT_TOKEN_BYTE_SIZE + PA_BYTE_SIZE + 1;
    public static final int PAYLOAD_SET_1_MAX_SIZE = PAYLOAD_SET_1_MIN_SIZE - 1 + GlobalConst.UID_MAX_LEN;
    public static final int PAYLOAD_SET_2_SIZE = ENC_KEY_QR_PAYLOAD_BYTE_SIZE + OFFSET_BYTE_SIZE * 6;
    public static final String ENC_KEY_QR_PAYLOAD_KEY = "encryptedKeyQrPayload";
    public static final String TIME_OFFSET_KEY = "timeOffset";
    public static final String LATITUDE_OFFSET_KEY = "latitudeOffset";
    public static final String LONGITUDE_OFFSET_KEY = "longitudeOffset";
    public static final String ACCEL_X_OFFSET_KEY = "accelXOffset";
    public static final String ACCEL_Y_OFFSET_KEY = "accelYOffset";
    public static final String ACCEL_Z_OFFSET_KEY = "accelZOffset";
    public static final String FV_KEY = "fv";
    public static final String ENC_QR_PAYLOAD = "encryptedQrPayload";
    public static final String DEC_PAY_TOKEN_KEY = "decryptedPaymentToken";
    public static final String PA_KEY = "pa";
    public static final String UID_KEY = "uid";
    public static final String MF_KEY = "metadataFingerprint";

    public static final String PAYLOAD_SET_1_KEY = "payloadSet1";
    public static final String PAYLOAD_SET_2_KEY = "payloadSet2";

    public static byte[] offsetValueToFourBytes(@NonNull Double offset){
        boolean positive = offset >= 0;
        String val = String.format(Locale.US, "%.9f", offset);
        val = positive ? val.substring(2) : val.substring(3);
        int value = Integer.parseInt(val);
        byte[] offsetBytes = Format.oneDecimalToFourBytes(value);
        if(positive) offsetBytes[0] = (byte)(offsetBytes[0] | 0x80);  // add sign bit to the most significant bit
        return offsetBytes;
    }

    public static double fourBytesToOffsetValue(@NonNull byte[] offsetBytes){
        if (offsetBytes.length != 4){
            throw new IllegalArgumentException("Input array must have a length of 4");
        }
        int sign = (offsetBytes[0] & 0xFF) >> 7; // most significant bit is the sign bit
        boolean positive = sign == 1;
        if (positive) offsetBytes[0] = (byte)(offsetBytes[0] & 0x7F);   // remove the sign bit
        int value = Format.fourBytesToOneDecimal(offsetBytes);
        // apply 0 padding:
        int valLen = String.valueOf(value).length();
        StringBuilder zeros = new StringBuilder("0.");
        for (int i = 0; i < 9 - valLen; i++){
            zeros.append("0");
        }
        String result = positive ? zeros.append(value).toString() : "-" + zeros.append(value);
        return Double.parseDouble(result);
    }

    public static byte[] paymentAmountToBytes(String integer, String fraction){
        if (integer.length() > 4 || fraction.length() > 1){
            // integer must be 0 - 9999; fraction must be 0 - 9
            throw new IllegalArgumentException("Invalid payment amount");
        }
        if(fraction.isBlank()) fraction = "0";
        byte[] result = new byte[3]; // 2 bytes for integer, 1 byte for fraction
        byte[] integerBytes = Format.oneDecimalToTwoBytes(Integer.parseInt(integer));
        result[0] = integerBytes[0];
        result[1] = integerBytes[1];
        result[2] = (byte)(Integer.parseInt(fraction) & 0xF);
        return result;
    }

    public static float bytesToPaymentAmount(byte[] paBytes){
        if (paBytes.length != 3) {
            throw new IllegalArgumentException("Input array must have a length of 3");
        }
        byte[] integerBytes = new byte[2];
        integerBytes[0] = paBytes[0];
        integerBytes[1] = paBytes[1];
        int integer = Format.twoBytesToDecimals(integerBytes)[0];
        int fraction = paBytes[2] & 0xF;
        return Float.parseFloat(integer + "." + fraction);
    }

    /*
    * Payload set 1 to be encoded into a QR code.
    * */
    public static byte[] finalizePayloadSet1(byte[] fuzzyVault, byte[] encryptedQrPayload){
        // fuzzy vault(bytes) + IV+ciphertext of [payment token(bytes) + payment amount(bytes) + UID(byte)]
        if (fuzzyVault.length != FV_BYTE_SIZE) {
            throw new IllegalArgumentException("fuzzyVault must have a length of " + FV_BYTE_SIZE);
        }
        if((fuzzyVault.length + encryptedQrPayload.length) < PAYLOAD_SET_1_MIN_SIZE
                || (fuzzyVault.length + encryptedQrPayload.length) > PAYLOAD_SET_1_MAX_SIZE){
            throw new IllegalArgumentException("payloadSet1 must have a length between " + PAYLOAD_SET_1_MIN_SIZE + " - " + PAYLOAD_SET_1_MAX_SIZE);
        }
        return ArrayUtils.concatByteArrays(fuzzyVault, encryptedQrPayload);
    }
    public static Map<String, byte[]> unpackPayloadSet1(byte[] payloadSet1){
        // fuzzy vault + IV+ciphertext of [payment token + payment amount + UID (variable length)]
        if (payloadSet1.length < PAYLOAD_SET_1_MIN_SIZE || payloadSet1.length > PAYLOAD_SET_1_MAX_SIZE){
            throw new IllegalArgumentException("payloadSet1 must have a length between " + PAYLOAD_SET_1_MIN_SIZE + " - " + PAYLOAD_SET_1_MAX_SIZE);
        }
        byte[] fv = new byte[FV_BYTE_SIZE];
        byte[] encQrPayload = new byte[payloadSet1.length - FV_BYTE_SIZE];
        System.arraycopy(payloadSet1, 0, fv, 0, FV_BYTE_SIZE);
        System.arraycopy(payloadSet1, FV_BYTE_SIZE, encQrPayload, 0, encQrPayload.length);
        return Map.of(
                FV_KEY, fv,
                ENC_QR_PAYLOAD, encQrPayload
        );
    }

    /*
    * Payload set 2 to be transmitted via ultrasonic audio channel.
    * */
    public static byte[] finalizePayloadSet2(byte[] encryptedKeyForQrPayload, byte[][] metadataOffsets){
        // encryptedKeyForQrPayload (16 bytes + 12 bytes IV + 12 bytes tag) + 6 offset values (4 bytes each)
        if (encryptedKeyForQrPayload.length != ENC_KEY_QR_PAYLOAD_BYTE_SIZE) {
            throw new IllegalArgumentException("encryptedKeyForQrPayload must have a length of " + ENC_KEY_QR_PAYLOAD_BYTE_SIZE);
        }
        if (metadataOffsets.length != 6){
            throw new IllegalArgumentException("metadataOffsets must have a length of 6");
        }
        byte[] totalOffsetBytes = new byte[6 * OFFSET_BYTE_SIZE];
        for (int i = 0; i < 6; i++){
            if (metadataOffsets[i].length != OFFSET_BYTE_SIZE){
                throw new IllegalArgumentException("metadataOffsets[" + i + "] must have a length of " + OFFSET_BYTE_SIZE);
            }
            System.arraycopy(metadataOffsets[i], 0, totalOffsetBytes, OFFSET_BYTE_SIZE * i, OFFSET_BYTE_SIZE);
        }
        return ArrayUtils.concatByteArrays(encryptedKeyForQrPayload, totalOffsetBytes);
    }

    public static Map<String, byte[]> unpackPayloadSet2(byte[] payloadSet2){
        // encryptedKeyQrPayload (16 bytes + 12 bytes IV + 12 bytes tag) + 6 offset values (4 bytes each)
        if (payloadSet2.length != PAYLOAD_SET_2_SIZE){
            throw new IllegalArgumentException("payloadSet2 must have a length of " + PAYLOAD_SET_2_SIZE);
        }
        byte[] encryptedKeyQrPayload = new byte[ENC_KEY_QR_PAYLOAD_BYTE_SIZE];
        byte[][] metadataOffsets = new byte[6][OFFSET_BYTE_SIZE];
        System.arraycopy(payloadSet2, 0, encryptedKeyQrPayload, 0, ENC_KEY_QR_PAYLOAD_BYTE_SIZE);
        for (int i = 0; i < 6; i++){
            System.arraycopy(payloadSet2, ENC_KEY_QR_PAYLOAD_BYTE_SIZE + OFFSET_BYTE_SIZE * i, metadataOffsets[i], 0, OFFSET_BYTE_SIZE);
        }
        return Map.of(
                ENC_KEY_QR_PAYLOAD_KEY, encryptedKeyQrPayload,
                TIME_OFFSET_KEY, metadataOffsets[0],
                LATITUDE_OFFSET_KEY, metadataOffsets[1],
                LONGITUDE_OFFSET_KEY, metadataOffsets[2],
                ACCEL_X_OFFSET_KEY, metadataOffsets[3],
                ACCEL_Y_OFFSET_KEY, metadataOffsets[4],
                ACCEL_Z_OFFSET_KEY, metadataOffsets[5]
        );
    }

    public static String uidBytesToString(byte[] uidBytes){
        return new String(uidBytes, StandardCharsets.UTF_8);
    }

    public static boolean isPayloadSet1Valid(byte[] payloadSet1){
        return payloadSet1 != null
                && payloadSet1.length > PAYLOAD_SET_1_MIN_SIZE
                && payloadSet1.length <= PAYLOAD_SET_1_MAX_SIZE;
    }

    public static boolean isPayloadSet2Valid(byte[] payloadSet2){
        return payloadSet2 != null && payloadSet2.length == PAYLOAD_SET_2_SIZE;
    }

    public static Map<String, byte[]> rawInputsToPayloads(@NonNull final String uid,
                                                          @NonNull final byte[] hashedPw,
                                                          @NonNull final String paIntVal,
                                                          @NonNull final String paFracVal,
                                                          @NonNull final Long timestamp,
                                                          @NonNull final Double latitude,
                                                          @NonNull final Double longitude,
                                                          @NonNull final Float accelX,
                                                          @NonNull final Float accelY,
                                                          @NonNull final Float accelZ) throws Exception {
        final byte[] uidBytes = uid.getBytes(StandardCharsets.UTF_8);
        byte[] paBytes = paymentAmountToBytes(paIntVal, paFracVal);

        // Quantize timestamp
        Map<String, Object> quantizedTimeResults = Arithmetic.quantizeRefValue(
                timestamp, Metadata.TIME_MS_TOL, Metadata.TIME_IS_TWO_SIDED);
        final long quantizedTime = (long)quantizedTimeResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double timeOffset = (double)quantizedTimeResults.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] timeOffsetBytes = offsetValueToFourBytes(timeOffset);

        // Quantize latitude
        Map<String, Double> quantizedLatResults = Arithmetic.quantizeRefValue(
                latitude, Metadata.LATITUDE_TOL, Metadata.LOCATION_IS_TWO_SIDED);
        final double quantizedLatitude = quantizedLatResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double latOffset = quantizedLatResults.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] latOffsetBytes = offsetValueToFourBytes(latOffset);

        // Quantize longitude
        Map<String, Double> quantizedLongResults = Arithmetic.quantizeRefValue(
                longitude, Metadata.LONGITUDE_TOL, Metadata.LOCATION_IS_TWO_SIDED);
        final double quantizedLongitude = quantizedLongResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double longOffset = quantizedLongResults.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] longOffsetBytes = offsetValueToFourBytes(longOffset);

        // Quantize accelerometer values
        Map<String, Double> quantizedAccelXResults = Arithmetic.quantizeRefValue(
                accelX, Metadata.X_AXIS_TOL, Metadata.ACCEL_IS_TWO_SIDED);
        final double quantizedAccelX = quantizedAccelXResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double accelXOffset = quantizedAccelXResults.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] accelXOffsetBytes = offsetValueToFourBytes(accelXOffset);

        Map<String, Double> quantizedAccelYResults = Arithmetic.quantizeRefValue(
                accelY, Metadata.Y_AXIS_TOL, Metadata.ACCEL_IS_TWO_SIDED);
        final double quantizedAccelY = quantizedAccelYResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double accelYOffset = quantizedAccelYResults.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] accelYOffsetBytes = offsetValueToFourBytes(accelYOffset);

        Map<String, Double> quantizedAccelZResults = Arithmetic.quantizeRefValue(
                accelZ, Metadata.Z_AXIS_TOL, Metadata.ACCEL_IS_TWO_SIDED);
        final double quantizedAccelZ = quantizedAccelZResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double accelZOffset = quantizedAccelZResults.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] accelZOffsetBytes = offsetValueToFourBytes(accelZOffset);

        final byte[][] metadataOffsets = new byte[][]{
                timeOffsetBytes, latOffsetBytes, longOffsetBytes,
                accelXOffsetBytes, accelYOffsetBytes, accelZOffsetBytes
        };

        // Compute metadata fingerprint
        byte[] metadataFingerprint = Crypto.computeMetadataFingerprint(quantizedLatitude, quantizedLongitude,
                quantizedAccelX, quantizedAccelY, quantizedAccelZ, quantizedTime);

        // Compute payment token
        byte[] paymentToken = Crypto.computePaymentToken(uidBytes, hashedPw, paBytes, metadataFingerprint);

        // Generate random key for encrypting payment token
        SecretKey key1 = Crypto.generateAesKey();
        // Concat payment token, pa, uid and encrypt the resulting payload with AES-GCM, IV is prepended to the ciphertext
        byte[] encryptedQrPayload = Crypto.encryptAesGcm(key1, ArrayUtils.concatByteArrays(paymentToken, paBytes, uidBytes));

        // Generate another random key for encrypting the keyForPaymentToken
        SecretKey key2 = Crypto.generateAesKey();
        // Encrypt keyToken with AES-GCM, IV is prepended to the ciphertext
        byte[] encryptedKeyForQrPayload = Crypto.encryptAesGcm(key2, key1.getEncoded());

        // compute CRC16 checksum for key 2
        byte[] key2Bytes = key2.getEncoded();
        byte[] key2Crc16Checksum = CRC.computeCrc16(key2Bytes);
        // concat key2 with its checksum
        byte[] key2AndCRC = ArrayUtils.concatByteArrays(key2Bytes, key2Crc16Checksum);

        // Lock key2AndCRC with fuzzy vault; the key itself is encoded as coefficients
        int[] coefficients = Format.twoBytesToDecimals(key2AndCRC);
        // Generate genuine points (X) from metadata fingerprint
        int[] genuinePointsX = Format.twoBytesToDecimals(metadataFingerprint);
        int[][] genuineSet = FuzzyVault.generateGenuineSet(coefficients, genuinePointsX);
        int[][] chaffSet = FuzzyVault.generateChaffSetV3(coefficients, genuinePointsX, FuzzyVault.CHAFF_SET_SIZE);
        int[][] fuzzyVault = FuzzyVault.generateFuzzyVault(genuineSet, chaffSet);
        byte[] fuzzyVaultBytes = FuzzyVault.flattenFuzzyVaultToBytes(fuzzyVault);

        byte[] payloadSet1 = finalizePayloadSet1(fuzzyVaultBytes, encryptedQrPayload);
        byte[] payloadSet2 = finalizePayloadSet2(encryptedKeyForQrPayload, metadataOffsets);

        return Map.of(
                PAYLOAD_SET_1_KEY, payloadSet1,
                PAYLOAD_SET_2_KEY, payloadSet2
        );
    }

    /*
    * Should return payer's payment token,
    * uid, payment amount, and payee's metadata fingerprint
    * */
    public static Map<String, byte[]> recoverSecretsFromPayloads(@NonNull final byte[] payloadSet1,
                                                                 @NonNull final byte[] payloadSet2,
                                                                 @NonNull final Long timestamp,
                                                                 @NonNull final Double latitude,
                                                                 @NonNull final Double longitude,
                                                                 @NonNull final Float accelX,
                                                                 @NonNull final Float accelY,
                                                                 @NonNull final Float accelZ) throws Exception {

        Map<String, byte[]> unpackedPayloadSet2 = unpackPayloadSet2(payloadSet2);
        byte[] encryptedKeyForQrPayload = unpackedPayloadSet2.get(ENC_KEY_QR_PAYLOAD_KEY);
        byte[] timeOffsetBytes = unpackedPayloadSet2.get(TIME_OFFSET_KEY);
        byte[] latitudeOffsetBytes = unpackedPayloadSet2.get(LATITUDE_OFFSET_KEY);
        byte[] longitudeOffsetBytes = unpackedPayloadSet2.get(LONGITUDE_OFFSET_KEY);
        byte[] accelXOffsetBytes = unpackedPayloadSet2.get(ACCEL_X_OFFSET_KEY);
        byte[] accelYOffsetBytes = unpackedPayloadSet2.get(ACCEL_Y_OFFSET_KEY);
        byte[] accelZOffsetBytes = unpackedPayloadSet2.get(ACCEL_Z_OFFSET_KEY);

        double timeOffset = fourBytesToOffsetValue(timeOffsetBytes);
        double latOffset = fourBytesToOffsetValue(latitudeOffsetBytes);
        double longOffset = fourBytesToOffsetValue(longitudeOffsetBytes);
        double accelXOffset = fourBytesToOffsetValue(accelXOffsetBytes);
        double accelYOffset = fourBytesToOffsetValue(accelYOffsetBytes);
        double accelZOffset = fourBytesToOffsetValue(accelZOffsetBytes);

        long quantizedTime = Arithmetic.quantizeOtherVal(
                timestamp, Metadata.TIME_MS_TOL, Metadata.TIME_IS_TWO_SIDED, timeOffset);
        double quantizedLatitude = Arithmetic.quantizeOtherVal(
                latitude, Metadata.LATITUDE_TOL, Metadata.LOCATION_IS_TWO_SIDED, latOffset);
        double quantizedLongitude = Arithmetic.quantizeOtherVal(
                longitude, Metadata.LONGITUDE_TOL, Metadata.LOCATION_IS_TWO_SIDED, longOffset);
        double quantizedAccelX = Arithmetic.quantizeOtherVal(
                accelX, Metadata.X_AXIS_TOL, Metadata.ACCEL_IS_TWO_SIDED, accelXOffset);
        double quantizedAccelY = Arithmetic.quantizeOtherVal(
                accelY, Metadata.Y_AXIS_TOL, Metadata.ACCEL_IS_TWO_SIDED, accelYOffset);
        double quantizedAccelZ = Arithmetic.quantizeOtherVal(
                accelZ, Metadata.Z_AXIS_TOL, Metadata.ACCEL_IS_TWO_SIDED, accelZOffset);

        // Compute metadata fingerprint
        byte[] metadataFingerprint = Crypto.computeMetadataFingerprint(
                quantizedLatitude, quantizedLongitude, quantizedAccelX,
                quantizedAccelY, quantizedAccelZ, quantizedTime);

        Map<String, byte[]> unpackedPayloadSet1 = unpackPayloadSet1(payloadSet1);
        byte[] fvBytes = unpackedPayloadSet1.get(FV_KEY);
        byte[] encryptedQrPayload = unpackedPayloadSet1.get(ENC_QR_PAYLOAD);

        int[][] fuzzyVault = FuzzyVault.fuzzyVaultBytesToDecimalPairs(fvBytes);
        int[] genuinePointsX = Format.twoBytesToDecimals(metadataFingerprint);
        int[][] matchedGenuinePoints = FuzzyVault.matchGenuinePointsFromFV(fuzzyVault, genuinePointsX, FuzzyVault.TOTAL_COEFFICIENT_SIZE);
        int[] reconstructedCoefficients = FuzzyVault.reconstructPolynomialCoefficients(matchedGenuinePoints);
        byte[] key2AndCrc = Format.decimalsToTwoBytes(reconstructedCoefficients);
        byte[] key2 = new byte[key2AndCrc.length - 2];
        System.arraycopy(key2AndCrc, 0, key2, 0, key2.length);
        byte[] crc = new byte[2];
        System.arraycopy(key2AndCrc, key2AndCrc.length - 2, crc, 0, 2);
        if(!verifyKey2AndCrc(key2, crc)){
            throw new PayloadException("Key2 reconstruction failed - CRC mismatch.");
        }
        SecretKey reconstructedKey2 = Crypto.secretKeyFromBytes(key2);
        byte[] decryptedQrPayloadKeyBytes = Crypto.decryptAesGcm(reconstructedKey2, encryptedKeyForQrPayload);
        SecretKey decryptedQrPayloadKey =  Crypto.secretKeyFromBytes(decryptedQrPayloadKeyBytes);
        byte[] decryptedQrPayload = Crypto.decryptAesGcm(decryptedQrPayloadKey, encryptedQrPayload);
        // | payment token | pa | uid |
        byte[] decryptedPaymentToken = new byte[PAYMENT_TOKEN_BYTE_SIZE];
        byte[] paBytes = new byte[PA_BYTE_SIZE];
        byte[] uidBytes = new byte[decryptedQrPayload.length - PAYMENT_TOKEN_BYTE_SIZE - PA_BYTE_SIZE];
        System.arraycopy(decryptedQrPayload, 0, decryptedPaymentToken, 0, PAYMENT_TOKEN_BYTE_SIZE);
        System.arraycopy(decryptedQrPayload, PAYMENT_TOKEN_BYTE_SIZE, paBytes, 0, PA_BYTE_SIZE);
        System.arraycopy(decryptedQrPayload, PAYMENT_TOKEN_BYTE_SIZE + PA_BYTE_SIZE, uidBytes, 0, uidBytes.length);

        return Map.of(UID_KEY, uidBytes,
                PA_KEY, paBytes,
                DEC_PAY_TOKEN_KEY, decryptedPaymentToken,
                MF_KEY, metadataFingerprint);
    }

    public static boolean verifyKey2AndCrc(@NonNull byte[] key2, @NonNull byte[] crc) throws PayloadException {
        if (key2.length != Crypto.AES_KEY_SIZE_BYTES) {
            throw new PayloadException("Key 2 must be of length " + Crypto.AES_KEY_SIZE_BYTES + " but is of length " + key2.length + ".");
        }
        if (crc.length != 2){
            throw new PayloadException("CRC must be of length 2 but is of length " + crc.length + ".");
        }
        byte[] recomputedChecksum = CRC.computeCrc16(key2);
        return Arrays.equals(recomputedChecksum, crc);
    }

    public static boolean verifyKey2AndCrc(@NonNull byte[] key2AndCrc) throws PayloadException {
        byte[] key2 = new byte[key2AndCrc.length - 2];
        System.arraycopy(key2AndCrc, 0, key2, 0, key2.length);
        byte[] crc = new byte[2];
        System.arraycopy(key2AndCrc, key2AndCrc.length - 2, crc, 0, 2);
        return verifyKey2AndCrc(key2, crc);
    }

    /*
    * Deprecated methods below
    * */
    @Deprecated
    public static byte[] offsetValueToTwoBytes(double offset){
        boolean positive = offset >= 0;
        String val = String.format("%.4f", offset);
        val = positive ? val.substring(2) : val.substring(3);
        int value = Integer.parseInt(val);
        byte[] result = Format.oneDecimalToTwoBytes(value);
        if(positive) result[0] = (byte)(result[0] | 0x80);  // add sign bit to the most significant bit
        return result;
    }

    @Deprecated
    public static double twoBytesToOffsetValue(byte[] offsetBytes){
        if (offsetBytes.length != 2) {
            throw new IllegalArgumentException("Input array must have a length of 2");
        }
        int sign = (offsetBytes[0] & 0xFF) >> 7;
        boolean positive = sign == 1;
        if (positive) offsetBytes[0] = (byte)(offsetBytes[0] & 0x7F);   // remove the sign bit
        int value = Format.twoBytesToDecimals(offsetBytes)[0];
        // apply 0 padding:
        int valLen = String.valueOf(value).length();
        StringBuilder zeros = new StringBuilder("0.");
        for (int i = 0; i < 4 - valLen; i++){
            zeros.append("0");
        }
        String result = positive ? zeros.append(value).toString() : "-" + zeros.append(value);
        return Double.parseDouble(result);
    }

    private Payload(){

    }
}
