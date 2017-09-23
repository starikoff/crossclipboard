package ru.ra.storage.sqlite.upgrades;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import ru.ra.storage.UpgradeException;
import ru.ra.storage.sqlite.SqliteContentStorage;
import ru.ra.storage.sqlite.SqliteStorageUpgrade;

public class SqliteUpgrade002ContentTimeIndex extends SqliteStorageUpgrade {
    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    protected void doUpgrade(SqliteContentStorage storage)
            throws UpgradeException {
        DataSource ds = storage.getDataSource();
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.addBatch(
                    "CREATE INDEX contents_time_idx ON contents(created)");
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new UpgradeException(e);
        }
    }
}
