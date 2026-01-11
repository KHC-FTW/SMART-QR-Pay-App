package com.khchan744.smart_qr_pay.activity;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.bgservice.Metadata;
import com.khchan744.smart_qr_pay.bgservice.QrGen;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TransferMetadataActivity extends AppCompatActivity implements Metadata.MetadataCallback {

    private static final int METADATA_COLLECTION_INTERVAL_MS = 5000;

    private ImageView ivQrCode;
    private Metadata metadata;
    private ScheduledExecutorService metadataScheduler;
    private final Object schedulerLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transfer_metadata);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        metadata = Metadata.getInstance(this);
        ivQrCode = findViewById(R.id.ivQrCode);

        findViewById(R.id.btnBack).setOnClickListener(v -> {stopMetadataCollection(); finish();});
        findViewById(R.id.btnBackToMenu).setOnClickListener(v -> {stopMetadataCollection(); finish();});

        startMetadataCollection();
    }

    @Override
    public void onMetadataCollected(long timeMs, Location location, float[] accelerometer) {
        runOnUiThread(()->{
            String timestamp = String.format("%d", timeMs);
            String latitude = String.format("%.10f", location.getLatitude());
            String longitude = String.format("%.10f", location.getLongitude());
            String accelX = String.format("%.10f", accelerometer[0]);
            String accelY = String.format("%.10f", accelerometer[1]);
            String accelZ = String.format("%.10f", accelerometer[2]);

            String concat = timestamp + "_" + latitude + "_" + longitude + "_" + accelX + "_" + accelY + "_" + accelZ;
            // Generate and display new QR code
            Bitmap qrBitmap = QrGen.generateQrCodeFromBytes(concat.getBytes(StandardCharsets.UTF_8));
            ivQrCode.setImageBitmap(qrBitmap);
        });
    }

    @Override
    public void onMetadataCollectionFailure(String errorMessage) {
        // should ignore
        runOnUiThread(()->Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        metadata.startUpdates();
        startMetadataCollection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopMetadataCollection();
        metadata.stopUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMetadataCollection();
        metadata.stopUpdates();
    }

    private void startMetadataCollection() {
        synchronized (schedulerLock) {
            if (metadataScheduler == null || metadataScheduler.isShutdown()) {
                metadataScheduler = Executors.newSingleThreadScheduledExecutor();
                metadataScheduler.scheduleWithFixedDelay(
                        () -> metadata.startCollection(this, this),
                        0,
                        METADATA_COLLECTION_INTERVAL_MS,
                        TimeUnit.MILLISECONDS
                );
            }
        }
    }

    private void stopMetadataCollection() {
        synchronized (schedulerLock) {
            if (metadataScheduler != null && !metadataScheduler.isShutdown()) {
                metadataScheduler.shutdownNow();
                metadataScheduler = null;
            }
        }
    }
}