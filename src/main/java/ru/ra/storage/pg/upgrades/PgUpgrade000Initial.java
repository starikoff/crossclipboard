package ru.ra.storage.pg.upgrades;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import ru.ra.storage.UpgradeException;
import ru.ra.storage.pg.PgContentStorage;
import ru.ra.storage.pg.PgStorageUpgrade;

public class PgUpgrade000Initial extends PgStorageUpgrade {
    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    protected void doUpgrade(PgContentStorage storage)
            throws UpgradeException {
        DataSource ds = storage.getDataSource();
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.addBatch("CREATE TABLE contents ("
                    + "id SERIAL PRIMARY KEY, "
                    + "token VARCHAR(4096) NOT NULL, "
                    + "created TIMESTAMP NOT NULL DEFAULT current_timestamp, "
                    + "content VARCHAR("
                    + storage.getSizeLimit() + "))");
                stmt.addBatch("CREATE INDEX contents_time_idx ON contents(created)");
                stmt.addBatch("CREATE INDEX contents_token_time_idx ON contents(token, created)");
                stmt.addBatch("CREATE TABLE urls (" //
                    + "id INT NOT NULL, " //
                    + "title VARCHAR(1024), " // 
                    + "FOREIGN KEY (id) REFERENCES contents(id) ON DELETE CASCADE)");
                stmt.addBatch("CREATE INDEX urls_id_idx ON urls(id)");
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new UpgradeException(e);
        }
    }
}
