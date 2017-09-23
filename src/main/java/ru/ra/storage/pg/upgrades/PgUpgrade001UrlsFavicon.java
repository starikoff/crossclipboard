package ru.ra.storage.pg.upgrades;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import ru.ra.storage.UpgradeException;
import ru.ra.storage.pg.PgContentStorage;
import ru.ra.storage.pg.PgStorageUpgrade;

public class PgUpgrade001UrlsFavicon extends PgStorageUpgrade {
    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    protected void doUpgrade(PgContentStorage storage) throws UpgradeException {
        DataSource ds = storage.getDataSource();
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.addBatch("ALTER TABLE urls ADD COLUMN favicon VARCHAR(1024)");
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new UpgradeException(e);
        }
    }
}
