package com.khchan744.smart_qr_pay.bgservicetest;

import com.google.android.gms.common.util.ArrayUtils;
import com.khchan744.smart_qr_pay.bgservice.Arithmetic;
import com.khchan744.smart_qr_pay.bgservice.CRC;
import com.khchan744.smart_qr_pay.bgservice.Crypto;
import com.khchan744.smart_qr_pay.bgservice.Format;
import com.khchan744.smart_qr_pay.bgservice.FuzzyVault;
import com.khchan744.smart_qr_pay.bgservice.Metadata;
import com.khchan744.smart_qr_pay.bgservice.Payload;
import com.khchan744.smart_qr_pay.param.GlobalConst;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.SecretKey;

public class IntegrationTest {

    @Test
    public void integrationTest1() throws Exception {

        long startTime = System.currentTimeMillis();

        // Login
        final String uid = "khchan744";
        final byte[] uidBytes = uid.getBytes(StandardCharsets.UTF_8);
        final String pw = "aSfnkj_sad612!&";
        final byte[] hashedPw = Crypto.sha256Hash(pw.getBytes(StandardCharsets.UTF_8));

        // Enter payment amount
        final String paymentInteger = "183";
        final String paymentFraction = "8";
        final byte[] paBytes = Payload.paymentAmountToBytes(paymentInteger, paymentFraction);

        // Collect metadata
        final long timestamp = 1766397561789L; final int timeTol = 10000;
        final double latitude = 22.2780783; final double latTol = 0.00005;
        final double longitude = 114.2261503; final double longTol = 0.00003;
        final float accelX = 0.7636095f; final float accelXTol = 0.3f;
        final float accelY = 4.9725847f; final float accelYTol = 0.2f;
        final float accelZ = 7.579864f; final float accelZTol = 0.05f;

        // Quantize timestamp
        Map<String, Object> quantizedResult1 = Arithmetic.quantizeRefValue(timestamp, timeTol, false);
        final long quantizedTime = (long)quantizedResult1.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double timeOffset = (double)quantizedResult1.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] timeOffsetBytes = Payload.offsetValueToFourBytes(timeOffset);

        // Quantize latitude
        Map<String, Double> quantizedResult2 = Arithmetic.quantizeRefValue(latitude, latTol, true);
        final double quantizedLatitude = quantizedResult2.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double latOffset = quantizedResult2.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] latOffsetBytes = Payload.offsetValueToFourBytes(latOffset);

        // Quantize longitude
        Map<String, Double> quantizedResult3 = Arithmetic.quantizeRefValue(longitude, longTol, true);
        final double quantizedLongitude = quantizedResult3.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double longOffset = quantizedResult3.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] longOffsetBytes = Payload.offsetValueToFourBytes(longOffset);

        // Quantize accelerometer values
        Map<String, Double> quantizedResult4 = Arithmetic.quantizeRefValue(accelX, accelXTol, true);
        final double quantizedAccelX = quantizedResult4.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double accelXOffset = quantizedResult4.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] accelXOffsetBytes = Payload.offsetValueToFourBytes(accelXOffset);

        Map<String, Double> quantizedResult5 = Arithmetic.quantizeRefValue(accelY, accelYTol, true);
        final double quantizedAccelY = quantizedResult5.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double accelYOffset = quantizedResult5.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] accelYOffsetBytes = Payload.offsetValueToFourBytes(accelYOffset);

        Map<String, Double> quantizedResult6 = Arithmetic.quantizeRefValue(accelZ, accelZTol, true);
        final double quantizedAccelZ = quantizedResult6.get(Arithmetic.QUANTIZED_VAL_KEY);
        final double accelZOffset = quantizedResult6.get(Arithmetic.OFFSET_VAL_KEY);
        final byte[] accelZOffsetBytes = Payload.offsetValueToFourBytes(accelZOffset);

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
        SecretKey keyEncQrCode = Crypto.generateAesKey();
        // Concat payment token, pa, uid and encrypt the resulting payload with AES-GCM, IV is prepended to the ciphertext
        byte[] encryptedQrPayload = Crypto.encryptAesGcm(keyEncQrCode, ArrayUtils.concatByteArrays(paymentToken, paBytes, uidBytes));

        // Generate another random key for encrypting the keyToken
        SecretKey keyEnc = Crypto.generateAesKey();
        // Encrypt keyToken with AES-GCM, IV is prepended to the ciphertext
        byte[] encryptedKeyQrCode = Crypto.encryptAesGcm(keyEnc, keyEncQrCode.getEncoded());

        // compute CRC16 checksum for keyEnc
        byte[] keyEncBytes = keyEnc.getEncoded();
        byte[] keyEncCrc16Checksum = CRC.computeCrc16(keyEncBytes);
        // concat keyEnc with its checksum
        byte[] kenEncAndChecksum = ArrayUtils.concatByteArrays(keyEncBytes, keyEncCrc16Checksum);

        // Lock keyEnc with fuzzy vault; the key itself is encoded as coefficients
        int[] coefficients = Format.twoBytesToDecimals(kenEncAndChecksum);
        // Generate genuine points (X) from metadata fingerprint
        int[] genuinePointsX = Format.twoBytesToDecimals(metadataFingerprint);
        int[][] genuineSet = FuzzyVault.generateGenuineSet(coefficients, genuinePointsX);
        int[][] chaffSet = FuzzyVault.generateChaffSetV3(coefficients, genuinePointsX, FuzzyVault.CHAFF_SET_SIZE);
        int[][] fuzzyVault = FuzzyVault.generateFuzzyVault(genuineSet, chaffSet);
        byte[] fuzzyVaultBytes = FuzzyVault.flattenFuzzyVaultToBytes(fuzzyVault);

        byte[] payloadSet1 = Payload.finalizePayloadSet1(fuzzyVaultBytes, encryptedQrPayload);
        byte[] payloadSet2 = Payload.finalizePayloadSet2(encryptedKeyQrCode, metadataOffsets);

        long endTime1 = System.currentTimeMillis();

        // Assume payloadSet1 encoded to QR code, scanned, and decoded
        // Assume payloadSet2 transmitted through ultrasonic audio channel and received

        // Collect metadata (recipient)
        final long timestamp2 = timestamp + timeTol / 2;
        final double latitude2 = latitude - latTol / 2;
        final double longitude2 = longitude + longTol / 2;
        final float accelX2 = accelX + accelXTol / 2;
        final float accelY2 = accelY - accelYTol / 2;
        final float accelZ2 = accelZ + accelZTol / 2;

        Map<String, byte[]> unpackedPayloadSet2 = Payload.unpackPayloadSet2(payloadSet2);
        byte[] encryptedKeyForQrPayload = unpackedPayloadSet2.get(Payload.ENC_KEY_QR_PAYLOAD_KEY);
        byte[] timeOffsetBytes2 = unpackedPayloadSet2.get(Payload.TIME_OFFSET_KEY);
        byte[] latitudeBytes2 = unpackedPayloadSet2.get(Payload.LATITUDE_OFFSET_KEY);
        byte[] longitudeBytes2 = unpackedPayloadSet2.get(Payload.LONGITUDE_OFFSET_KEY);
        byte[] accelXBytes2 = unpackedPayloadSet2.get(Payload.ACCEL_X_OFFSET_KEY);
        byte[] accelYBytes2 = unpackedPayloadSet2.get(Payload.ACCEL_Y_OFFSET_KEY);
        byte[] accelZBytes2 = unpackedPayloadSet2.get(Payload.ACCEL_Z_OFFSET_KEY);

        double timeOffset2 = Payload.fourBytesToOffsetValue(timeOffsetBytes2);
        double latOffset2 = Payload.fourBytesToOffsetValue(latitudeBytes2);
        double longOffset2 = Payload.fourBytesToOffsetValue(longitudeBytes2);
        double accelXOffset2 = Payload.fourBytesToOffsetValue(accelXBytes2);
        double accelYOffset2 = Payload.fourBytesToOffsetValue(accelYBytes2);
        double accelZOffset2 = Payload.fourBytesToOffsetValue(accelZBytes2);

        long quantizedTime2 = Arithmetic.quantizeOtherVal(timestamp2, timeTol, false, timeOffset2);
        double quantizedLatitude2 = Arithmetic.quantizeOtherVal(latitude2, latTol, true, latOffset2);
        double quantizedLongitude2 = Arithmetic.quantizeOtherVal(longitude2, longTol, true, longOffset2);
        double quantizedAccelX2 = Arithmetic.quantizeOtherVal(accelX2, accelXTol, true, accelXOffset2);
        double quantizedAccelY2 = Arithmetic.quantizeOtherVal(accelY2, accelYTol, true, accelYOffset2);
        double quantizedAccelZ2 = Arithmetic.quantizeOtherVal(accelZ2, accelZTol, true, accelZOffset2);

        // Compute metadata fingerprint
        byte[] metadataFingerprint2 = Crypto.computeMetadataFingerprint(quantizedLatitude2, quantizedLongitude2,
                quantizedAccelX2, quantizedAccelY2, quantizedAccelZ2, quantizedTime2);

        Map<String, byte[]> unpackedPayloadSet1 = Payload.unpackPayloadSet1(payloadSet1);
        byte[] fvBytes = unpackedPayloadSet1.get(Payload.FV_KEY);
        byte[] ivAndEncryptedQrPayloadBytes = unpackedPayloadSet1.get(Payload.ENC_QR_PAYLOAD);

        int[][] fuzzyVault2 = FuzzyVault.fuzzyVaultBytesToDecimalPairs(fvBytes);
        int[] genuinePointsX2 = Format.twoBytesToDecimals(metadataFingerprint2);
        int[][] matchedGenuinePoints = FuzzyVault.matchGenuinePointsFromFV(fuzzyVault2, genuinePointsX2, 9);
        int[] reconstructedCoefficients = FuzzyVault.reconstructPolynomialCoefficients(matchedGenuinePoints);
        byte[] keyEncAndChecksum = Format.decimalsToTwoBytes(reconstructedCoefficients);
        byte[] keyEncBytes2 = new byte[keyEncAndChecksum.length - 2];
        System.arraycopy(keyEncAndChecksum, 0, keyEncBytes2, 0, keyEncBytes2.length);
        byte[] checksum = new byte[2];
        System.arraycopy(keyEncAndChecksum, keyEncAndChecksum.length - 2, checksum, 0, 2);
        byte[] recomputedChecksum = CRC.computeCrc16(keyEncBytes2);
        Assert.assertArrayEquals(checksum, recomputedChecksum);

        SecretKey reconstructedKeyEnc = Crypto.secretKeyFromBytes(keyEncBytes2);
        byte[] decryptedQrPayloadKeyBytes = Crypto.decryptAesGcm(reconstructedKeyEnc, encryptedKeyForQrPayload);
        SecretKey decryptedQrPayloadKey =  Crypto.secretKeyFromBytes(decryptedQrPayloadKeyBytes);
        byte[] decryptedQrPayload = Crypto.decryptAesGcm(decryptedQrPayloadKey, ivAndEncryptedQrPayloadBytes);
        // | payment token | pa | uid |
        byte[] decryptedPaymentToken = new byte[Payload.PAYMENT_TOKEN_BYTE_SIZE];
        byte[] paBytes2 = new byte[Payload.PA_BYTE_SIZE];
        byte[] uidBytes2 = new byte[decryptedQrPayload.length - Payload.PAYMENT_TOKEN_BYTE_SIZE - Payload.PA_BYTE_SIZE];
        System.arraycopy(decryptedQrPayload, 0, decryptedPaymentToken, 0, Payload.PAYMENT_TOKEN_BYTE_SIZE);
        System.arraycopy(decryptedQrPayload, Payload.PAYMENT_TOKEN_BYTE_SIZE, paBytes2, 0, Payload.PA_BYTE_SIZE);
        System.arraycopy(decryptedQrPayload, Payload.PAYMENT_TOKEN_BYTE_SIZE + Payload.PA_BYTE_SIZE, uidBytes2, 0, uidBytes2.length);

        byte[] recomputedPaymentToken = Crypto.computePaymentToken(uidBytes2, hashedPw, paBytes2, metadataFingerprint2);

        long endTime2 = System.currentTimeMillis();

        System.out.println("Time taken for first half: " + (endTime1 - startTime) + " ms");
        System.out.println("Time taken for second half: " + (endTime2 - endTime1) + " ms");
        System.out.println("Total time taken: " + (endTime2 - startTime) + " ms");

        Assert.assertArrayEquals(decryptedPaymentToken, recomputedPaymentToken);

    }

    @Test
    public void integrationTest2() throws Exception {
        final String paymentInteger = "183";
        final String paymentFraction = "8";
        final long timestamp = 1766397561789L;
        final double latitude = 22.2780783;
        final double longitude = 114.2261503;
        final float accelX = 0.7636095f;
        final float accelY = 4.9725847f;
        final float accelZ = 7.579864f;

        Map<String, byte[]> payloads = Payload.rawInputsToPayloads(
                GlobalConst.TEST_UID,
                GlobalConst.TEST_SECRET,
                paymentInteger,
                paymentFraction,
                timestamp,
                latitude,
                longitude,
                accelX,
                accelY,
                accelZ
        );
        byte[] payloadSet1 = payloads.get(Payload.PAYLOAD_SET_1_KEY);
        byte[] payloadSet2 = payloads.get(Payload.PAYLOAD_SET_2_KEY);

        /*
        *  Transmission takes place; assume successfully received.
        * */

        // Collect metadata (recipient)
        final long timestamp2 = timestamp + Metadata.TIME_MS_TOL / 2;
        final double latitude2 = latitude - Metadata.LATITUDE_TOL / 2;
        final double longitude2 = longitude + Metadata.LONGITUDE_TOL / 2;
        final float accelX2 = accelX + Metadata.X_AXIS_TOL / 2;
        final float accelY2 = accelY - Metadata.Y_AXIS_TOL / 2;
        final float accelZ2 = accelZ + Metadata.Z_AXIS_TOL / 2;

        Map<String, byte[]> recoveredSecrets = Payload.recoverSecretsFromPayloads(
                payloadSet1, payloadSet2, timestamp2, latitude2, longitude2,
                accelX2, accelY2, accelZ2);

        byte[] payerUidBytes = recoveredSecrets.get(Payload.UID_KEY);
        byte[] payerPaymentAmountBytes = recoveredSecrets.get(Payload.PA_KEY);
        byte[] payerPaymentToken = recoveredSecrets.get(Payload.DEC_PAY_TOKEN_KEY);
        byte[] payeeMetadataFingerprint = recoveredSecrets.get(Payload.MF_KEY);
        byte[] recomputedPaymentToken = Crypto.computePaymentToken(
                payerUidBytes, GlobalConst.TEST_SECRET, payerPaymentAmountBytes, payeeMetadataFingerprint);

        Assert.assertArrayEquals(payerPaymentToken, recomputedPaymentToken);
    }

    @Test
    public void integrationTest3() throws Exception {
        final String paymentInteger = "183";
        final String paymentFraction = "8";
        final long timestamp = 1767332926351L;
        final double latitude = 22.2780782;
        final double longitude = 114.2261675;
        final float accelX = -0.078f;
        final float accelY = 0.17895001f;
        final float accelZ = 9.663f;

        Map<String, byte[]> payloads = Payload.rawInputsToPayloads(
                GlobalConst.TEST_UID,
                GlobalConst.TEST_SECRET,
                paymentInteger,
                paymentFraction,
                timestamp,
                latitude,
                longitude,
                accelX,
                accelY,
                accelZ
        );
        byte[] payloadSet1 = payloads.get(Payload.PAYLOAD_SET_1_KEY);
        byte[] payloadSet2 = payloads.get(Payload.PAYLOAD_SET_2_KEY);

        /*
         *  Transmission takes place; assume successfully received.
         * */

        // Collect metadata (recipient)
        final long timestamp2 = timestamp + Metadata.TIME_MS_TOL / 2;
        final double latitude2 = latitude - Metadata.LATITUDE_TOL / 2;
        final double longitude2 = longitude + Metadata.LONGITUDE_TOL / 2;
        final float accelX2 = accelX + Metadata.X_AXIS_TOL / 2;
        final float accelY2 = accelY - Metadata.Y_AXIS_TOL / 2;
        final float accelZ2 = accelZ + Metadata.Z_AXIS_TOL / 2;

        Map<String, byte[]> recoveredSecrets = Payload.recoverSecretsFromPayloads(
                payloadSet1, payloadSet2, timestamp2, latitude2, longitude2,
                accelX2, accelY2, accelZ2);

        byte[] payerUidBytes = recoveredSecrets.get(Payload.UID_KEY);
        byte[] payerPaymentAmountBytes = recoveredSecrets.get(Payload.PA_KEY);
        byte[] payerPaymentToken = recoveredSecrets.get(Payload.DEC_PAY_TOKEN_KEY);
        byte[] payeeMetadataFingerprint = recoveredSecrets.get(Payload.MF_KEY);
        byte[] recomputedPaymentToken = Crypto.computePaymentToken(
                payerUidBytes, GlobalConst.TEST_SECRET, payerPaymentAmountBytes, payeeMetadataFingerprint);

        Assert.assertArrayEquals(payerPaymentToken, recomputedPaymentToken);
    }
}
