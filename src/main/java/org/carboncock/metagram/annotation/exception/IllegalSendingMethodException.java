package org.carboncock.metagram.annotation.exception;

public class IllegalSendingMethodException extends Exception {
    public IllegalSendingMethodException(String error){
        super(error);
    }
}
