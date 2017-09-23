package ru.ra.storage.sqlite;

import ru.ra.storage.IStorageUpgrade;
import ru.ra.storage.UpgradeException;

public abstract class SqliteStorageUpgrade implements
        IStorageUpgrade<SqliteContentStorage> {
    @Override
    public final void upgrade(SqliteContentStorage storage)
            throws UpgradeException {
        doUpgrade(storage);
    }

    protected abstract void doUpgrade(SqliteContentStorage storage)
            throws UpgradeException;
}
