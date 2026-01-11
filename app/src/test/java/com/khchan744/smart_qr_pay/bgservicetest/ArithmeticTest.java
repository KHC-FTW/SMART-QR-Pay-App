package com.khchan744.smart_qr_pay.bgservicetest;


import com.khchan744.smart_qr_pay.bgservice.Arithmetic;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class ArithmeticTest {

    @Test
    public void test1_timestamp_quantizeRefValue() {
        final long rawVal = 1766225492;
        final int tolerance = 20000;
        final boolean isTwoSided = false;

        Map<String, Object> results = Arithmetic.quantizeRefValue(rawVal, tolerance, isTwoSided);
        long quantizedVal = (long)results.get(Arithmetic.QUANTIZED_VAL_KEY);
        double offset = (double)results.get(Arithmetic.OFFSET_VAL_KEY);

        long expectedQuanVal = 1766240000;
        double expectedOffset = 0.2254;

        Assert.assertEquals(expectedQuanVal, quantizedVal);
        Assert.assertEquals(expectedOffset, offset, 0.0001);

    }

    @Test
    public void test2_accelerometer_quantizeRefValue() {
        final float rawVal = -0.03f;
        final float tolerance = 0.05f;
        final boolean isTwoSided = true;

        Map<String, Double> results = Arithmetic.quantizeRefValue(rawVal, tolerance, isTwoSided);
        double quantizedVal = results.get(Arithmetic.QUANTIZED_VAL_KEY);
        double offset = results.get(Arithmetic.OFFSET_VAL_KEY);

        double expectedQuantizedVal = 0.0;
        double expectedOffset = 0.3;

        Assert.assertEquals(expectedQuantizedVal, quantizedVal, 0.0001);
        Assert.assertEquals(expectedOffset, offset, 0.0001);

    }

    @Test
    public void test3_timestamp_equalness(){
        final int tolerance = 15000;
        final boolean isTwoSided = false;
        final long timestamp1 = 1766390541368L;
        final long timestamp2 = 1766390541368L + 14999;
        final long timestamp3 = 1766390541368L + 2132;
        final long timestamp4 = 1766390541368L + 15001;

        Map<String, Object> refResults = Arithmetic.quantizeRefValue(timestamp1, tolerance, isTwoSided);
        long quantizedTimestamp1 = (long)refResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        double offset = (double)refResults.get(Arithmetic.OFFSET_VAL_KEY);

        long quantizedTimestamp2 = Arithmetic.quantizeOtherVal(timestamp2, tolerance, isTwoSided, offset);
        long quantizedTimestamp3 = Arithmetic.quantizeOtherVal(timestamp3, tolerance, isTwoSided, offset);
        long quantizedTimestamp4 = Arithmetic.quantizeOtherVal(timestamp4, tolerance, isTwoSided, offset);

        System.out.println("Offset: " + offset);
        System.out.println("Quantized timestamp1: " + quantizedTimestamp1);
        System.out.println("Quantized timestamp2: " + quantizedTimestamp2);
        System.out.println("Quantized timestamp3: " + quantizedTimestamp3);
        System.out.println("Quantized timestamp4: " + quantizedTimestamp4);

        Assert.assertEquals(quantizedTimestamp1, quantizedTimestamp2);
        Assert.assertEquals(quantizedTimestamp1, quantizedTimestamp3);
        Assert.assertNotEquals(quantizedTimestamp1, quantizedTimestamp4);

    }

    @Test
    public void test4_geolocation_equalness(){
        final double tolerance = 0.00002;
        final boolean isTwoSided = true;
        final double latitude1 = 22.2780516;
        final double latitude2 = 22.2780492;
        final double latitude3 = 22.2780643;
        final double latitude4 = 22.2780726;
        final double latitude5 = 22.2780306;

        Map<String, Double> refResults = Arithmetic.quantizeRefValue(latitude1, tolerance, isTwoSided);
        double quantizedLatitude1 = refResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        double offset = refResults.get(Arithmetic.OFFSET_VAL_KEY);
        System.out.println("Offset: " + offset);

        double quantizedLatitude2 = Arithmetic.quantizeOtherVal(latitude2, tolerance, isTwoSided, offset);
        double quantizedLatitude3 = Arithmetic.quantizeOtherVal(latitude3, tolerance, isTwoSided, offset);
        double quantizedLatitude4 = Arithmetic.quantizeOtherVal(latitude4, tolerance, isTwoSided, offset);
        double quantizedLatitude5 = Arithmetic.quantizeOtherVal(latitude5, tolerance, isTwoSided, offset);

        System.out.println("Quantized latitude1: " + quantizedLatitude1);
        System.out.println("Quantized latitude2: " + quantizedLatitude2);
        System.out.println("Quantized latitude3: " + quantizedLatitude3);
        System.out.println("Quantized latitude4: " + quantizedLatitude4);
        System.out.println("Quantized latitude5: " + quantizedLatitude5);

        Assert.assertEquals(String.valueOf(quantizedLatitude1), String.valueOf(quantizedLatitude2));
        Assert.assertEquals(String.valueOf(quantizedLatitude1), String.valueOf(quantizedLatitude3));
        Assert.assertNotEquals(String.valueOf(quantizedLatitude1), String.valueOf(quantizedLatitude4));
        Assert.assertNotEquals(String.valueOf(quantizedLatitude1), String.valueOf(quantizedLatitude5));

    }

    @Test
    public void test5_accelerometer_equalness(){
        final float tolerance = 0.15f;
        final boolean isTwoSided = true;
        final float accelX1 = -0.0236f;
        final float accelX2 = -0.0236f + 0.1f;
        final float accelX3 = -0.0236f - 0.149f;
        final float accelX4 = -0.0236f + 0.1501f;
        final float accelX5 = -0.0236f - 0.1501f;

        Map<String, Double> refResults = Arithmetic.quantizeRefValue(accelX1, tolerance, isTwoSided);
        double quantizedRefVal = refResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        double offset = refResults.get(Arithmetic.OFFSET_VAL_KEY);

        double quantizedAccelX2 = Arithmetic.quantizeOtherVal(accelX2, tolerance, isTwoSided, offset);
        double quantizedAccelX3 = Arithmetic.quantizeOtherVal(accelX3, tolerance, isTwoSided, offset);
        double quantizedAccelX4 = Arithmetic.quantizeOtherVal(accelX4, tolerance, isTwoSided, offset);
        double quantizedAccelX5 = Arithmetic.quantizeOtherVal(accelX5, tolerance, isTwoSided, offset);

        Assert.assertEquals(String.valueOf(quantizedRefVal), String.valueOf(quantizedAccelX2));
        Assert.assertEquals(String.valueOf(quantizedRefVal), String.valueOf(quantizedAccelX3));
        Assert.assertNotEquals(String.valueOf(quantizedRefVal), String.valueOf(quantizedAccelX4));
        Assert.assertNotEquals(String.valueOf(quantizedRefVal), String.valueOf(quantizedAccelX5));

    }

    @Test
    public void test6_accelerometer_equalness(){
        final float tolerance = 100.0f;
        final boolean isTwoSided = true;
        final float accelX1 = -0.078f;
        final float accelX2 = -0.5165506f;

        Map<String, Double> refResults = Arithmetic.quantizeRefValue(accelX1, tolerance, isTwoSided);
        double quantizedRefVal = refResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        double offset = refResults.get(Arithmetic.OFFSET_VAL_KEY);

        double quantizedAccelX2 = Arithmetic.quantizeOtherVal(accelX2, tolerance, isTwoSided, offset);

        Assert.assertEquals(String.valueOf(quantizedRefVal), String.valueOf(quantizedAccelX2));

    }

    @Test
    public void test7_accelerometer_equalness(){
        final float tolerance = 100.0f;
        final boolean isTwoSided = true;
        final float accelY1 = -0.078f;
        final float accelY2 = -0.5165506f;

        Map<String, Double> refResults = Arithmetic.quantizeRefValue(accelY1, tolerance, isTwoSided);
        double quantizedRefVal = refResults.get(Arithmetic.QUANTIZED_VAL_KEY);
        double offset = refResults.get(Arithmetic.OFFSET_VAL_KEY);

        System.out.println("Offset: " + String.valueOf(offset));

        double quantizedAccelX2 = Arithmetic.quantizeOtherVal(accelY2, tolerance, isTwoSided, offset);

        Assert.assertEquals(String.valueOf(quantizedRefVal), String.valueOf(quantizedAccelX2));



    }



}
