package com.khchan744.smart_qr_pay.bgservice;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.quietmodem.Quiet.FrameTransmitter;
import org.quietmodem.Quiet.FrameTransmitterConfig;
import org.quietmodem.Quiet.ModemException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuietTransmitter {

    public static final String TRANSMITTER_PROFILE = "near-ultrasonic-long-range";
    private final int transmissionDelayMs;

    private final Context context;
    private FrameTransmitter frameTransmitter;
    private ExecutorService executorService;
    private final AtomicBoolean isTransmitting = new AtomicBoolean(false);

    public QuietTransmitter(@NonNull Context context, @NonNull Integer transmissionDelayMs) {
        this.context = context.getApplicationContext();
        this.transmissionDelayMs = transmissionDelayMs;
    }

    public boolean isTransmitting() {
        return isTransmitting.get();
    }

    public void start(@NonNull byte[] payload, @NonNull Runnable onStart, @NonNull Runnable onFailure) {
        if (isTransmitting.getAndSet(true)) {
            return; // Already transmitting
        }

        try {
            FrameTransmitterConfig transmitterConfig = new FrameTransmitterConfig(context, TRANSMITTER_PROFILE);
            frameTransmitter = new FrameTransmitter(transmitterConfig);
        } catch (IOException | ModemException e) {
            isTransmitting.set(false);
            onFailure.run();
            return;
        }

        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        onStart.run();

        executorService.submit(() -> {
            while (isTransmitting.get()) {
                try {
                    frameTransmitter.send(payload);
                    Thread.sleep(transmissionDelayMs);
                } catch (IOException e) {
                    // Can happen if transmitter is closed, stop the loop
                    break;
                } catch (InterruptedException e) {
                    // Thread was interrupted, stop the loop
                    Thread.currentThread().interrupt(); // Preserve interrupted status
                    break;
                }
            }
            // Clean up after loop finishes
            stopInternal();
        });
    }

    public void stop() {
        if (!isTransmitting.getAndSet(false)) {
            return; // Not transmitting
        }
        stopInternal();
    }

    private void stopInternal() {
        if (executorService != null) {
            executorService.shutdownNow(); // Interrupts the running task
            executorService = null;
        }
        if (frameTransmitter != null) {
            try {
                frameTransmitter.close();
            } catch (Exception ignored) {
            }
            frameTransmitter = null;
        }
    }
}
