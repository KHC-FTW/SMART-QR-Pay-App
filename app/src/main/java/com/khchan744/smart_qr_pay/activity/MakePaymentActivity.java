package com.khchan744.smart_qr_pay.activity;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.bgservice.Arithmetic;
import com.khchan744.smart_qr_pay.bgservice.Metadata;
import com.khchan744.smart_qr_pay.bgservice.Payload;
import com.khchan744.smart_qr_pay.bgservice.QrGen;
import com.khchan744.smart_qr_pay.bgservice.QuietTransmitter;
import com.khchan744.smart_qr_pay.network.NetworkClient;
import com.khchan744.smart_qr_pay.param.UserProfile;
import com.khchan744.smart_qr_pay.param.GlobalConst;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MakePaymentActivity extends AppCompatActivity implements Metadata.MetadataCallback {

    private static final int MAX_TOTAL_CHARS = 6;     // e.g. 9999.9
    private static final int MAX_INT_DIGITS = 4;      // 0 - 9999
    private static final int MAX_DEC_DIGITS = 1;      // 0 - 9
    private static final int METADATA_COLLECTION_INTERVAL_MS = 1000;
    private static final int AUDIO_TRANSMISSION_DELAY_MS = 1800;

    private static final long METADATA_COLLECTION_DELAY = 700L;

    private ConstraintLayout layoutEnterAmount;
    private ConstraintLayout layoutShowQr;

    private TextView tvScanToReceive;
    private TextView tvAmountValue;
    private Button btnGenerateQr;
    private ImageView ivQrCode;

    private Metadata metadata;
    private ScheduledExecutorService metadataScheduler;
    private QuietTransmitter quietTransmitter;

    // --- Cached Metadata ---
    private final Object metadataLock = new Object();
    private long cachedTimeMs;
    private double cachedLatitude;
    private double cachedLongitude;
    private float cachedAccelX;
    private float cachedAccelY;
    private float cachedAccelZ;
    // --- Metadata Collection ---

    private UserProfile userProfile;

    private final AtomicBoolean isFirstMetadataCollected = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_payment);

        // Initialize services
        metadata = Metadata.getInstance(this);
        quietTransmitter = new QuietTransmitter(this, AUDIO_TRANSMISSION_DELAY_MS);

        // Find views
        layoutEnterAmount = findViewById(R.id.layoutEnterAmount);
        layoutShowQr = findViewById(R.id.layoutShowQr);
        tvAmountValue = findViewById(R.id.tvAmountValue);
        btnGenerateQr = findViewById(R.id.btnGenerateQr);
        tvScanToReceive = findViewById(R.id.tvScanToReceive);
        ivQrCode = findViewById(R.id.ivQrCode);
        Button btnMakeAnotherPayment = findViewById(R.id.btnMakeAnotherPayment);

        userProfile = UserProfile.getInstance();

        // Setup keypad listeners
        setupKeypadListeners();

        btnGenerateQr.setOnClickListener(v -> {
            layoutEnterAmount.setVisibility(View.GONE);
            layoutShowQr.setVisibility(View.VISIBLE);
            String paymentAmount = safeText(tvAmountValue);
            String paymentMsg = "Scan to receive HK$" + paymentAmount + "\nfrom " + userProfile.getUsername();
            tvScanToReceive.setText(paymentMsg);
            Glide.with(this).load(R.drawable.loading).into(ivQrCode);
            new Handler(Looper.getMainLooper())
                    .postDelayed(this::startMetadataCollectionAndTransmission, METADATA_COLLECTION_DELAY);

        });

        btnMakeAnotherPayment.setOnClickListener(v -> {
            stopMetadataCollectionAndTransmission();
            layoutShowQr.setVisibility(View.GONE);
            layoutEnterAmount.setVisibility(View.VISIBLE);
            tvAmountValue.setText("");
            tvScanToReceive.setText("");
            ivQrCode.setImageBitmap(null);
            updateGenerateButtonEnabled();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnFinish).setOnClickListener(v -> finish());

        updateGenerateButtonEnabled();
    }

    private void startMetadataCollectionAndTransmission() {
        isFirstMetadataCollected.set(false);
        if (metadataScheduler == null || metadataScheduler.isShutdown()) {
            metadataScheduler = Executors.newSingleThreadScheduledExecutor();
        }
        // Start collecting metadata at a fixed interval
        metadataScheduler.scheduleWithFixedDelay(
                () -> metadata.startCollection(this, this),
                0,
                METADATA_COLLECTION_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void stopMetadataCollectionAndTransmission() {
        // Stop the scheduler
        if (metadataScheduler != null) {
            metadataScheduler.shutdownNow();
            metadataScheduler = null;
        }
        // Stop audio transmission
        if (quietTransmitter.isTransmitting()) {
            quietTransmitter.stop();
        }
    }

    @Override
    public void onMetadataCollected(long timeMs, Location location, float[] accelerometer) {
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
                needsUpdate = Arithmetic.hasExceededTolerance(timeMs, cachedTimeMs, Metadata.TIME_MS_TOL)
                        || Arithmetic.hasExceededTolerance(latitude, cachedLatitude, Metadata.LATITUDE_TOL)
                        || Arithmetic.hasExceededTolerance(longitude, cachedLongitude, Metadata.LONGITUDE_TOL)
                        || Arithmetic.hasExceededTolerance(accelX, cachedAccelX, Metadata.X_AXIS_TOL)
                        || Arithmetic.hasExceededTolerance(accelY, cachedAccelY, Metadata.Y_AXIS_TOL)
                        || Arithmetic.hasExceededTolerance(accelZ, cachedAccelZ, Metadata.Z_AXIS_TOL);

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

    @Override
    public void onMetadataCollectionFailure(String errorMessage) {
        // Show error on UI thread, but don't stop the scheduler from retrying
        runOnUiThread(() -> Toast.makeText(this, "Metadata Error: " + errorMessage, Toast.LENGTH_SHORT).show());
    }

    private void updateQrAndAudio() {
        // This method is called on the UI thread when metadata is updated
        synchronized (metadataLock) {
            // Abort ongoing transmission before starting a new one
            if (quietTransmitter.isTransmitting()) {
                quietTransmitter.stop();
            }
            String[] paymentAmount = splitAmountParts(safeText(tvAmountValue));

            Map<String, byte[]> payloads = null;
            try {
                payloads = Payload.rawInputsToPayloads(
                        userProfile.getUsername(),
                        userProfile.isOnline() ? userProfile.getSecretBytes() : GlobalConst.TEST_SECRET,
                        paymentAmount[0], paymentAmount[1],
                        cachedTimeMs, cachedLatitude, cachedLongitude,
                        cachedAccelX, cachedAccelY, cachedAccelZ
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

    @Override
    protected void onPause() {
        super.onPause();
        stopMetadataCollectionAndTransmission();
        metadata.stopUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        metadata.startUpdates();
        // If returning to the QR screen, restart the process
        if (layoutShowQr.getVisibility() == View.VISIBLE) {
            startMetadataCollectionAndTransmission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMetadataCollectionAndTransmission(); // Ensure all background tasks are stopped
    }

    // --- Keypad and UI Logic (largely unchanged) ---

    private void setupKeypadListeners() {
        findViewById(R.id.btn0).setOnClickListener(v -> onKeypadPress("0"));
        findViewById(R.id.btn1).setOnClickListener(v -> onKeypadPress("1"));
        findViewById(R.id.btn2).setOnClickListener(v -> onKeypadPress("2"));
        findViewById(R.id.btn3).setOnClickListener(v -> onKeypadPress("3"));
        findViewById(R.id.btn4).setOnClickListener(v -> onKeypadPress("4"));
        findViewById(R.id.btn5).setOnClickListener(v -> onKeypadPress("5"));
        findViewById(R.id.btn6).setOnClickListener(v -> onKeypadPress("6"));
        findViewById(R.id.btn7).setOnClickListener(v -> onKeypadPress("7"));
        findViewById(R.id.btn8).setOnClickListener(v -> onKeypadPress("8"));
        findViewById(R.id.btn9).setOnClickListener(v -> onKeypadPress("9"));
        findViewById(R.id.btnDot).setOnClickListener(v -> onKeypadPress("."));
        findViewById(R.id.btnBackspace).setOnClickListener(v -> onKeypadPress("x"));
    }

    private void onKeypadPress(@NonNull String key) {
        String current = safeText(tvAmountValue);

        if ("x".equals(key)) {
            if (!current.isEmpty()) {
                tvAmountValue.setText(current.substring(0, current.length() - 1));
            }
        } else if (".".equals(key)) {
            if (!current.isEmpty() && !current.contains(".")) {
                tvAmountValue.setText(current + ".");
            }
        } else { // Digit
            if (current.length() >= MAX_TOTAL_CHARS) return;
            if ("0".equals(current)) return; // Must be followed by a dot

            String[] parts = splitAmountParts(current);
            boolean hasDot = current.contains(".");

            if (!hasDot) {
                if (parts[0].length() < MAX_INT_DIGITS) {
                    tvAmountValue.setText(current + key);
                }
            } else {
                if (parts[1].isEmpty()) {
                    tvAmountValue.setText(current + key);
                }
            }
        }
        updateGenerateButtonEnabled();
    }

    @NonNull
    private String[] splitAmountParts(@NonNull String amount) {
        int dotIdx = amount.indexOf('.');
        if (dotIdx < 0) return new String[]{amount, ""};
        String integer = amount.substring(0, dotIdx);
        String fraction = (dotIdx + 1 < amount.length()) ? amount.substring(dotIdx + 1) : "";
        return new String[]{integer, fraction};
    }

    private void updateGenerateButtonEnabled() {
        String amount = safeText(tvAmountValue);
        btnGenerateQr.setEnabled(isValidNonZeroAmount(amount));
    }

    private boolean isValidNonZeroAmount(@NonNull String amount) {
        if (TextUtils.isEmpty(amount) || amount.endsWith(".")) return false;
        try {
            return Double.parseDouble(amount) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @NonNull
    private static String safeText(@NonNull TextView tv) {
        CharSequence cs = tv.getText();
        return cs == null ? "" : cs.toString().trim();
    }
}
