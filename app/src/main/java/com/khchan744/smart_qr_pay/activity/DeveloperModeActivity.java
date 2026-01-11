package com.khchan744.smart_qr_pay.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.khchan744.smart_qr_pay.R;

@ExperimentalGetImage
public class DeveloperModeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_developer_mode);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnTestGetMetadata = findViewById(R.id.btnTestGetMetadata);
        Button btnTestTransmitter = findViewById(R.id.btnTestTransmitter);
        Button btnTestReceiver = findViewById(R.id.btnTestReceiver);
        Button btnTestGenQrCode = findViewById(R.id.btnTestGenQrCode);
        Button btnTestScanQrCode = findViewById(R.id.btnTestScanQrCode);
        Button btnTransferMetadata = findViewById(R.id.btnTransferMetadata);
        Button btnCompareMetadata = findViewById(R.id.btnCompareMetadata);
        Button btnBack = findViewById(R.id.btnBack);

        btnTestGetMetadata.setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, GetMetadataActivity.class)));

        btnTestTransmitter.setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, TransmitActivity.class)));

        btnTestReceiver.setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, ReceiveActivity.class)));

        btnTestGenQrCode.setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, GenQRActivity.class)));

        btnTestScanQrCode.setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, ScanQRActivity.class)));

        btnTransferMetadata.setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, TransferMetadataActivity.class)));

        btnCompareMetadata.setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, CompareMetadataActivity.class)));

        btnBack.setOnClickListener(v -> finish());
    }

    private void showClickedToast(String buttonName) {
        Toast.makeText(this, "You clicked " + buttonName, Toast.LENGTH_SHORT).show();
    }
}

