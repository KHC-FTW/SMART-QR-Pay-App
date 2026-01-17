package com.khchan744.smart_qr_pay.bgservicetest;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

import com.khchan744.smart_qr_pay.activity.MakePaymentActivity;
import com.khchan744.smart_qr_pay.bgservice.Crypto;
import com.khchan744.smart_qr_pay.bgservice.Metadata;
import com.khchan744.smart_qr_pay.bgservice.Payload;
import com.khchan744.smart_qr_pay.param.GlobalConst;

@RunWith(AndroidJUnit4.class)
public class PayloadInstrumentedTest{

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
        final byte[] p1 = payloads.get(Payload.PAYLOAD_SET_1_KEY);
        final byte[] p2 = payloads.get(Payload.PAYLOAD_SET_2_KEY);
        assertNotNull(p1);
        assertNotNull(p2);
        Log.d(TAG, "Payload Set 1 size: " + p1.length);
        Log.d(TAG, "Payload Set 2 size: " + p2.length);
    }


    @Test
    public void timeFromMetadataToPayloads(){
        // Goal: measure wall-clock time from calling Metadata.startCollection() until
        // Payload.rawInputsToPayloads() completes, on a real device.
        //
        // NOTE: Metadata.startCollection() uses callbacks; to make this test deterministic we:
        //  - run the loop sequentially (N iterations)
        //  - for each iteration, wait for onMetadataCollected/onMetadataCollectionFailure using a latch
        //  - retry when metadata isn't ready yet

        final int trialCnt = 100;
        final List<Long> durationsMs = new ArrayList<>(trialCnt);

        // Important: start a real Activity once. Metadata.startCollection() requires an Activity
        // instance (for permission checks). Re-creating activities inside a tight loop can be slow
        // and can itself skew the measurement.
        final Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), MakePaymentActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);

        // getInstance() expects a Context; use the Activity's application context.
        final Metadata metadata = Metadata.getInstance(activity.getApplicationContext());

        // Ensure background updates are running so location/accelerometer become available.
        metadata.startUpdates();

        // -----------------------------------------------------------------------------
        // OLD (incorrect) approach kept for reference:
        //  - It used while(remaining > 0) and decremented remaining inside the callback.
        //  - That doesn't block the loop, so it will enqueue many startCollection() calls
        //    quickly and race with callbacks.
        // -----------------------------------------------------------------------------
        // final int cnt = 100;
        // List<Long> durations = new ArrayList<>(cnt);
        // int remaining = cnt;
        // while(remaining > 0){
        //     long startTime = System.currentTimeMillis();
        //     metadata.startCollection(activity, new Metadata.MetadataCallback() { ... remaining--; });
        // }

        final int maxAttemptsPerIteration = 20;   // prevents infinite loop if metadata never becomes ready
        final long attemptTimeoutMs = 3000;       // wait up to 3s for callback per attempt
        final long retryBackoffMs = 150;          // short backoff between "not ready" retries

        for (int i = 0; i < trialCnt; i++) {
            Long durationForThisIteration = null;

            for (int attempt = 1; attempt <= maxAttemptsPerIteration; attempt++) {
                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<String> failureMsg = new AtomicReference<>(null);
                final AtomicReference<Long> durationRef = new AtomicReference<>(null);

                final long startTimeMs = System.currentTimeMillis();

                // Do the collection. When it returns successfully, run rawInputsToPayloads() and
                // measure the end-to-end time.
                metadata.startCollection(activity, new Metadata.MetadataCallback() {
                    @Override
                    public void onMetadataCollected(long timeMs, Location location, float[] accelerometer) {
                        try {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            float accelX = accelerometer[0];
                            float accelY = accelerometer[1];
                            float accelZ = accelerometer[2];

                            // This is the method-under-test endpoint for timing.
                            Map<String, byte[]> payloads = Payload.rawInputsToPayloads(
                                    "testUser123",
                                    GlobalConst.TEST_SECRET,
                                    "123", "4",
                                    timeMs,
                                    latitude, longitude,
                                    accelX, accelY, accelZ
                            );

                            // Basic sanity checks so the optimizer doesn't remove work and we catch regressions.
                            if (payloads.get(Payload.PAYLOAD_SET_1_KEY) == null
                                    || payloads.get(Payload.PAYLOAD_SET_2_KEY) == null) {
                                failureMsg.set("Payload.rawInputsToPayloads returned null payloads");
                            } else {
                                long endTimeMs = System.currentTimeMillis();
                                durationRef.set(endTimeMs - startTimeMs);
                            }
                        } catch (Exception e) {
                            failureMsg.set("Exception in onMetadataCollected: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onMetadataCollectionFailure(String errorMessage) {
                        // Common case at the beginning: location/accelerometer not ready yet.
                        failureMsg.set(errorMessage);
                        latch.countDown();
                    }
                });

                boolean callbackArrived;
                try {
                    callbackArrived = latch.await(attemptTimeoutMs, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Interrupted while waiting for metadata callback");
                    return;
                }

                if (!callbackArrived) {
                    // Callback didn't arrive in time (device busy, GPS cold start, etc.). Retry.
                    failureMsg.set("Timeout waiting for metadata callback");
                }

                Long maybeDuration = durationRef.get();
                if (maybeDuration != null) {
                    durationForThisIteration = maybeDuration;
                    break;
                }

                // If we reached here, we failed this attempt.
                // In early attempts, "Metadata not available yet" is expected; we'll retry.
                // After max attempts, we fail the test with a useful message.
                if (attempt == maxAttemptsPerIteration) {
                    fail("Iteration " + (i + 1) + " failed after " + maxAttemptsPerIteration
                            + " attempts. Last error: " + failureMsg.get());
                }

                try {
                    Thread.sleep(retryBackoffMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Interrupted during retry backoff");
                    return;
                }
            }

            // Store result for statistics.
            durationsMs.add(durationForThisIteration);
            Log.d(TAG, "Iteration " + (i + 1) + "/" + trialCnt + " duration: " + durationForThisIteration + " ms");
        }

        // Compute and log summary stats: max, avg, stddev.
        assertEquals("Expected duration list size to equal iteration count", trialCnt, durationsMs.size());

        long max = Long.MIN_VALUE;
        double sum = 0.0;
        for (long d : durationsMs) {
            if (d > max) max = d;
            sum += d;
        }
        double avg = sum / durationsMs.size();

        double varianceSum = 0.0;
        for (long d : durationsMs) {
            double diff = d - avg;
            varianceSum += diff * diff;
        }
        double stdDev = Math.sqrt(varianceSum / durationsMs.size());

        Log.d(TAG, "==== timeFromMetadataToPayloads stats (ms) ====");
        Log.d(TAG, "Max: " + max);
        Log.d(TAG, "Avg: " + String.format(java.util.Locale.US, "%.3f", avg));
        Log.d(TAG, "StdDev: " + String.format(java.util.Locale.US, "%.3f", stdDev));

        // Cleanup to reduce background work on the device after tests.
        metadata.stopUpdates();
        activity.finish();
    }


}
