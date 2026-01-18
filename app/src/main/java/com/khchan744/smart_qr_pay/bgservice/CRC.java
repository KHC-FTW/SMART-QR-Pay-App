package com.khchan744.smart_qr_pay.bgservice;

import java.nio.ByteBuffer;

public class CRC {
    private static final int POLYNOMIAL = 0x1021; // CRC-16-CCITT (XMODEM)
    private static final int INITIAL_VALUE = 0x0000;

    // computes the CRC16-CCITT checksum for the given byte array
    public static byte[] computeCrc16(byte[] data) {
        int crc = INITIAL_VALUE;

        for (byte b : data) {
            crc ^= (b & 0xFF) << 8; // XOR the byte into the high-order byte of crc
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) { // Check if MSB is 1
                    crc = (crc << 1) ^ POLYNOMIAL;
                } else {
                    crc <<= 1;
                }
            }
        }

        // the result is a 16-bit value (2 bytes)
        return ByteBuffer.allocate(2).putShort((short) (crc & 0xFFFF)).array();
    }


    private CRC(){

    }
}
