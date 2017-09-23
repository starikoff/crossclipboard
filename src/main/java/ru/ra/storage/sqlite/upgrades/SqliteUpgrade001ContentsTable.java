package ru.ra.storage.sqlite.upgrades;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import ru.ra.storage.UpgradeException;
import ru.ra.storage.sqlite.SqliteContentStorage;
import ru.ra.storage.sqlite.SqliteStorageUpgrade;

public class SqliteUpgrade001ContentsTable extends SqliteStorageUpgrade {
    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    protected void doUpgrade(SqliteContentStorage storage)
            throws UpgradeException {
        DataSource ds = storage.getDataSource();
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.addBatch("CREATE TABLE contents (" //
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "token VARCHAR(4096) NOT NULL, "
                    + "created TIMESTAMP NOT NULL DEFAULT current_timestamp, "
                    + "content VARCHAR(" + storage.getSizeLimit() + "))");
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new UpgradeException(e);
        }
    }
}
