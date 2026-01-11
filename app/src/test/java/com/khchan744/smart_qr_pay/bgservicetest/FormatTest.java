package com.khchan744.smart_qr_pay.bgservicetest;

import com.khchan744.smart_qr_pay.bgservice.Crypto;
import com.khchan744.smart_qr_pay.bgservice.Format;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import javax.crypto.SecretKey;

public class FormatTest {
    @Test
    public void testBytesDecimalConversion() throws Exception {
        SecretKey key = Crypto.generateAesKey();
        byte[] keyBytes = key.getEncoded();
        int[] coefficients = Format.twoBytesToDecimals(keyBytes);
        System.out.println("Coefficient values from key: " + Arrays.toString(coefficients));
        byte[] recoveredKeyBytes = Format.decimalsToTwoBytes(coefficients);
        Assert.assertArrayEquals(keyBytes, recoveredKeyBytes);
        SecretKey recoveredKey = Crypto.secretKeyFromBytes(recoveredKeyBytes);
        Assert.assertEquals(key, recoveredKey);
    }

    @Test
    public void testBytesDecimalConversion2(){
        int decVal = 9999;
        byte[] bytes = Format.oneDecimalToTwoBytes(decVal);
        int recoveredDecVal = Format.twoBytesToDecimals(bytes)[0];
        Assert.assertEquals(decVal, recoveredDecVal);
    }

    @Test
    public void testBytesDecimalConversion3(){
        int decVal = 0;
        byte[] bytes = Format.oneDecimalToTwoBytes(decVal);
        int recoveredDecVal = Format.twoBytesToDecimals(bytes)[0];
        Assert.assertEquals(decVal, recoveredDecVal);
    }

    @Test
    public void testBytesDecimalConversion4(){
        int decVal = 9;
        byte[] bytes = Format.oneDecimalToTwoBytes(decVal);
        int recoveredDecVal = Format.twoBytesToDecimals(bytes)[0];
        Assert.assertEquals(decVal, recoveredDecVal);
    }
}
