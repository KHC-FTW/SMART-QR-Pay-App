package com.khchan744.smart_qr_pay.bgservice;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class FuzzyVaultInterpolationTest {

    @Test
    public void reconstructPolynomialCoefficients_roundTrip_randomSmall() {
        Random r = new Random(12345);

        for (int trial = 0; trial < 50; trial++) {
            int k = 2 + r.nextInt(10); // degree < k
            int[] coeffs = new int[k];
            for (int i = 0; i < k; i++) {
                // stay in 0..65535 domain, but interpolation works mod 65537
                coeffs[i] = r.nextInt(65536);
            }

            int[] xs = new int[k];
            int[] ys = new int[k];
            // pick distinct x values in 0..65535
            boolean[] used = new boolean[65536];
            for (int i = 0; i < k; i++) {
                int x;
                do {
                    x = r.nextInt(65536);
                } while (used[x]);
                used[x] = true;
                xs[i] = x;
                ys[i] = FuzzyVault.evaluatePolynomial(coeffs, x);
            }

            int[] reconstructed = FuzzyVault.reconstructPolynomialCoefficients(xs, ys);
            assertArrayEquals("Trial " + trial + " failed", coeffs, reconstructed);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void reconstructPolynomialCoefficients_duplicateX_throws() {
        int[] xs = new int[]{10, 10};
        int[] ys = new int[]{5, 7};
        FuzzyVault.reconstructPolynomialCoefficients(xs, ys);
    }

    @Test
    public void reconstructPolynomialCoefficients_singlePoint_constant() {
        int[] xs = new int[]{123};
        int[] ys = new int[]{456};
        int[] coeffs = FuzzyVault.reconstructPolynomialCoefficients(xs, ys);
        assertArrayEquals(new int[]{456}, coeffs);
        assertEquals(456, FuzzyVault.evaluatePolynomial(coeffs, 0));
        assertEquals(456, FuzzyVault.evaluatePolynomial(coeffs, 123));
        assertEquals(456, FuzzyVault.evaluatePolynomial(coeffs, 65535));
    }
}

