package ru.ra.storage.pg;

import ru.ra.storage.IStorageUpgrade;
import ru.ra.storage.UpgradeException;

public abstract class PgStorageUpgrade implements
        IStorageUpgrade<PgContentStorage> {
    @Override
    public final void upgrade(PgContentStorage storage)
            throws UpgradeException {
        doUpgrade(storage);
    }

    protected abstract void doUpgrade(PgContentStorage storage)
            throws UpgradeException;
}
