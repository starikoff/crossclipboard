package ru.ra.errors;

public final class ContentWriteException extends Exception {
    private static final long serialVersionUID = 2520825996100238456L;

    public ContentWriteException(Throwable e) {
        super(e);
    }

    public ContentWriteException(String msg) {
        super(msg);
    }
}