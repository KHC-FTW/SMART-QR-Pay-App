package com.khchan744.smart_qr_pay.bgservicetest;

import com.khchan744.smart_qr_pay.bgservice.Crypto;
import com.khchan744.smart_qr_pay.bgservice.Format;
import com.khchan744.smart_qr_pay.bgservice.FuzzyVault;
import com.khchan744.smart_qr_pay.customexception.FuzzyVaultException;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import javax.crypto.SecretKey;

public class FuzzyVaultTest {


    @Test
    public void testEvaluatePolynomial() {
        // 38724x^4 + 60001x^3 + 40031x^2 + 734x + 42342
        int[] coefficients = {42342, 734, 40031, 60001, 38724};
        int x = 52342;
        long y = FuzzyVault.evaluatePolynomial(coefficients, x);
        System.out.println("y = " + y);
        Assert.assertEquals(39748, y);
    }

    @Test
    public void testGenerateGenuineSet(){
        int[] coefficients = {
                53650, 3651, 11260, 53126,
                33625, 7859, 33296, 21388
        };
        int[] genuinePointsX = {
                63306, 33711, 33104, 46081,
                11032, 36693, 36240, 6495,
                34656, 52694, 10102, 24227,
                10547, 18483, 11900, 40323
        };
        int[][] genuineSet = FuzzyVault.generateGenuineSet(coefficients, genuinePointsX);
        System.out.println("Genuine points:");
        for (int i = 0; i < genuineSet[0].length; i++){
            System.out.printf("(%5d, %5d)\n", genuineSet[0][i], genuineSet[1][i]);
        }
        Assert.assertEquals(16, genuineSet[0].length);
        Assert.assertEquals(16, genuineSet[1].length);

        int[] reconstructedCoefficients = FuzzyVault.reconstructPolynomialCoefficients(genuineSet);
        System.out.println(Arrays.toString(reconstructedCoefficients));
    }

    @Test
    public void testFuzzyVaultLockAndUnlock() throws FuzzyVaultException {
        int[] coefficients = {
                53650, 3651, 11260, 53126,
                33625, 7859, 33296, 21388
        };
        int[] genuinePointsX = {
                63306, 33711, 33104, 46081,
                11032, 36693, 36240, 6495,
                34656, 52694, 10102, 24227,
                10547, 18483, 11900, 40323
        };
        int[] genuineSetX1 = {
                63306, 33711, 36240, 6495,
                52694, 10102, 10547, 40323
        };
        int[] genuineSetX2 = {
                34656, 52694, 10102, 24227,
                10547, 18483, 11900, 40323
        };
        int[][] genuineSet = FuzzyVault.generateGenuineSet(coefficients, genuinePointsX);
        int[][] chaffSet = FuzzyVault.generateChaffSetV3(coefficients, genuinePointsX, 160);
        int[][] fuzzyVault = FuzzyVault.generateFuzzyVault(genuineSet, chaffSet);
        Assert.assertEquals(176, fuzzyVault[0].length);
        System.out.println("Fuzzy vault points:");
        for (int i = 0; i < fuzzyVault[0].length; i++){
            System.out.printf("(%5d, %5d)\n", fuzzyVault[0][i], fuzzyVault[1][i]);
        }

        byte[] vaultBytes = FuzzyVault.flattenFuzzyVaultToBytes(fuzzyVault);
        int[][] recoveredFvPairs = FuzzyVault.fuzzyVaultBytesToDecimalPairs(vaultBytes);
        Assert.assertArrayEquals(fuzzyVault[0], recoveredFvPairs[0]);
        Assert.assertArrayEquals(fuzzyVault[1], recoveredFvPairs[1]);

        int[][] candidateGenuinePoints = FuzzyVault.matchGenuinePointsFromFV(recoveredFvPairs, genuinePointsX, 8);
        int[] reconstructedCoefficients = FuzzyVault.reconstructPolynomialCoefficients(candidateGenuinePoints);
        System.out.println("Reconstructed coefficients: " + Arrays.toString(reconstructedCoefficients));

        int[][] genuineSet1 = FuzzyVault.matchGenuinePointsFromFV(recoveredFvPairs, genuineSetX1, 8);
        int[] reconstructedCoefficientsSet1 = FuzzyVault.reconstructPolynomialCoefficients(genuineSet1);
        System.out.println("Reconstructed coefficients from Set 1: " + Arrays.toString(reconstructedCoefficientsSet1));

        int[][] genuineSet2 = FuzzyVault.matchGenuinePointsFromFV(recoveredFvPairs, genuineSetX2, 8);
        int[] reconstructedCoefficientsSet2 = FuzzyVault.reconstructPolynomialCoefficients(genuineSet2);
        System.out.println("Reconstructed coefficients from Set 2: " + Arrays.toString(reconstructedCoefficientsSet2));

        Assert.assertArrayEquals(coefficients, reconstructedCoefficients);
        Assert.assertArrayEquals(coefficients, reconstructedCoefficientsSet1);
        Assert.assertArrayEquals(coefficients, reconstructedCoefficientsSet2);

    }
}
