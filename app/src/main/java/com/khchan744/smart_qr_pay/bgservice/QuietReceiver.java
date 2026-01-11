package com.khchan744.smart_qr_pay.bgservice;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.quietmodem.Quiet.FrameReceiver;
import org.quietmodem.Quiet.FrameReceiverConfig;
import org.quietmodem.Quiet.ModemException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuietReceiver {

    public interface ReceiverCallback {
        void onReceive(@NonNull byte[] payload);
        void onFail(@NonNull String errorMessage);
        // void onPermissionDenied();
    }

    public static final String RECEIVER_PROFILE = "near-ultrasonic-long-range";
    public static final int REQ_RECORD_AUDIO = 2002;

    final int bufferSize;

    private final Activity activity;
    private final ReceiverCallback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FrameReceiver receiver;
    private ExecutorService executorService;
    private final AtomicBoolean isReceiving = new AtomicBoolean(false);

    public QuietReceiver(@NonNull Activity activity, @NonNull ReceiverCallback callback, @NonNull Integer bufferSize) {
        this.activity = activity;
        this.callback = callback;
        this.bufferSize = bufferSize;
    }

    public boolean isReceiving() {
        return isReceiving.get();
    }

    public void start() {
        if (!hasRecordAudioPermission()) {
            requestPermission();
            return;
        }

        if (isReceiving.getAndSet(true)) {
            return; // Already receiving
        }

        try {
            FrameReceiverConfig receiverConfig = new FrameReceiverConfig(activity, RECEIVER_PROFILE);
            receiver = new FrameReceiver(receiverConfig);
            receiver.setBlocking(0, 0);
        } catch (IOException | ModemException e) {
            isReceiving.set(false);
            mainHandler.post(() -> callback.onFail("Failed to initialize receiver: " + e.getMessage()));
            return;
        }

        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        executorService.submit(this::receiveLoop);
    }

    public void stop() {
        if (!isReceiving.getAndSet(false)) {
            return; // Not receiving
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        if (receiver != null) {
            try {
                receiver.close();
            } catch (Exception ignored) {
            }
            receiver = null;
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[bufferSize];
        while (isReceiving.get()) {
            try {
                long bytesRead = receiver.receive(buffer);
                if (bytesRead > 0) {
                    final byte[] payload = new byte[(int) bytesRead];
                    System.arraycopy(buffer, 0, payload, 0, (int) bytesRead);
                    mainHandler.post(() -> callback.onReceive(payload));
                    // Stop after one successful reception
                    // mainHandler.post(this::stop);
                    // return; // Exit loop
                }
            } catch (IOException e) {
                // This is expected when receiver is closed by stop()
                if (isReceiving.get()) {
                    mainHandler.post(() -> callback.onFail("Receive failed: " + e.getMessage()));
                }
                break; // Exit loop on error
            } catch (Exception e) {
                if (isReceiving.get()) {
                    mainHandler.post(() -> callback.onFail("Unexpected error: " + e.getMessage()));
                }
                break; // Exit loop on error
            }
        }
        // Ensure state is consistent after loop exits
        isReceiving.set(false);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                start();
            } else {
                callback.onFail("Record audio permission denied, but this is required to receive data.");
                // callback.onPermissionDenied();
            }
        }
    }

    private boolean hasRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
    }
}
