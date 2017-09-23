package ru.ra.errors;

public final class PasswordValidationException extends Exception {
    private static final long serialVersionUID = -7359082658701456197L;

    public PasswordValidationException(String message) {
        super(message);
    }
}
