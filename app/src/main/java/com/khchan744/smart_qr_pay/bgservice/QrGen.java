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
        // You'll need a utility function for this conversion, as BoofCV
        // does not have direct Bitmap conversion built-in to its core library.
        // A simple conversion can be implemented as shown below.
        // return convertGrayU8ToBitmap(grayImage);
        Bitmap bitmap = Bitmap.createBitmap(grayImage.width, grayImage.height, Bitmap.Config.ARGB_8888);
        ConvertBitmap.grayToBitmap(grayImage, bitmap, null);
        return bitmap;
    }

    // Utility function to convert BoofCV GrayU8 to Android Bitmap
    /*private static Bitmap convertGrayU8ToBitmap(GrayU8 gray) {
        int width = gray.width;
        int height = gray.height;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Iterate over pixels and set them in the Bitmap
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = gray.get(x, y) & 0xFF;
                // QR codes use black for modules (data) and white for background
                int color = value == 0 ? 0xFF000000 : 0xFFFFFFFF;
                bitmap.setPixel(x, y, color);
            }
        }
        return bitmap;
    }*/
    private QrGen(){

    }
}
