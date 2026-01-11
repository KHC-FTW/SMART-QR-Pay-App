package com.khchan744.smart_qr_pay.customexception;

public class PayloadException extends Exception{
    public PayloadException(String message){
        super(message);
    }

    public PayloadException(){
        super("Unexpected behaviors occurred during Payload operation.");
    }
}
