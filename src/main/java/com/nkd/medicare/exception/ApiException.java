package com.nkd.medicare.exception;

public class ApiException extends RuntimeException{

    public ApiException(String message){
        super(message);
    }

    public ApiException(){
        super("An error occurred! Please try again in a few minutes");
    }
}
