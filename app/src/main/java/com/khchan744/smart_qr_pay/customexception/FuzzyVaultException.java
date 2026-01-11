package com.khchan744.smart_qr_pay.customexception;

public class FuzzyVaultException extends Exception{
    public FuzzyVaultException(String message) {
        super(message);
    }

    public FuzzyVaultException(){
        super("Unexpected behaviors occurred during Fuzzy Vault operation.");
    }
}
