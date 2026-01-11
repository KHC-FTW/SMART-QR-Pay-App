package com.khchan744.smart_qr_pay.bgservice;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;



@ExperimentalGetImage
public class QrScanner {

    public interface QrScannerCallback {
        void onQrScanned(@NonNull Barcode barcode);
        void onCameraSetupFailed(@NonNull String message);
    }

    private static final int REQ_CAMERA = 2001;

    private final AppCompatActivity activity;
    private final PreviewView previewView;
    private final QrScannerCallback qrScannerCallback;

    private ProcessCameraProvider cameraProvider;
    private BarcodeScanner barcodeScanner;
    private ExecutorService analysisExecutor;
    private final AtomicBoolean hasAcquiredBarcode = new AtomicBoolean(false);

    public QrScanner(@NonNull AppCompatActivity activity,
                     @NonNull PreviewView previewView,
                     @NonNull QrScannerCallback qrScannerCallback) {
        this.activity = activity;
        this.previewView = previewView;
        this.qrScannerCallback = qrScannerCallback;
        init();
    }

    private void init() {
        analysisExecutor = Executors.newSingleThreadExecutor();
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);
    }

    public void start() {
        hasAcquiredBarcode.set(false);
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        } else {
            startCamera();
        }
    }

    public void stop() {
        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception ignored) {
            }
        }
    }

    public void release() {
        stop();
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (analysisExecutor != null) {
            analysisExecutor.shutdownNow();
        }
    }

    public void resumeScanning() {
        hasAcquiredBarcode.set(false);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(activity, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(activity);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindUseCases();
            } catch (Exception e) {
                qrScannerCallback.onCameraSetupFailed("Failed to start camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    private void bindUseCases() {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(analysisExecutor, this::analyzeFrame);

        CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

        cameraProvider.bindToLifecycle(activity, selector, preview, analysis);
    }

    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        if (hasAcquiredBarcode.get() || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), rotationDegrees);

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (hasAcquiredBarcode.get() || barcodes == null || barcodes.isEmpty()) return;

                    for (Barcode barcode : barcodes) {
                        if (barcode != null) {
                            hasAcquiredBarcode.set(true);
                            new Handler(Looper.getMainLooper()).post(() -> qrScannerCallback.onQrScanned(barcode));
                            break; // Process first valid barcode
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Silently ignore and continue scanning
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    // Helper method to check if the detected barcode is within the visible scan area
    // Deprecated due to usability complexity
    @Deprecated
    private boolean isBarcodeInScanArea(Rect barcodeBounds) {
        /*
        * Hardcoded values for now:
        * */
        if (barcodeBounds.top < 100 || barcodeBounds.top > 200) return false;
        if (barcodeBounds.left < 100 || barcodeBounds.left > 140) return false;
        if (barcodeBounds.right < 345 || barcodeBounds.right > 385) return false;
        if (barcodeBounds.bottom < 350 || barcodeBounds.bottom > 450) return false;
        return true;
    }
}
