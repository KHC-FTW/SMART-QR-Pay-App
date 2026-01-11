package com.khchan744.smart_qr_pay.bgservicetest;

import com.khchan744.smart_qr_pay.bgservice.CRC;

import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

public class CRCTest {
    @Test
    public void test1_ComputeCrc16() {
        final byte[] data = {
                (byte) 0xF2, (byte) 0x23, (byte) 0xA6, (byte) 0x23,
                (byte) 0x00, (byte) 0x01, (byte)0x3B, (byte) 0xFD,
                (byte) 0xB2, (byte) 0x7E, (byte) 0x32, (byte) 0x6A,
                (byte) 0x10, (byte) 0x5C, (byte)0xFE, (byte) 0xDC
        };

        final byte[] expected = {(byte) 0x4B, (byte) 0x93};

        final byte[] checksum = CRC.computeCrc16(data);

        Assert.assertArrayEquals(expected, checksum);

    }

    @Test(expected = ArrayComparisonFailure.class)
    public void test2_ComputeCrc16() {
        final byte[] data = {
                (byte) 0xF2, (byte) 0x23, (byte) 0xA6, (byte) 0x23,
                (byte) 0x00, (byte) 0x01, (byte)0x3B, (byte) 0xFD,
                (byte) 0xB2, (byte) 0x7E, (byte) 0x32, (byte) 0x6A,
                (byte) 0x10, (byte) 0x5C, (byte)0xFE, (byte) 0xDC
        };

        // artificially change one of the bits to simulate data corruption
        data[1] = (byte) ((data[1] ^ 0x01) & 0xFF);

        final byte[] expected = {(byte) 0x4B, (byte) 0x93};

        final byte[] checksum = CRC.computeCrc16(data);

        Assert.assertArrayEquals(expected, checksum);

    }
}
