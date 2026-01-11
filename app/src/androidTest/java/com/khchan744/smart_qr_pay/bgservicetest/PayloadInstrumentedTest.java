package com.khchan744.smart_qr_pay.bgservicetest;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.*;

import com.khchan744.smart_qr_pay.bgservice.Crypto;
import com.khchan744.smart_qr_pay.bgservice.Payload;

@RunWith(AndroidJUnit4.class)
public class PayloadInstrumentedTest {

    private static final String TAG = "PayloadPerformanceTest";

    @Test
    public void performanceTestRawInputsToPayloads() throws Exception {
        // 1. Prepare sample inputs
        final String uid = "testUser12345";
        final byte[] hashedPw = Crypto.sha256Hash("password123".getBytes(StandardCharsets.UTF_8));
        final String paIntVal = "199";
        final String paFracVal = "9";
        final Long timestamp = System.currentTimeMillis();
        final Double latitude = 34.052235;
        final Double longitude = -118.243683;
        final Float accelX = 0.15f;
        final Float accelY = -0.08f;
        final Float accelZ = 9.81f;

        // 2. Measure execution time
        long startTime = System.nanoTime();

        Map<String, byte[]> payloads = Payload.rawInputsToPayloads(
                uid, hashedPw, paIntVal, paFracVal, timestamp,
                latitude, longitude, accelX, accelY, accelZ
        );

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        // 3. Log the result
        Log.d(TAG, "rawInputsToPayloads execution time: " + durationMs + " ms");

        // 4. Assert that the output is not null
        assertNotNull(payloads);
        assertNotNull(payloads.get(Payload.PAYLOAD_SET_1_KEY));
        assertNotNull(payloads.get(Payload.PAYLOAD_SET_2_KEY));
        Log.d(TAG, "Payload Set 1 size: " + payloads.get(Payload.PAYLOAD_SET_1_KEY).length);
        Log.d(TAG, "Payload Set 2 size: " + payloads.get(Payload.PAYLOAD_SET_2_KEY).length);
    }
}
