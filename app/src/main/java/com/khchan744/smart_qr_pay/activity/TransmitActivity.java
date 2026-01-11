package com.khchan744.smart_qr_pay.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.bgservice.QuietTransmitter;

import java.nio.charset.StandardCharsets;

public class TransmitActivity extends AppCompatActivity {

    private static final int MAX_MESSAGE_LEN = 100;

    private static final int TRANSMISSION_DELAY_MS = 1800;

    private EditText etMessage;
    private Button btnTransmit;
    private Button btnStop;
    private Button btnBack;
    private TextView tvStatus;

    private QuietTransmitter quietTransmitterHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transmit);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etMessage = findViewById(R.id.etMessage);
        btnTransmit = findViewById(R.id.btnTransmit);
        btnStop = findViewById(R.id.btnStop);
        btnBack = findViewById(R.id.btnBack3);
        tvStatus = findViewById(R.id.tvStatus);

        quietTransmitterHelper = new QuietTransmitter(this, TRANSMISSION_DELAY_MS);

        // Default states
        updateUiForStoppedState();

        btnBack.setOnClickListener(v -> finish());

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > MAX_MESSAGE_LEN) {
                    String truncated = s.subSequence(0, MAX_MESSAGE_LEN).toString();
                    etMessage.setText(truncated);
                    etMessage.setSelection(truncated.length());
                    tvStatus.setText(String.format("Message too long. Max %d characters.", MAX_MESSAGE_LEN));
                } else {
                    if (tvStatus.getText().toString().startsWith("Message too long")) {
                        tvStatus.setText("");
                    }
                }
                btnTransmit.setEnabled(etMessage.getText().toString().trim().length() > 0);
            }
        });

        btnTransmit.setOnClickListener(v -> handleTransmitClick());
        btnStop.setOnClickListener(v -> handleStopClick());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (quietTransmitterHelper.isTransmitting()) {
            handleStopClick();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure all resources are released
        quietTransmitterHelper.stop();
    }

    private void handleTransmitClick() {
        byte[] payload = etMessage.getText().toString().getBytes(StandardCharsets.UTF_8);

        quietTransmitterHelper.start(payload,
                // onStart callback (runs on main thread)
                () -> runOnUiThread(this::updateUiForTransmittingState),
                // onFailure callback (runs on main thread)
                () -> runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to initialize transmitter", Toast.LENGTH_SHORT).show();
                    updateUiForStoppedState();
                })
        );
    }

    private void handleStopClick() {
        quietTransmitterHelper.stop();
        updateUiForStoppedState();
    }

    private void updateUiForTransmittingState() {
        tvStatus.setText("Message is transmitting...");
        etMessage.setEnabled(false);
        btnTransmit.setEnabled(false);
        btnStop.setEnabled(true);
    }

    private void updateUiForStoppedState() {
        tvStatus.setText("");
        etMessage.setEnabled(true);
        btnTransmit.setEnabled(etMessage.getText().toString().trim().length() > 0);
        btnStop.setEnabled(false);
    }
}
