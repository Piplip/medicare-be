package com.nkd.medicare.exception;

public class ExpiredTokenException extends RuntimeException{
    public  ExpiredTokenException(){
        super("Error! Token is expired");
    }

    public ExpiredTokenException(String accountID){
        super(accountID);
    }
}
