package com.khchan744.smart_qr_pay.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.khchan744.smart_qr_pay.R;
import com.khchan744.smart_qr_pay.bgservice.QrGen;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import boofcv.android.ConvertBitmap;
import boofcv.alg.fiducial.qrcode.QrCodeEncoder;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage;
import boofcv.struct.image.GrayU8;

public class GenQRActivity extends AppCompatActivity {

    private static final int MAX_MESSAGE_CHARS = 50;
    private static final int PAYLOAD_TOTAL_BYTES = 770;

    private EditText etMessage;
    private Button btnGenerate;
    private Button btnClear;
    private ImageView ivQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gen_qr);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnBack = findViewById(R.id.btnBackGenQR);
        btnBack.setOnClickListener(v -> finish());

        etMessage = findViewById(R.id.etGenQrMessage);
        btnGenerate = findViewById(R.id.btnGenerateQr);
        btnClear = findViewById(R.id.btnClearQr);
        ivQr = findViewById(R.id.ivQrCode);

        // Hard cap message length
        etMessage.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_MESSAGE_CHARS)});

        // Initial state
        btnGenerate.setEnabled(false);
        btnClear.setEnabled(false);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Only allow Generate when there's at least 1 char and we're not already showing a QR
                btnGenerate.setEnabled(s != null && s.length() > 0 && !btnClear.isEnabled());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnGenerate.setOnClickListener(v -> {
            btnGenerate.setEnabled(false); // prevent repeated tapping

            String message = etMessage.getText() == null ? "" : etMessage.getText().toString();
            if (message.isEmpty()) {
                return;
            }

            long timestampMs = System.currentTimeMillis();
            byte[] payload = buildPayload770(message, timestampMs);

            //Bitmap qrBitmap = generateQrBitmap(payload);
            Bitmap qrBitmap = QrGen.generateQrCodeFromBytes(payload);
            ivQr.setImageBitmap(qrBitmap);

            btnClear.setEnabled(true);
        });

        btnClear.setOnClickListener(v -> {
            etMessage.setText("");
            ivQr.setImageDrawable(null);
            btnClear.setEnabled(false);
            btnGenerate.setEnabled(false);
        });
    }

    /**
     * Payload format (ASCII header + binary padding):
     *   SQRP|v1|len=<n>|ts=<millis>\n<message bytes>
     * Then 0x00 padded/truncated to exactly 770 bytes.
     */
    @NonNull
    private static byte[] buildPayload770(@NonNull String message, long timestampMs) {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);

        // Keep it easy to parse and robust: length is based on the UTF-8 byte length.
        String header = "SQRP|v1|len=" + msgBytes.length + "|ts=" + timestampMs + "\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);

        byte[] combined = new byte[headerBytes.length + msgBytes.length];
        System.arraycopy(headerBytes, 0, combined, 0, headerBytes.length);
        System.arraycopy(msgBytes, 0, combined, headerBytes.length, msgBytes.length);

        byte[] out = new byte[PAYLOAD_TOTAL_BYTES];
        Arrays.fill(out, (byte) 0x00);
        System.arraycopy(combined, 0, out, 0, Math.min(combined.length, out.length));
        return out;
    }

    /**
     * Generates a QR code bitmap from raw bytes (byte mode) using BoofCV.
     */
    /*@NonNull
    private static Bitmap generateQrBitmap(@NonNull byte[] data) {
        // Encode using Byte mode + a reasonable ECC. BoofCV will pick a suitable version.
        QrCodeEncoder encoder = new QrCodeEncoder();
        encoder.setError(QrCode.ErrorLevel.M);
        encoder.addBytes(data);
        QrCode qr = encoder.fixate();

        // Render to an image. In BoofCV 1.2.4 the generator's border/quiet-zone is configured
        // internally, so we avoid calling version-specific setters.
        int pixelsPerModule = 12;
        QrCodeGeneratorImage generator = new QrCodeGeneratorImage(pixelsPerModule);
        generator.render(qr);
        GrayU8 gray = generator.getGray();

        Bitmap bitmap = Bitmap.createBitmap(gray.width, gray.height, Bitmap.Config.ARGB_8888);
        ConvertBitmap.grayToBitmap(gray, bitmap, null);
        return bitmap;
    }*/
}