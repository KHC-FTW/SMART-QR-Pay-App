package com.khchan744.smart_qr_pay.bgservicetest;

import com.khchan744.smart_qr_pay.bgservice.Payload;

import org.junit.Assert;
import org.junit.Test;

public class PayloadTest {
    @Test
    public void testOffsetFormatConversion(){
        double offset1 = 0.123456;
        double offset2 = -0.242395434;
        double offset3 = 0.33;
        double offset4 = -0.2899;

        byte[] byteOffset1 = Payload.offsetValueToTwoBytes(offset1);
        double convertedOffset1 = Payload.twoBytesToOffsetValue(byteOffset1);
        System.out.println("Converted offset: " + convertedOffset1);
        Assert.assertEquals("0.1235", String.valueOf(convertedOffset1));

        byte[] byteOffset2 = Payload.offsetValueToTwoBytes(offset2);
        double convertedOffset2 = Payload.twoBytesToOffsetValue(byteOffset2);
        System.out.println("Converted offset: " + convertedOffset2);
        Assert.assertEquals("-0.2424", String.valueOf(convertedOffset2));

        byte[] byteOffset3 = Payload.offsetValueToTwoBytes(offset3);
        double convertedOffset3 = Payload.twoBytesToOffsetValue(byteOffset3);
        System.out.println("Converted offset: " + convertedOffset3);
        Assert.assertEquals("0.33", String.valueOf(convertedOffset3));

        byte[] byteOffset4 = Payload.offsetValueToTwoBytes(offset4);
        double convertedOffset4 = Payload.twoBytesToOffsetValue(byteOffset4);
        System.out.println("Converted offset: " + convertedOffset4);
        Assert.assertEquals("-0.2899", String.valueOf(convertedOffset4));
    }

    @Test
    public void testOffsetFormatConversion2(){
        double offset1 = -0.123456789123;
        double offset2 = 0.499999999999;
        double offset3 = 0.0;
        double offset4 = -0.289964355;

        byte[] byteOffset1 = Payload.offsetValueToFourBytes(offset1);
        double convertedOffset1 = Payload.fourBytesToOffsetValue(byteOffset1);
        System.out.println("Converted offset: " + convertedOffset1);
        Assert.assertEquals("-0.123456789", String.valueOf(convertedOffset1));

        byte[] byteOffset2 = Payload.offsetValueToFourBytes(offset2);
        double convertedOffset2 = Payload.fourBytesToOffsetValue(byteOffset2);
        System.out.println("Converted offset: " + convertedOffset2);
        Assert.assertEquals("0.5", String.valueOf(convertedOffset2));

        byte[] byteOffset3 = Payload.offsetValueToFourBytes(offset3);
        double convertedOffset3 = Payload.fourBytesToOffsetValue(byteOffset3);
        System.out.println("Converted offset: " + convertedOffset3);
        Assert.assertEquals("0.0", String.valueOf(convertedOffset3));

        byte[] byteOffset4 = Payload.offsetValueToFourBytes(offset4);
        double convertedOffset4 = Payload.fourBytesToOffsetValue(byteOffset4);
        System.out.println("Converted offset: " + convertedOffset4);
        Assert.assertEquals("-0.289964355", String.valueOf(convertedOffset4));
    }

    @Test
    public void testPaymentAmountConversion(){
        String paInt = "673";
        String paFrac = "5";
        byte[] paBytes = Payload.paymentAmountToBytes(paInt, paFrac);
        float convertedPa = Payload.bytesToPaymentAmount(paBytes);
        System.out.println("Converted payment amount: " + convertedPa);
        Assert.assertEquals("673.5", String.valueOf(convertedPa));
    }
}
