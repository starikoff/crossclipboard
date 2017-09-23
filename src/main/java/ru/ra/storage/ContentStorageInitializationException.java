package ru.ra.storage;

public class ContentStorageInitializationException extends Exception {
    private static final long serialVersionUID = 7799971219842643400L;

    public ContentStorageInitializationException(String message) {
        super(message);
    }

    public ContentStorageInitializationException(Exception e) {
        super(e);
    }

    public ContentStorageInitializationException(String message, Exception e) {
        super(message, e);
    }
}
