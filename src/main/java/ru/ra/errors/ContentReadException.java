package ru.ra.errors;

public final class ContentReadException extends Exception {
    private static final long serialVersionUID = 2520825996100238456L;

    public ContentReadException(Throwable e) {
        super(e);
    }
}