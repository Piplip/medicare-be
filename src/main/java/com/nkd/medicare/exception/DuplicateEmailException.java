package com.nkd.medicare.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(){
        super("Email already used. Please try another email or login");
    }
}
