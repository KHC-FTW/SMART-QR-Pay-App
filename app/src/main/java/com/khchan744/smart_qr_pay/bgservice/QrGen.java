package com.khchan744.smart_qr_pay.bgservice;

import android.graphics.Bitmap;

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeEncoder;
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage;
import boofcv.android.ConvertBitmap;
import boofcv.struct.image.GrayU8;

public class QrGen {

    public static final int PIXELS_PER_MODULE = 12;

    public static Bitmap generateQrCodeFromBytes(byte[] data) {
        // 1. Encode the byte array into a QrCode data structure
        QrCode original = new QrCodeEncoder()
                .setError(QrCode.ErrorLevel.M)
                .addBytes(data) // Use addBytes to force binary encoding
                .fixate(); // Finalize the QR code data

        // 2. Render the QrCode data structure into a BoofCV image
        QrCodeGeneratorImage generator = new QrCodeGeneratorImage(PIXELS_PER_MODULE);
        GrayU8 grayImage = generator.render(original).getGray();

        // 3. Convert the BoofCV image (GrayU8) to an Android Bitmap
        Bitmap bitmap = Bitmap.createBitmap(grayImage.width, grayImage.height, Bitmap.Config.ARGB_8888);
        ConvertBitmap.grayToBitmap(grayImage, bitmap, null);
        return bitmap;
    }

    private QrGen(){

    }
}
