package ru.ra.storage.sqlite.upgrades;

import ru.ra.storage.UpgradeException;
import ru.ra.storage.sqlite.SqliteContentStorage;
import ru.ra.storage.sqlite.SqliteStorageUpgrade;

public class SqliteUpgrade000Initial extends SqliteStorageUpgrade {
    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    protected void doUpgrade(SqliteContentStorage storage)
            throws UpgradeException {
    }
}
