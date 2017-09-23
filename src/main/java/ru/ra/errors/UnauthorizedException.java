package ru.ra.errors;

public final class UnauthorizedException extends Exception {
    public UnauthorizedException(Exception e) {
        super(e);
    }

    public UnauthorizedException() {
        super();
    }

    private static final long serialVersionUID = -119652193576750614L;
}
