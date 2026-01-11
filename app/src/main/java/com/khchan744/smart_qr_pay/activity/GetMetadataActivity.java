package com.khchan744.smart_qr_pay.activity;

import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.khchan744.smart_qr_pay.R;
//import com.khchan744.smart_qr_pay.bgservice.Metadata;
import com.khchan744.smart_qr_pay.bgservice.Metadata;

public class GetMetadataActivity extends AppCompatActivity implements Metadata.MetadataCallback {

    private TextView tvTimestamp;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvAccelX;
    private TextView tvAccelY;
    private TextView tvAccelZ;
    private TextView tvError;

    private Button btnGet;
    private Button btnClear;
    private Button btnBack;

    private Metadata metadata;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // This can cause layout issues, consider removing if not needed.
        setContentView(R.layout.activity_get_metadata);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        metadata = Metadata.getInstance(this);

        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvAccelX = findViewById(R.id.tvAccelX);
        tvAccelY = findViewById(R.id.tvAccelY);
        tvAccelZ = findViewById(R.id.tvAccelZ);
        tvError = findViewById(R.id.tvError);

        btnGet = findViewById(R.id.btnGet);
        btnClear = findViewById(R.id.btnClear);
        btnBack = findViewById(R.id.btnBack);

        btnGet.setOnClickListener(v -> onGetPressed());
        btnClear.setOnClickListener(v -> clearAll());
        btnBack.setOnClickListener(v -> finish());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop updates to prevent memory leaks if this is the last activity using metadata
        if (isFinishing()) {
            metadata.stopUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        metadata.startUpdates();
    }

    private void onGetPressed() {
        tvError.setText("");
        metadata.startCollection(this, this);
    }

    private void clearAll() {
        tvTimestamp.setText("—");
        tvLatitude.setText("—");
        tvLongitude.setText("—");
        tvAccelX.setText("—");
        tvAccelY.setText("—");
        tvAccelZ.setText("—");
        tvError.setText("");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        metadata.onRequestPermissionsResult(requestCode, grantResults, this, this);
    }

    @Override
    public void onMetadataCollected(long timeMs, Location location, float[] accelerometer) {
        tvTimestamp.setText(String.valueOf(timeMs));

        if (location != null) {
            tvLatitude.setText(String.valueOf(location.getLatitude()));
            tvLongitude.setText(String.valueOf(location.getLongitude()));
        } else {
            tvLatitude.setText("—");
            tvLongitude.setText("—");
        }

        if (accelerometer != null && accelerometer.length >= 3) {
            tvAccelX.setText(String.valueOf(accelerometer[0]));
            tvAccelY.setText(String.valueOf(accelerometer[1]));
            tvAccelZ.setText(String.valueOf(accelerometer[2]));
        } else {
            tvAccelX.setText("—");
            tvAccelY.setText("—");
            tvAccelZ.setText("—");
        }

        tvError.setText("");
    }

    @Override
    public void onMetadataCollectionFailure(String errorMessage) {
        tvError.setText(errorMessage);
    }
}
