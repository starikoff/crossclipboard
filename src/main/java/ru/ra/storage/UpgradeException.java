package ru.ra.storage;

public class UpgradeException extends Exception {

    public UpgradeException(Exception cause) {
        super(cause);
    }

    public UpgradeException(int version, Exception cause) {
        super("error upgrading db to version " + version, cause);
    }

    public UpgradeException(int version, String message) {
        super("error upgrading db to version " + version + "; message: "
            + message);
    }
}
