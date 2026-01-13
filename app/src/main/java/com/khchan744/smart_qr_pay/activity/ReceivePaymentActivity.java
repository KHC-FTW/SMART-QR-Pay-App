package com.khchan744.smart_qr_pay.activity;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
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
import com.khchan744.smart_qr_pay.bgservice.Crypto;
import com.khchan744.smart_qr_pay.bgservice.Format;
import com.khchan744.smart_qr_pay.bgservice.Metadata;
import com.khchan744.smart_qr_pay.bgservice.Payload;
import com.khchan744.smart_qr_pay.bgservice.QrScanner;
import com.khchan744.smart_qr_pay.bgservice.QuietReceiver;
import com.khchan744.smart_qr_pay.network.NetworkClient;
import com.khchan744.smart_qr_pay.network.dto.PaymentReqDto;
import com.khchan744.smart_qr_pay.network.dto.StandardRespDto;
import com.khchan744.smart_qr_pay.param.UserProfile;
import com.khchan744.smart_qr_pay.param.GlobalConst;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@ExperimentalGetImage
public class ReceivePaymentActivity extends AppCompatActivity implements QrScanner.QrScannerCallback, QuietReceiver.ReceiverCallback, Metadata.MetadataCallback {

    private static final int BUFFER_SIZE = 1024;
    private static final long METADATA_COLLECTION_DELAY = 2000L;

    private enum State {
        SCANNING,
        PROCESSING,
        SHOWING_RESULT
    }

    private View cameraContainer;
    private View receivePaymentResultsContainer;
    private PreviewView previewView;
    private TextView tvProcessingHint;
    private TextView tvReceivePaymentResults;

    private QrScanner qrScanner;
    private QuietReceiver quietReceiver;
    private Metadata metadata;

    private volatile State currentState = State.SCANNING;

    private byte[] payloadSet1 = null;
//    private byte[] payloadSet1Latest = null;
    private byte[] payloadSet2 = null;

    private UserProfile userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_receive_payment);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cameraContainer = findViewById(R.id.cameraContainer);
        receivePaymentResultsContainer = findViewById(R.id.receivePaymentResultsContainer);
        previewView = findViewById(R.id.previewView);
        tvProcessingHint = findViewById(R.id.tvProcessingHint);
        tvReceivePaymentResults = findViewById(R.id.tvReceivePaymentResults);

        findViewById(R.id.btnScanAgain).setOnClickListener(v -> startScanningAndReceiving());
        findViewById(R.id.btnFinish).setOnClickListener(v -> {
            stopScanningAndReceiving();
            finish();
        });
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            stopScanningAndReceiving();
            finish();
        });

        metadata = Metadata.getInstance(this);
        quietReceiver = new QuietReceiver(this, this, BUFFER_SIZE);
        qrScanner = new QrScanner(this, previewView, this);

        userProfile = UserProfile.getInstance();

        startScanningAndReceiving();
    }

    private void startScanningAndReceiving() {
        // Reset state and payloads
        currentState = State.SCANNING;
        payloadSet1 = null;
        payloadSet2 = null;

        updateUiForScanningAndReceiving();
        quietReceiver.start();
        qrScanner.start();
    }

    private void stopScanningAndReceiving() {
        if (quietReceiver.isReceiving()) {
            quietReceiver.stop();
        }
        qrScanner.stop();
    }

    private void updateUiForScanningAndReceiving() {
        receivePaymentResultsContainer.setVisibility(View.GONE);
        tvProcessingHint.setVisibility(View.GONE);
        cameraContainer.setVisibility(View.VISIBLE);
        tvReceivePaymentResults.setText("");
    }

    private void updateUiForResultDisplay() {
        cameraContainer.setVisibility(View.GONE);
        receivePaymentResultsContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only restart scanning if we are not already processing or showing a result
        if (currentState == State.SCANNING) {
            startScanningAndReceiving();
        }
        metadata.startUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanningAndReceiving();
        metadata.stopUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure resources are released
        quietReceiver.stop();
        qrScanner.release();
        metadata.stopUpdates();
        payloadSet1 = null;
        payloadSet2 = null;
    }

    @Override
    public void onQrScanned(@NonNull Barcode barcode) {
        if (currentState != State.SCANNING) return;

        byte[] payloadSet1 = barcode.getRawBytes();
        if (Payload.isPayloadSet1Valid(payloadSet1)) {
            setPayloads(payloadSet1, true);
            checkAndProceedIfReady();
        } else {
            // If not valid, continue scanning
            qrScanner.resumeScanning();
        }
    }

    @Override
    public void onCameraSetupFailed(@NonNull String errorMessage) {
        currentState = State.SHOWING_RESULT;
        updateUiForResultDisplay();
        tvReceivePaymentResults.setText(errorMessage);
    }

    @Override
    public void onReceive(@NonNull byte[] payload) {
        if (currentState != State.SCANNING) return;

        if (Payload.isPayloadSet2Valid(payload)) {
            setPayloads(payload, false);
            checkAndProceedIfReady();
        } else {
            // Restart the receiver since it stops after one reception
            if (quietReceiver != null && !quietReceiver.isReceiving()) {
                quietReceiver.start();
            }
        }
    }

    @Override
    public void onFail(@NonNull String errorMessage) {
        currentState = State.SHOWING_RESULT;
        updateUiForResultDisplay();
        tvReceivePaymentResults.setText(errorMessage);
    }

    private void checkAndProceedIfReady() {
        // Check if both payloads are received and we are in the correct state
        if (currentState == State.SCANNING && payloadSet1 != null && payloadSet2 != null) {
            // Change state to prevent this block from running again
            currentState = State.PROCESSING;
            tvProcessingHint.setVisibility(View.VISIBLE);
            // Delay 2 seconds to make sure user stabilizes the phone
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                stopScanningAndReceiving();
                metadata.startCollection(this, this);
            }, METADATA_COLLECTION_DELAY);
        } else if (currentState == State.SCANNING) {
            // If only one payload is received, QR scanner needs to be told to continue
            qrScanner.resumeScanning();
            quietReceiver.start();
        }
    }

    private void setPayloads(byte[] payload, boolean isPayloadSet1) {
        if (isPayloadSet1) {
            payloadSet1 = payload;
        } else {
            payloadSet2 = payload;
        }
    }

    private byte[] getPayload(boolean isPayloadSet1) {
        if (isPayloadSet1) {
            return payloadSet1;
        } else {
            return payloadSet2;
        }
    }

    @Override
    public void onMetadataCollected(long timeMs, Location location, float[] accelerometer) {
        currentState = State.SHOWING_RESULT;
        updateUiForResultDisplay();

        byte[] payloadSet1 = getPayload(true);
        byte[] payloadSet2 = getPayload(false);

        // The check in checkAndProceedIfReady ensures these are not null
        if (payloadSet1 == null || payloadSet2 == null) {
            tvReceivePaymentResults.setText("Error: Payloads are missing.");
            return;
        }
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        float accelX = accelerometer[0];
        float accelY = accelerometer[1];
        float accelZ = accelerometer[2];

        try {
            Map<String, byte[]> recoveredSecrets = Payload.recoverSecretsFromPayloads(
                    payloadSet1, payloadSet2, timeMs, latitude, longitude,
                    accelX, accelY, accelZ);

            byte[] payerUidBytes = recoveredSecrets.get(Payload.UID_KEY);
            byte[] payerPaymentAmountBytes = recoveredSecrets.get(Payload.PA_KEY);
            byte[] payerPaymentToken = recoveredSecrets.get(Payload.DEC_PAY_TOKEN_KEY);
            byte[] payeeMetadataFingerprintBytes = recoveredSecrets.get(Payload.MF_KEY);


            if(userProfile.isOnline()){
                String paymentToken = Format.base64Encode(payerPaymentToken);
                String payerUsername = new String(payerUidBytes, StandardCharsets.UTF_8);
                String paymentAmount = String.format(Locale.US, "%.1f", Payload.bytesToPaymentAmount(payerPaymentAmountBytes));
                String payeeMetadataFingerprint = Format.base64Encode(payeeMetadataFingerprintBytes);

                PaymentReqDto paymentReqDto = new PaymentReqDto(paymentToken, payerUsername, paymentAmount, payeeMetadataFingerprint);
                String bearerToken = "Bearer " + userProfile.getJwt();
                NetworkClient.getAuthApi().verifyPayment(bearerToken, paymentReqDto).enqueue(new Callback<StandardRespDto>() {
                    @Override
                    public void onResponse(Call<StandardRespDto> call, Response<StandardRespDto> response) {
                        if (!response.isSuccessful()) {
                            tvReceivePaymentResults.setText("Request failed: HTTP " + response.code());
                            return;
                        }
                        StandardRespDto respBody = response.body();
                        if (respBody == null) {
                            tvReceivePaymentResults.setText("Empty response from server.");
                            return;
                        }
                        String message = respBody.getResponse() != null ? respBody.getResponse() : "";
                        tvReceivePaymentResults.setText(message);
                    }

                    @Override
                    public void onFailure(Call<StandardRespDto> call, Throwable t) {
                        tvReceivePaymentResults.setText("Network error: " + (t.getMessage() != null ? t.getMessage() : "unknown"));
                    }
                });

            }else{
                // if server not available, compare locally for simulation
                byte[] recomputedPaymentToken = Crypto.computePaymentToken(
                        payerUidBytes, GlobalConst.TEST_SECRET, payerPaymentAmountBytes, payeeMetadataFingerprintBytes);

                if (Arrays.equals(payerPaymentToken, recomputedPaymentToken)) {
                    float payerPaymentAmount = Payload.bytesToPaymentAmount(payerPaymentAmountBytes);
                    String payerUid = new String(payerUidBytes, StandardCharsets.UTF_8);
                    String paymentSuccess = "You have successfully received HK$" + payerPaymentAmount + " from " + payerUid + ".";
                    tvReceivePaymentResults.setText(paymentSuccess);
                } else {
                    tvReceivePaymentResults.setText("Receive payment failure: invalid payment token.");
                }
            }

        } catch (Exception e) {
            tvReceivePaymentResults.setText(e.getMessage());
        }
    }

    @Override
    public void onMetadataCollectionFailure(String errorMessage) {
        // Check if we are still in the processing state to avoid multiple retries
        if (currentState == State.PROCESSING) {
            updateUiForResultDisplay();
            String retryMessage = errorMessage + "\nRetrying...";
            tvReceivePaymentResults.setText(retryMessage);
            // Retry collection
            metadata.startCollection(this, this);
        }
    }
}
