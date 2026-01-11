package com.khchan744.smart_qr_pay.customexception;

public class CredentialsException extends Exception{
    public CredentialsException(String message){
        super(message);
    }

    public CredentialsException(){
        super("Exception occurred in Credentials class.");
    }
}
