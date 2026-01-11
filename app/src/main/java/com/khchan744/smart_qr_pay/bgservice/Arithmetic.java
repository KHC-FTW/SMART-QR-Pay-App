package com.khchan744.smart_qr_pay.bgservice;

import androidx.annotation.NonNull;

import java.util.Map;

public class Arithmetic {

    public static final String QUANTIZED_VAL_KEY = "quantizedVal";
    public static final String OFFSET_VAL_KEY = "offset";

    public static Map<String, Object> quantizeRefValue(@NonNull Long refVal,
                                                       @NonNull Integer tolerance,
                                                       @NonNull Boolean isTwoSided){
        if (isTwoSided){
            refVal -= tolerance;
            tolerance *= 2;
        }
        double divisionResult = (double)refVal / tolerance;
        double refCorrected = Math.floor(divisionResult) + 0.5;
        double offset = refCorrected - divisionResult;
        long quantizedVal = Math.round(refCorrected) * (long)tolerance;
        return Map.of(
                QUANTIZED_VAL_KEY, quantizedVal,
                OFFSET_VAL_KEY, offset
        );
    }

    public static Map<String, Double> quantizeRefValue(@NonNull Double refVal,
                                                       @NonNull Double tolerance,
                                                       @NonNull Boolean isTwoSided){
        if (isTwoSided){
            refVal -= tolerance;
            tolerance *= 2;
        }
        double divisionResult = refVal / tolerance;
        double refCorrected = Math.floor(divisionResult) + 0.5;
        double offset = refCorrected - divisionResult;
        double quantizedVal = Math.round(refCorrected) * tolerance;
        return Map.of(
                QUANTIZED_VAL_KEY, quantizedVal,
                OFFSET_VAL_KEY, offset
        );
    }

    public static Map<String, Double> quantizeRefValue(@NonNull Float refVal,
                                                       @NonNull Float tolerance,
                                                       @NonNull Boolean isTwoSided){
        if (isTwoSided){
            refVal -= tolerance;
            tolerance *= 2;
        }
        double divisionResult = refVal / tolerance;
        double refCorrected = Math.floor(divisionResult) + 0.5;
        double offset = refCorrected - divisionResult;
        double quantizedVal = Math.round(refCorrected) * tolerance;
        return Map.of(
                QUANTIZED_VAL_KEY, quantizedVal,
                OFFSET_VAL_KEY, offset
        );
    }

    public static long quantizeOtherVal(@NonNull Long otherVal,
                                        @NonNull Integer tolerance,
                                        @NonNull Boolean isTwoSided,
                                        @NonNull Double offset){
        if (isTwoSided) tolerance *= 2;
        double divisionResult = (double)otherVal / tolerance;
        double correctedVal = divisionResult + offset;
        return Math.round(correctedVal) * (long)tolerance;
    }

    public static double quantizeOtherVal(@NonNull Double otherVal,
                                          @NonNull Double tolerance,
                                          @NonNull Boolean isTwoSided,
                                          @NonNull Double offset){
        if (isTwoSided) tolerance *= 2;
        double divisionResult = otherVal / tolerance;
        double correctedVal = divisionResult + offset;
        return Math.round(correctedVal) * tolerance;
    }

    public static double quantizeOtherVal(@NonNull Float rawVal,
                                          @NonNull Float tolerance,
                                          @NonNull Boolean isTwoSided,
                                          @NonNull Double offset){
        if (isTwoSided) tolerance *= 2;
        double divisionResult = rawVal / tolerance;
        double correctedVal = divisionResult + offset;
        return Math.round(correctedVal) * tolerance;
    }

    public static boolean hasExceededTolerance(@NonNull Long val1,
                                               @NonNull Long val2,
                                               @NonNull Integer tolerance){
        return Math.abs(val1 - val2) > tolerance;
    }

    public static boolean hasExceededTolerance(@NonNull Double val1,
                                               @NonNull Double val2,
                                               @NonNull Double tolerance){
        return Math.abs(val1 - val2) > tolerance;
    }

    public static boolean hasExceededTolerance(@NonNull Float val1,
                                               @NonNull Float val2,
                                               @NonNull Float tolerance){
        return Math.abs(val1 - val2) > tolerance;
    }

    public static int mod(int value, int prime) {
        int result = value % prime;
        return result < 0 ? result + prime : result;
    }

    public static int addMod(int a, int b, int prime) {
        int sum = a + b;
        return sum >= prime ? sum - prime : sum;
    }

    public static int subMod(int a, int b, int prime) {
        int difference = a - b;
        return difference < 0 ? difference + prime : difference;
    }

    public static int mulMod(int a, int b, int prime) {
        return (int) (((long) a * (long) b) % prime);
    }

    public static int powMod(int base, int exponent, int prime) {
        long result = 1;
        long b = mod(base, prime);
        int e = exponent;
        while (e > 0) {
            if ((e & 1) == 1) {
                result = (result * b) % prime;
            }
            b = (b * b) % prime;
            e >>>= 1;
        }
        return (int) result;
    }

    public static int invMod(int a, int prime) {
        int aa = mod(a, prime);
        if (aa == 0) {
            throw new ArithmeticException("No modular inverse for 0 in GF(" + prime + ")");
        }
        // Fermat's little theorem: a^(p-2) mod p
        return powMod(aa, prime - 2, prime);
    }


    private Arithmetic(){

    }
}
