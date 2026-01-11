package com.khchan744.smart_qr_pay.activity;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.view.PreviewView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.bgservice.Metadata;
import com.khchan744.smart_qr_pay.bgservice.QrScanner;

import java.nio.charset.StandardCharsets;

/**
 * CompareMetadataActivity
 *
 * Flow:
 *  1) Start in camera scanning mode (full screen under the header)
 *  2) When a valid SmartQrPay QR is scanned, switch to result display tables
 *
 * The detailed parsing/comparison and table population will be implemented later.
 */
@ExperimentalGetImage
public class CompareMetadataActivity extends AppCompatActivity implements QrScanner.QrScannerCallback, Metadata.MetadataCallback {

    private static final long METADATA_COLLECTION_DELAY = 2750L;

    private View cameraContainer;
    private View resultsScrollView;
    private PreviewView previewView;

    /*
    * TextView for each table
    * */
    private TextView tvTimestampValueYou;
    private TextView tvTimestampValueOther;
    private TextView tvTimestampValueDiff;
    private TextView tvTimestampValueTol;

    private TextView tvLatitudeValueYou;
    private TextView tvLatitudeValueOther;
    private TextView tvLatitudeValueDiff;
    private TextView tvLatitudeValueTol;

    private TextView tvLongitudeValueYou;
    private TextView tvLongitudeValueOther;
    private TextView tvLongitudeValueDiff;
    private TextView tvLongitudeValueTol;

    private TextView tvAccelXValueYou;
    private TextView tvAccelXValueOther;
    private TextView tvAccelXValueDiff;
    private TextView tvAccelXValueTol;

    private TextView tvAccelYValueYou;
    private TextView tvAccelYValueOther;
    private TextView tvAccelYValueDiff;
    private TextView tvAccelYValueTol;

    private TextView tvAccelZValueYou;
    private TextView tvAccelZValueOther;
    private TextView tvAccelZValueDiff;
    private TextView tvAccelZValueTol;
    /*
     * TextView for each table
     * */

    private QrScanner qrScanner;
    private Metadata metadata;

    private String cachedOtherTimestamp;
    private String cachedOtherLatitude;
    private String cachedOtherLongitude;
    private String cachedOtherAccelX;
    private String cachedOtherAccelY;
    private String cachedOtherAccelZ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_compare_metadata);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cameraContainer = findViewById(R.id.cameraContainer);
        resultsScrollView = findViewById(R.id.resultsScrollView);
        previewView = findViewById(R.id.previewView);

        setupTextViews();

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (qrScanner != null) qrScanner.stop();
            finish();
        });

        findViewById(R.id.btnScanAgain).setOnClickListener(v -> {
            if (qrScanner != null) startScanningUi();
        });

        metadata = Metadata.getInstance(this);
        qrScanner = new QrScanner(this, previewView, this);

        // Start in scanning mode.
        startScanningUi();
    }

    private void setupTextViews() {
        tvTimestampValueYou = findViewById(R.id.tvTimestampValueYou);
        tvTimestampValueOther = findViewById(R.id.tvTimestampValueOther);
        tvTimestampValueDiff = findViewById(R.id.tvTimestampValueDiff);
        tvTimestampValueTol = findViewById(R.id.tvTimestampValueTol);

        tvLatitudeValueYou = findViewById(R.id.tvLatitudeValueYou);
        tvLatitudeValueOther = findViewById(R.id.tvLatitudeValueOther);
        tvLatitudeValueDiff = findViewById(R.id.tvLatitudeValueDiff);
        tvLatitudeValueTol = findViewById(R.id.tvLatitudeValueTol);

        tvLongitudeValueYou = findViewById(R.id.tvLongitudeValueYou);
        tvLongitudeValueOther = findViewById(R.id.tvLongitudeValueOther);
        tvLongitudeValueDiff = findViewById(R.id.tvLongitudeValueDiff);
        tvLongitudeValueTol = findViewById(R.id.tvLongitudeValueTol);

        tvAccelXValueYou = findViewById(R.id.tvAccelXValueYou);
        tvAccelXValueOther = findViewById(R.id.tvAccelXValueOther);
        tvAccelXValueDiff = findViewById(R.id.tvAccelXValueDiff);
        tvAccelXValueTol = findViewById(R.id.tvAccelXValueTol);

        tvAccelYValueYou = findViewById(R.id.tvAccelYValueYou);
        tvAccelYValueOther = findViewById(R.id.tvAccelYValueOther);
        tvAccelYValueDiff = findViewById(R.id.tvAccelYValueDiff);
        tvAccelYValueTol = findViewById(R.id.tvAccelYValueTol);

        tvAccelZValueYou = findViewById(R.id.tvAccelZValueYou);
        tvAccelZValueOther = findViewById(R.id.tvAccelZValueOther);
        tvAccelZValueDiff = findViewById(R.id.tvAccelZValueDiff);
        tvAccelZValueTol = findViewById(R.id.tvAccelZValueTol);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraContainer.getVisibility() == View.VISIBLE) {
            qrScanner.start();
        }
        if(metadata != null) metadata.startUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (qrScanner != null) qrScanner.stop();
        if(metadata != null) metadata.stopUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrScanner != null) qrScanner.release();
        if(metadata != null) metadata.stopUpdates();
    }

    private void startScanningUi() {
        resultsScrollView.setVisibility(View.GONE);
        cameraContainer.setVisibility(View.VISIBLE);
        qrScanner.start();
    }

    private void showResultsUi() {
        cameraContainer.setVisibility(View.GONE);
        resultsScrollView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onQrScanned(@NonNull Barcode barcode) {
        boolean isInvalidQr = true;
        byte[] qrBytes = barcode.getRawBytes();
        if(qrBytes != null){
            String qrString = new String(qrBytes, StandardCharsets.UTF_8);
            String[] metadataStrings = processQrString(qrString);
            if(metadataStrings != null){
                isInvalidQr = false;
                cachedOtherTimestamp = metadataStrings[0];
                cachedOtherLatitude = metadataStrings[1];
                cachedOtherLongitude = metadataStrings[2];
                cachedOtherAccelX = metadataStrings[3];
                cachedOtherAccelY = metadataStrings[4];
                cachedOtherAccelZ = metadataStrings[5];
                // Delay 2 seconds to make sure user stabilizes the phone
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    qrScanner.stop();
                    metadata.startCollection(this, this);
                }, METADATA_COLLECTION_DELAY);
            }
        }
        if (isInvalidQr) {
            qrScanner.resumeScanning();
        }
    }

    private String[] processQrString(String qrString){
        String[] qrStrings = qrString.split("_");
        return qrStrings.length == 6 ? qrStrings : null;
    }

    @Override
    public void onCameraSetupFailed(@NonNull String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (qrScanner != null) {
            qrScanner.onRequestPermissionsResult(requestCode, grantResults);
        }
        if(metadata != null){
            metadata.onRequestPermissionsResult(requestCode, grantResults, this, this);
        }
    }

    @Override
    public void onMetadataCollected(long timeMs, Location location, float[] accelerometer) {
        String yourTimestamp = String.format("%d", timeMs);
        String yourLatitude = String.format("%.10f", location.getLatitude());
        String yourLongitude = String.format("%.10f", location.getLongitude());
        String yourAccelX = String.format("%.10f", accelerometer[0]);
        String yourAccelY = String.format("%.10f", accelerometer[1]);
        String yourAccelZ = String.format("%.10f", accelerometer[2]);
        tvTimestampValueYou.setText(yourTimestamp);
        tvLatitudeValueYou.setText(yourLatitude);
        tvLongitudeValueYou.setText(yourLongitude);
        tvAccelXValueYou.setText(yourAccelX);
        tvAccelYValueYou.setText(yourAccelY);
        tvAccelZValueYou.setText(yourAccelZ);

        tvTimestampValueOther.setText(cachedOtherTimestamp);
        tvLatitudeValueOther.setText(cachedOtherLatitude);
        tvLongitudeValueOther.setText(cachedOtherLongitude);
        tvAccelXValueOther.setText(cachedOtherAccelX);
        tvAccelYValueOther.setText(cachedOtherAccelY);
        tvAccelZValueOther.setText(cachedOtherAccelZ);

        String timestampDiff = String.format("%d", Long.parseLong(yourTimestamp) - Long.parseLong(cachedOtherTimestamp));
        String latitudeDiff = String.format("%.10f", Math.abs(Double.parseDouble(yourLatitude) - Double.parseDouble(cachedOtherLatitude)));
        String longitudeDiff = String.format("%.10f", Math.abs(Double.parseDouble(yourLongitude) - Double.parseDouble(cachedOtherLongitude)));
        String accelXDiff = String.format("%.10f", Math.abs(Float.parseFloat(yourAccelX) - Float.parseFloat(cachedOtherAccelX)));
        String accelYDiff = String.format("%.10f", Math.abs(Float.parseFloat(yourAccelY) - Float.parseFloat(cachedOtherAccelY)));
        String accelZDiff = String.format("%.10f", Math.abs(Float.parseFloat(yourAccelZ) - Float.parseFloat(cachedOtherAccelZ)));

        tvTimestampValueDiff.setText(timestampDiff);
        tvLatitudeValueDiff.setText(latitudeDiff);
        tvLongitudeValueDiff.setText(longitudeDiff);
        tvAccelXValueDiff.setText(accelXDiff);
        tvAccelYValueDiff.setText(accelYDiff);
        tvAccelZValueDiff.setText(accelZDiff);

        tvTimestampValueTol.setText(String.format("%d", Metadata.TIME_MS_TOL));
        tvLatitudeValueTol.setText(String.format("%.6f", Metadata.LATITUDE_TOL));
        tvLongitudeValueTol.setText(String.format("%.6f", Metadata.LONGITUDE_TOL));
        tvAccelXValueTol.setText(String.format("%.6f", Metadata.X_AXIS_TOL));
        tvAccelYValueTol.setText(String.format("%.6f", Metadata.Y_AXIS_TOL));
        tvAccelZValueTol.setText(String.format("%.6f", Metadata.Z_AXIS_TOL));

        showResultsUi();
    }


    @Override
    public void onMetadataCollectionFailure(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
}