package com.khchan744.smart_qr_pay.bgservice;

import androidx.annotation.NonNull;

import com.khchan744.smart_qr_pay.customexception.FuzzyVaultException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class FuzzyVault {
    private static final int PRIME = 65537;
    private static final int FINITE_FIELD_MAX_VAL = 65535;
    public static final int GENUINE_SET_SIZE = 16;
    public static final int CHAFF_SET_SIZE = 160;
    public static final int FUZZY_VAULT_SIZE = GENUINE_SET_SIZE + CHAFF_SET_SIZE;
    public static final int TOTAL_COEFFICIENT_SIZE = 9; // 8 for key + 1 for CRC

    private static final Random random = new Random();

    public static int[][] generateGenuineSet(@NonNull int[] coefficients, @NonNull int[] genuinePointsX){
        int[][] genuineSet = new int[2][genuinePointsX.length];
        for (int i = 0; i < genuinePointsX.length; i++){
            int y = evaluatePolynomial(coefficients, genuinePointsX[i]);
            genuineSet[0][i] = genuinePointsX[i];
            genuineSet[1][i] = y;
        }
        return genuineSet;
    }

    public static int[][] generateChaffSetV3(@NonNull int[] coefficients,
                                             @NonNull int[] genuinePointsX,
                                             @NonNull Integer chaffSize) {
        int[][] chaffSet = new int[2][chaffSize];
        final int finiteFieldSize = FINITE_FIELD_MAX_VAL + 1;

        // O(1) membership for x values in range [0, 65535]
        boolean[] usedX = new boolean[finiteFieldSize];

        // Mark genuine x as used (also normalizes to range just in case).
        for (int x : genuinePointsX) {
            int xi = x & 0xFFFF;
            usedX[xi] = true;
        }

        int done = 0;
        while (done < chaffSize) {
            int chaff_x;
            do {
                chaff_x = generateRandomInt(finiteFieldSize);
            } while (usedX[chaff_x]);

            usedX[chaff_x] = true;
            chaffSet[0][done] = chaff_x;

            int unwanted_y = evaluatePolynomial(coefficients, chaff_x);

            int chaff_y;
            do {
                chaff_y = generateRandomInt(finiteFieldSize);
            } while (chaff_y == unwanted_y);

            chaffSet[1][done] = chaff_y;
            done++;
        }

        return chaffSet;
    }

    public static int evaluatePolynomial(int[] coefficients, int x){
        long y = 0;
        long xPower = 1;
        for (int coefficient : coefficients) {
            y = (y + coefficient * xPower) % PRIME;
            xPower = (xPower * x) % PRIME;
        }
        return (int)y;
    }

    public static int[][] generateFuzzyVault(@NonNull int[][] genuineSet, @NonNull int[][] chaffSet){
        int vaultSize = genuineSet[0].length + chaffSet[0].length;
        int[][] fuzzyVault = new int[2][vaultSize];
        boolean[] filled = new boolean[vaultSize];
        // pick random spots to fill the genuine points
        for(int i = 0; i < genuineSet[0].length; i++){
            int randomIdx;
            do{
                randomIdx = generateRandomInt(vaultSize);
            }while(filled[randomIdx]);
            filled[randomIdx] = true;
            fuzzyVault[0][randomIdx] = genuineSet[0][i];
            fuzzyVault[1][randomIdx] = genuineSet[1][i];
        }
        // fill in the remaining spots with chaff points
        for (int i = 0, fvIdx = 0; i < chaffSet[0].length; i++, fvIdx++){
            while(filled[fvIdx]) fvIdx++;
            fuzzyVault[0][fvIdx] = chaffSet[0][i];
            fuzzyVault[1][fvIdx] = chaffSet[1][i];
        }
        return fuzzyVault;
    }

    public static byte[] flattenFuzzyVaultToBytes(@NonNull int[][] fuzzyVault){
        // each point has 4 bytes (2 for x and 2 for y)
        byte[] vaultBytes = new byte[fuzzyVault[0].length * 2 + fuzzyVault[1].length * 2];
        for (int i = 0; i < fuzzyVault[0].length; i++){
            byte[] xBytes = Format.oneDecimalToTwoBytes(fuzzyVault[0][i]);
            byte[] yBytes = Format.oneDecimalToTwoBytes(fuzzyVault[1][i]);
            vaultBytes[4 * i] = xBytes[0];
            vaultBytes[4 * i + 1] = xBytes[1];
            vaultBytes[4 * i + 2] = yBytes[0];
            vaultBytes[4 * i + 3] = yBytes[1];
        }
        return vaultBytes;
    }

    public static int[][] fuzzyVaultBytesToDecimalPairs(@NonNull byte[] vaultBytes){
        if (vaultBytes.length % 4 != 0){
            throw new IllegalArgumentException("Byte array length must be a multiple of 4.");
        }
        int[][] vaultPairs = new int[2][vaultBytes.length / 4];
        for (int i = 0; i < vaultPairs[0].length; i++){
            byte[] xyBytePair = new byte[4];
            System.arraycopy(vaultBytes, 4 * i, xyBytePair, 0, 4);
            int[] xyDecimalPair = Format.twoBytesToDecimals(xyBytePair);
            vaultPairs[0][i] = xyDecimalPair[0];
            vaultPairs[1][i] = xyDecimalPair[1];
        }
        return vaultPairs;
    }

    public static int[][] matchGenuinePointsFromFV(@NonNull int[][] fuzzyVault,
                                                   @NonNull int[] probableX,
                                                   int target) throws FuzzyVaultException
    {
        int[] distinctX = getDistinctIntArray(probableX);

        if (target < 1 || target > distinctX.length){
            target = distinctX.length;
        }
        int[][] genuinePoints = new int[2][target];
        int done = 0;
        for (int i = 0; i < distinctX.length; i++){
            for (int j = 0; j < fuzzyVault[0].length; j++){
                if (distinctX[i] == fuzzyVault[0][j]) {
                    genuinePoints[0][done] = fuzzyVault[0][j];
                    genuinePoints[1][done] = fuzzyVault[1][j];
                    done++;
                    break;
                }
            }
            if (done == target){
                break;
            }
        }
        if (done < target){
            throw new FuzzyVaultException("Not enough genuine points found. Expected: " + target + " but found: " + done + " only.");
        }
        return genuinePoints;
    }

    private static int[] getDistinctIntArray(int[] inputArray) {
        return IntStream.of(inputArray)
                .distinct()
                .toArray();
    }


    /**
     * Reconstruct polynomial coefficients from (x,y) pairs using Lagrange interpolation over GF(PRIME).
     *
     * <p>Input format: points[0] = x-values, points[1] = corresponding y-values.</p>
     * <p>Output format: coefficients in ascending power order (c0, c1, ..., c(k-1)) such that
     * f(x) = Σ c_i x^i (mod PRIME). This matches {@link #evaluatePolynomial(int[], int)}.</p>
     *
     * <p>This implementation is intentionally small and optimized for your fuzzy-vault constraints
     * (k <= 16, degree <= 8/9). It constructs each Lagrange basis polynomial directly.</p>
     */
    public static int[] reconstructPolynomialCoefficients(@NonNull int[][] points) {
        if (points.length < 2) {
            throw new IllegalArgumentException("points must be of shape [2][n]");
        }
        return reconstructPolynomialCoefficients(points[0], points[1]);
    }

    /**
     * Same as {@link #reconstructPolynomialCoefficients(int[][])} but takes x/y arrays.
     */
    public static int[] reconstructPolynomialCoefficients(@NonNull int[] xPoints, @NonNull int[] yPoints) {
        if (xPoints.length != yPoints.length) {
            throw new IllegalArgumentException("xs and ys must have the same length");
        }
        final int coeffCnt = xPoints.length;
        if (coeffCnt == 0) {
            throw new IllegalArgumentException("Need at least 1 point to interpolate");
        }
        // Result degree is at most (k-1) with k points.
        int[] coeffResults = new int[coeffCnt];
        // For each point i, build the basis polynomial:
        //   L_i(t) = Π_{j!=i} (t - x_j) / (x_i - x_j)
        // in coefficient form (degree k-1), then scale by y_i and accumulate.
        for (int i = 0; i < coeffCnt; i++) {
            int denomProduct = 1;
            // Π_{i!=j} (x_i - x_j) // Product of denominators
            for (int j = 0; j < coeffCnt; j++) {
                if (i != j){
                    int diff = Arithmetic.subMod(xPoints[i], xPoints[j], PRIME);
                    denomProduct = Arithmetic.mulMod(denomProduct, diff, PRIME);
                }
            }
            // y_i * inv(Σ_{j!=i} (x_i - x_j)) (y_i times one over summation of denominators)
            int denomInverse = Arithmetic.invMod(denomProduct, PRIME);
            int productOfYAndDenom = Arithmetic.mulMod(yPoints[i], denomInverse, PRIME);
            // Build numerator polynomial N(t) = Π_{j!=i} (t - x_j)
            // Start with N(t)=1
            int[] numeratorCoeff = new int[1];
            numeratorCoeff[0] = 1;
            for (int j = 0; j < coeffCnt; j++) {
                if (i != j){
                    // (t - x_j) = t + (-x_j)
                    int moddedConst = Arithmetic.mod(-xPoints[j], PRIME);
                    numeratorCoeff = multiplyByMonicLinear(numeratorCoeff, moddedConst);
                }
            }
            // Accumulate productOfYAndDenom * numeratorCoeff into result
            for (int d = 0; d < numeratorCoeff.length; d++) {
                int finalProduct = Arithmetic.mulMod(numeratorCoeff[d], productOfYAndDenom, PRIME);
                coeffResults[d] = Arithmetic.addMod(coeffResults[d], finalProduct, PRIME);
            }
        }
        return coeffResults;
    }

    /**
     * Multiply a polynomial by (t + c) in GF(PRIME).
     *
     * <p>poly is in ascending power order. Output is a new array of length poly.length + 1.</p>
     */
    private static int[] multiplyByMonicLinear(@NonNull int[] polyCoeff, int constant) {
        int[] newPolyCoeff = new int[polyCoeff.length + 1];
        for (int i = 0; i < polyCoeff.length; i++) {
            // constant part (degree 0)
            int constantProduct = Arithmetic.mulMod(polyCoeff[i], constant, PRIME);
            newPolyCoeff[i] = Arithmetic.addMod(newPolyCoeff[i], constantProduct, PRIME);
            // t part (degree 1)
            newPolyCoeff[i + 1] = Arithmetic.addMod(newPolyCoeff[i + 1], polyCoeff[i], PRIME);
        }
        return newPolyCoeff;
    }


    private static int generateRandomInt(int max) {
        return random.nextInt(max);
    }

    /*
    * Deprecated methods below
    * */
    @Deprecated
    public static int[][] generateChaffSetV1(@NonNull int[] coefficients,
                                             @NonNull int[] genuinePointsX,
                                             @NonNull Integer chaffSize){
        int[][] chaffSet = new int[2][chaffSize];
        final int finiteFieldSize = FINITE_FIELD_MAX_VAL + 1;
        List<Integer> candidateChaffX = new ArrayList<>(finiteFieldSize);
        for (int i = 0; i < finiteFieldSize; i++) {
            candidateChaffX.add(i);
        }
        for (int pointsX : genuinePointsX) {
            candidateChaffX.remove(pointsX);
        }

        int done = 0;
        do{
            int randomIdx = generateRandomInt(candidateChaffX.size());
            int chaff_x = candidateChaffX.get(randomIdx);
            chaffSet[0][done] = chaff_x;
            candidateChaffX.remove(chaff_x);

            int unwanted_y = evaluatePolynomial(coefficients, chaff_x);
            do {
                int chaff_y = generateRandomInt(finiteFieldSize);
                if (chaff_y != unwanted_y){
                    chaffSet[1][done++] = chaff_y;
                    break;
                }
            }while(true);

        }while(done < chaffSize);

        return chaffSet;
    }

    @Deprecated
    public static int[][] generateChaffSetV2(@NonNull int[] coefficients,
                                             @NonNull int[] genuinePointsX,
                                             @NonNull Integer chaffSize){
        int[][] chaffSet = new int[2][chaffSize];
        final int finiteFieldSize = FINITE_FIELD_MAX_VAL + 1;
        List<Integer> excludedX = new ArrayList<>(genuinePointsX.length + chaffSize);
        for(int x : genuinePointsX) {
            excludedX.add(x);
        }

        int done = 0;
        do{
            do{
                int randomX = generateRandomInt(finiteFieldSize);
                if(!excludedX.contains(randomX)) {
                    chaffSet[0][done] = randomX;
                    excludedX.add(randomX);
                    break;
                }
            }while(true);

            int chaff_x = chaffSet[0][done];
            int unwanted_y = evaluatePolynomial(coefficients, chaff_x);
            do {
                int chaff_y = generateRandomInt(finiteFieldSize);
                if (chaff_y != unwanted_y){
                    chaffSet[1][done++] = chaff_y;
                    break;
                }
            }while(true);

        }while(done < chaffSize);

        return chaffSet;
    }

    private FuzzyVault(){

    }
}
