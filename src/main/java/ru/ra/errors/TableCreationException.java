package ru.ra.errors;

public final class TableCreationException extends Exception {
    private static final long serialVersionUID = 7601413196595319020L;

    public TableCreationException(Throwable e) {
        super(e);
    }
}