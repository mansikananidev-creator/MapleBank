package com.unibank.bankingSystem.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
