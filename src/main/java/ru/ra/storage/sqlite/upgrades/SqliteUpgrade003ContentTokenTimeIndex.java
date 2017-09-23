package ru.ra.storage.sqlite.upgrades;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import ru.ra.storage.UpgradeException;
import ru.ra.storage.sqlite.SqliteContentStorage;
import ru.ra.storage.sqlite.SqliteStorageUpgrade;

public class SqliteUpgrade003ContentTokenTimeIndex
        extends SqliteStorageUpgrade {
    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    protected void doUpgrade(SqliteContentStorage storage)
            throws UpgradeException {
        DataSource ds = storage.getDataSource();
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.addBatch(
                    "CREATE INDEX contents_token_time_idx ON contents(token, created)");
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new UpgradeException(e);
        }
    }
}
