package ru.ra.errors;

public final class ContentTooLargeException extends Exception {
    private static final long serialVersionUID = -4669774442837595979L;

    public static final int KB = 1024;

    public final int limit;

    public final int size;

    public ContentTooLargeException(int limit, int size) {
        this.limit = limit;
        this.size = size;
    }

    @Override
    public String getMessage() {
        return message(limit, size);
    }

    static String message(int limit, int size) {
        return String.format("Size limit is %d kB, while you provided %.01f kB",
            limit / KB, ((double) size) / KB);
    }
}
