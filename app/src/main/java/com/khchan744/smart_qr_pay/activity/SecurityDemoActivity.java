package com.khchan744.smart_qr_pay.activity;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.bgservice.Arithmetic;
import com.khchan744.smart_qr_pay.bgservice.Metadata;
import com.khchan744.smart_qr_pay.bgservice.Payload;
import com.khchan744.smart_qr_pay.bgservice.QrGen;
import com.khchan744.smart_qr_pay.param.GlobalConst;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SecurityDemoActivity extends MakePaymentActivity {

    private ConstraintLayout fakeQrConfigContainer;
    private ConstraintLayout layoutShowQr;
    private SwitchCompat switchQrCode;
    private TextView tvQrCodeMode;
    private SwitchCompat switchTimestamp;
    private TextView tvTimestampMode;
    private SwitchCompat switchLatitude;
    private TextView tvLatitudeMode;
    private SwitchCompat switchLongitude;
    private TextView tvLongitudeMode;
    private SwitchCompat switchXAxis;
    private TextView tvXAxisMode;
    private SwitchCompat switchYAxis;
    private TextView tvYAxisMode;
    private SwitchCompat switchZAxis;
    private TextView tvZAxisMode;
    private Button btnGenerateFakeQr;
    private TextView tvScanToReceive;
    private ImageView ivQrCode;

    // --- Cached Metadata ---
    private final Object metadataLock = new Object();
    private long cachedTimeMs;
    private double cachedLatitude;
    private double cachedLongitude;
    private float cachedAccelX;
    private float cachedAccelY;
    private float cachedAccelZ;
    // --- Metadata Collection ---

    private final AtomicBoolean isFirstMetadataCollected = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_demo);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initUI();
    }

    private boolean isGenFakeQrCodeBtnEnabled(){
        return !(switchQrCode.isChecked() && switchTimestamp.isChecked() &&
                switchLatitude.isChecked() && switchLongitude.isChecked() &&
                switchXAxis.isChecked() && switchYAxis.isChecked() && switchZAxis.isChecked());
    }

    @Override
    public void onMetadataCollected(long timeMs, Location location, float[] accelerometer){
        boolean needsUpdate = false;
        synchronized (metadataLock) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            float accelX = accelerometer[0];
            float accelY = accelerometer[1];
            float accelZ = accelerometer[2];
            if (!isFirstMetadataCollected.getAndSet(true)) {
                // First collection, always update
                needsUpdate = true;
            } else {
                // Subsequent collections, compare with cached values
                needsUpdate = switchQrCode.isChecked() && (
                        Arithmetic.hasExceededTolerance(timeMs, cachedTimeMs, Metadata.TIME_MS_TOL)
                        || Arithmetic.hasExceededTolerance(latitude, cachedLatitude, Metadata.LATITUDE_TOL)
                        || Arithmetic.hasExceededTolerance(longitude, cachedLongitude, Metadata.LONGITUDE_TOL)
                        || Arithmetic.hasExceededTolerance(accelX, cachedAccelX, Metadata.X_AXIS_TOL)
                        || Arithmetic.hasExceededTolerance(accelY, cachedAccelY, Metadata.Y_AXIS_TOL)
                        || Arithmetic.hasExceededTolerance(accelZ, cachedAccelZ, Metadata.Z_AXIS_TOL));
            }

            if (needsUpdate) {
                // Overwrite cached values
                this.cachedTimeMs = timeMs;
                this.cachedLatitude = latitude;
                this.cachedLongitude = longitude;
                this.cachedAccelX = accelX;
                this.cachedAccelY = accelY;
                this.cachedAccelZ = accelZ;
            }
        }

        if (needsUpdate) {
            // Trigger re-computation and update of QR/Audio
            runOnUiThread(this::updateQrAndAudio);
        }
    }

    private void updateQrAndAudio(){
        // This method is called on the UI thread when metadata is updated
        synchronized (metadataLock) {
            // Abort ongoing transmission before starting a new one
            if (quietTransmitter.isTransmitting()) {
                quietTransmitter.stop();
            }

            Map<String, byte[]> payloads = null;
            try {
                payloads = Payload.rawInputsToPayloads(
                        userProfile.getUsername(),
                        userProfile.isOnline() ? userProfile.getSecretBytes() : GlobalConst.TEST_SECRET,
                        "100", "0",
                        switchTimestamp.isChecked() ? cachedTimeMs : cachedTimeMs - Metadata.TIME_MS_TOL * 3,
                        switchLatitude.isChecked() ? cachedLatitude : cachedLatitude - Metadata.LATITUDE_TOL * 3,
                        switchLongitude.isChecked() ? cachedLongitude : cachedLongitude - Metadata.LONGITUDE_TOL * 3,
                        switchXAxis.isChecked() ? cachedAccelX : cachedAccelX - Metadata.X_AXIS_TOL * 3,
                        switchYAxis.isChecked() ? cachedAccelY : cachedAccelY - Metadata.Y_AXIS_TOL * 3,
                        switchZAxis.isChecked() ? cachedAccelZ : cachedAccelZ - Metadata.Z_AXIS_TOL * 3
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            byte[] payloadSet1 = payloads.get(Payload.PAYLOAD_SET_1_KEY);
            byte[] payloadSet2 = payloads.get(Payload.PAYLOAD_SET_2_KEY);

            // Generate and display new QR code

            Bitmap qrBitmap = QrGen.generateQrCodeFromBytes(payloadSet1);
            Glide.with(this).clear(ivQrCode);
            ivQrCode.setImageBitmap(qrBitmap);

            // Start new audio transmission
            quietTransmitter.start(
                    payloadSet2,
                    () -> { /* Transmission started */ },
                    () -> runOnUiThread(() -> Toast.makeText(this, "Audio transmission failed to start", Toast.LENGTH_SHORT).show())
            );
        }
    }

    private void initUI(){
        fakeQrConfigContainer = findViewById(R.id.fakeQrConfigContainer);
        layoutShowQr = findViewById(R.id.layoutShowQr);
        switchQrCode = findViewById(R.id.switchQrCode);
        tvQrCodeMode = findViewById(R.id.tvQrCodeMode);
        switchTimestamp = findViewById(R.id.switchTimestamp);
        tvTimestampMode = findViewById(R.id.tvTimestampMode);
        switchLatitude = findViewById(R.id.switchLatitude);
        tvLatitudeMode = findViewById(R.id.tvLatitudeMode);
        switchLongitude = findViewById(R.id.switchLongitude);
        tvLongitudeMode = findViewById(R.id.tvLongitudeMode);
        switchXAxis = findViewById(R.id.switchXAxis);
        tvXAxisMode = findViewById(R.id.tvXAxisMode);
        switchYAxis = findViewById(R.id.switchYAxis);
        tvYAxisMode = findViewById(R.id.tvYAxisMode);
        switchZAxis = findViewById(R.id.switchZAxis);
        tvZAxisMode = findViewById(R.id.tvZAxisMode);
        btnGenerateFakeQr = findViewById(R.id.btnGenerateFakeQr);
        tvScanToReceive = findViewById(R.id.tvScanToReceive);
        ivQrCode = findViewById(R.id.ivQrCode);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnFinish).setOnClickListener(v -> finish());

        switchQrCode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isChecked) {
                tvQrCodeMode.setText(isChecked ? "Dynamic" : "Static");
                btnGenerateFakeQr.setEnabled(isGenFakeQrCodeBtnEnabled());
            }
        });
        switchTimestamp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isChecked) {
                tvTimestampMode.setText(isChecked ? "Real-time" : "Tampered");
                btnGenerateFakeQr.setEnabled(isGenFakeQrCodeBtnEnabled());
            }
        });
        switchLatitude.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isChecked) {
                tvLatitudeMode.setText(isChecked ? "Real-time" : "Tampered");
                btnGenerateFakeQr.setEnabled(isGenFakeQrCodeBtnEnabled());
            }
        });
        switchLongitude.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isChecked) {
                tvLongitudeMode.setText(isChecked ? "Real-time" : "Tampered");
                btnGenerateFakeQr.setEnabled(isGenFakeQrCodeBtnEnabled());
            }
        });
        switchXAxis.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isChecked) {
                tvXAxisMode.setText(isChecked ? "Real-time" : "Tampered");
                btnGenerateFakeQr.setEnabled(isGenFakeQrCodeBtnEnabled());
            }
        });
        switchYAxis.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isChecked) {
                tvYAxisMode.setText(isChecked ? "Real-time" : "Tampered");
                btnGenerateFakeQr.setEnabled(isGenFakeQrCodeBtnEnabled());
            }
        });
        switchZAxis.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isChecked) {
                tvZAxisMode.setText(isChecked ? "Real-time" : "Tampered");
                btnGenerateFakeQr.setEnabled(isGenFakeQrCodeBtnEnabled());
            }
        });
        btnGenerateFakeQr.setOnClickListener(v -> {
            fakeQrConfigContainer.setVisibility(View.GONE);
            layoutShowQr.setVisibility(View.VISIBLE);
            tvScanToReceive.setText("Scan to receive HK$100\nfrom " + userProfile.getUsername());
            Glide.with(this).load(R.drawable.loading).into(ivQrCode);
            new Handler(Looper.getMainLooper())
                    .postDelayed(this::startMetadataCollectionAndTransmission, METADATA_COLLECTION_DELAY);
        });
        findViewById(R.id.btnMakeAnotherPayment).setOnClickListener(v ->{
            stopMetadataCollectionAndTransmission();
            isFirstMetadataCollected.set(false);
            switchQrCode.setChecked(true); switchTimestamp.setChecked(true);
            switchLatitude.setChecked(true); switchLongitude.setChecked(true);
            switchXAxis.setChecked(true); switchYAxis.setChecked(true); switchZAxis.setChecked(true);
            layoutShowQr.setVisibility(View.GONE);
            fakeQrConfigContainer.setVisibility(View.VISIBLE);
            ivQrCode.setImageBitmap(null);
        });
    }
}