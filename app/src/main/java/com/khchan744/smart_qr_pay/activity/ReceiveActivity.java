package com.khchan744.smart_qr_pay.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.bgservice.QuietReceiver;

import java.nio.charset.StandardCharsets;

public class ReceiveActivity extends AppCompatActivity implements QuietReceiver.ReceiverCallback {

    private Button btnBack;
    private Button btnReceive;
    private Button btnStop;
    private TextView tvReceived;
    private TextView tvError;

    private QuietReceiver quietReceiver;

    private static final int BUFFER_SIZE = 1024;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_receive);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBack = findViewById(R.id.btnBack2);
        btnReceive = findViewById(R.id.btnReceive);
        btnStop = findViewById(R.id.btnStop);
        tvReceived = findViewById(R.id.tvReceived);
        tvError = findViewById(R.id.tvError);

        quietReceiver = new QuietReceiver(this, this, BUFFER_SIZE);

        updateUiForStoppedState();

        btnBack.setOnClickListener(v -> finish());
        btnReceive.setOnClickListener(v -> handleReceiveClick());
        btnStop.setOnClickListener(v -> handleStopClick());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (quietReceiver.isReceiving()) {
            handleStopClick();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        quietReceiver.stop();
    }

    private void handleReceiveClick() {
        tvReceived.setText("");
        tvError.setText("");
        updateUiForReceivingState();
        quietReceiver.start();
    }

    private void handleStopClick() {
        quietReceiver.stop();
        updateUiForStoppedState();
    }

    @Override
    public void onReceive(@NonNull byte[] payload) {
        String receivedText = new String(payload, StandardCharsets.UTF_8);
        tvReceived.setText(receivedText);
        tvError.setText("");
        // The helper auto-stops, so we just need to update the UI
        updateUiForStoppedState();
    }

    @Override
    public void onFail(@NonNull String errorMessage) {
        tvError.setText(errorMessage);
        updateUiForStoppedState();
    }

    /*@Override
    public void onPermissionDenied() {
        Toast.makeText(this, "Record audio permission is required to receive data", Toast.LENGTH_LONG).show();
        updateUiForStoppedState();
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        quietReceiver.onRequestPermissionsResult(requestCode, grantResults);
    }

    private void updateUiForReceivingState() {
        btnReceive.setEnabled(false);
        btnStop.setEnabled(true);
        tvError.setText("Receiving...");
    }

    private void updateUiForStoppedState() {
        btnReceive.setEnabled(true);
        btnStop.setEnabled(false);
        if (tvError.getText().toString().equals("Receiving...")) {
            tvError.setText("");
        }
    }
}
