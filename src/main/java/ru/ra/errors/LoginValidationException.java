package ru.ra.errors;

public final class LoginValidationException extends Exception {
    private static final long serialVersionUID = 3673344195426743166L;

    public LoginValidationException(String message) {
        super(message);
    }
}
