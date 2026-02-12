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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnTestGetMetadata).setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, GetMetadataActivity.class)));

        findViewById(R.id.btnTestTransmitter).setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, TransmitActivity.class)));

        findViewById(R.id.btnTestReceiver).setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, ReceiveActivity.class)));

        findViewById(R.id.btnTestGenQrCode).setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, GenQRActivity.class)));

        findViewById(R.id.btnTestScanQrCode).setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, ScanQRActivity.class)));

        findViewById(R.id.btnTransferMetadata).setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, TransferMetadataActivity.class)));

        findViewById(R.id.btnCompareMetadata).setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, CompareMetadataActivity.class)));

        findViewById(R.id.btnSecurityDemo).setOnClickListener(v ->
                startActivity(new Intent(DeveloperModeActivity.this, SecurityDemoActivity.class)));

    }

    private void showClickedToast(String buttonName) {
        Toast.makeText(this, "You clicked " + buttonName, Toast.LENGTH_SHORT).show();
    }
}

