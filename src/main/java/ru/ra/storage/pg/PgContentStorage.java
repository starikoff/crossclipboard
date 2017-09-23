package ru.ra.storage.pg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.PooledDataSource;

import ru.ra.AuthInfo;
import ru.ra.Environment.IDisposable;
import ru.ra.errors.ContentReadException;
import ru.ra.errors.ContentTooLargeException;
import ru.ra.errors.ContentWriteException;
import ru.ra.errors.TableCreationException;
import ru.ra.storage.IContentStorage;
import ru.ra.storage.ILink;
import ru.ra.storage.INote;
import ru.ra.storage.INote.INoteCoord;
import ru.ra.storage.IStorageUpgrade;
import ru.ra.storage.IUpgradableStorage;
import ru.ra.storage.UpgradeException;

public class PgContentStorage implements IContentStorage,
        IUpgradableStorage<PgContentStorage>, IDisposable {
    private static final Logger log =
        LoggerFactory.getLogger(PgContentStorage.class);

    private final DataSource ds;

    public DataSource getDataSource() {
        return ds;
    }

    public PgContentStorage(final DataSource ds) throws TableCreationException {
        this.ds = ds;
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.addBatch(
                    "CREATE TABLE IF NOT EXISTS properties (key VARCHAR(128) PRIMARY KEY NOT NULL, value VARCHAR(1024))");
                stmt.executeBatch();
            }
        } catch (final SQLException e) {
            throw new TableCreationException(e);
        }
    }

    @Override
    public void dispose() {
        if (ds instanceof PooledDataSource) {
            try {
                ((PooledDataSource) ds).close();
            } catch (final SQLException e) {
                log.error("error closing data source", e);
            }
        }
    }

    private int getVersion() throws ContentReadException, UpgradeException {
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT value FROM properties WHERE key='db.version'")) {
                    int version = -1;
                    if (rs.next()) {
                        String versionStr = rs.getString("value");
                        try {
                            version = Integer.valueOf(versionStr);
                        } catch (NumberFormatException
                                | NullPointerException e) {
                            throw new UpgradeException(version, e);
                        }
                        if (rs.next()) {
                            throw new UpgradeException(getVersion(),
                                "more than one value found for 'db.version' key");
                        }
                    }
                    return version;
                }
            }
        } catch (SQLException e) {
            throw new ContentReadException(e);
        }
    }

    private void setVersion(int version) throws ContentWriteException {
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.addBatch(
                    "DELETE FROM properties WHERE key = 'db.version'");
                stmt.addBatch(
                    "INSERT INTO properties (key, value) VALUES ('db.version', '"
                        + version + "')");
                stmt.executeBatch();
                log.info("postgresql db version updated to " + version);
            }
        } catch (SQLException e) {
            throw new ContentWriteException(e);
        }
    }

    @Override
    public void upgrade(
            List<? extends IStorageUpgrade<PgContentStorage>> upgrades)
                    throws UpgradeException {
        Collections.sort(upgrades,
            new Comparator<IStorageUpgrade<PgContentStorage>>() {
                @Override
                public int compare(IStorageUpgrade<PgContentStorage> o1,
                        IStorageUpgrade<PgContentStorage> o2) {
                    return o1.getVersion() - o2.getVersion();
                }
            });
        try {
            int currentVersion = getVersion();
            for (IStorageUpgrade<PgContentStorage> upgrade : upgrades) {
                int upgradeVersion = upgrade.getVersion();
                if (currentVersion < upgradeVersion) {
                    upgrade.upgrade(this);
                    log.info("postgresql db upgrade to version "
                        + upgradeVersion + " passed");
                    setVersion(upgradeVersion);
                }
            }
        } catch (ContentReadException | ContentWriteException e) {
            throw new UpgradeException(e);
        }
    }

    @Override
    @Nonnull
    public List<INote> get(final AuthInfo auth) throws ContentReadException {
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(//
                "SELECT " //
                    + "  contents.id AS content_id, "
                    + "  contents.created AS created, "//
                    + "  contents.content AS content, "// 
                    + "  urls.id AS url_id, "//
                    + "  urls.title AS url_title, "//
                    + "  urls.favicon AS url_favicon " //
                    + "FROM contents "//
                    + "LEFT OUTER JOIN urls "//
                    + "ON contents.id=urls.id " //
                    + "WHERE contents.token=?" //
                    + "ORDER BY contents.created ASC")) {
                pstmt.setString(1, auth.getToken());
                List<INote> result = new ArrayList<>();
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        result.add(parseNote(auth, rs));
                    }
                    return result;
                }
            }
        } catch (final SQLException e) {
            throw new ContentReadException(e);
        }
    }

    private static INote parseNote(final AuthInfo auth, ResultSet rs)
            throws SQLException {
        final int contentId = rs.getInt("content_id");
        final Date created = rs.getTimestamp("created");
        final String content = rs.getString("content");
        rs.getInt("url_id");
        boolean isUrl = !rs.wasNull();
        final INoteCoord noteCoord = new INoteCoord() {
            @Override
            public AuthInfo getOwner() {
                return auth;
            }

            @Override
            public String getId() {
                return String.valueOf(contentId);
            }
        };
        INote note;
        if (isUrl) {
            final String title = rs.getString("url_title");
            final String favicon = rs.getString("url_favicon");
            note = new ILink() {
                @Override
                public Date getCreationDate() {
                    return created;
                }

                @Override
                public INoteCoord getCoord() {
                    return noteCoord;
                }

                @Override
                public String asText() {
                    return content;
                }

                @Override
                public String getTitle() {
                    return title;
                }

                @Override
                public String getFaviconUrl() {
                    return favicon;
                }
            };
        } else {
            note = new INote() {
                @Override
                public Date getCreationDate() {
                    return created;
                }

                @Override
                public INoteCoord getCoord() {
                    return noteCoord;
                }

                @Override
                public String asText() {
                    return content;
                }
            };
        }
        return note;
    }

    @Override
    public void remove(INoteCoord noteCoord) throws ContentWriteException {
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                "DELETE FROM contents WHERE token=? AND id=?")) {
                pstmt.setString(1, noteCoord.getOwner().getToken());
                pstmt.setInt(2, Integer.valueOf(noteCoord.getId()));
                pstmt.executeUpdate();
            }
        } catch (final SQLException e) {
            throw new ContentWriteException(e);
        }
    }

    @Override
    public int getContentSize(AuthInfo auth) throws ContentReadException {
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT SUM(LENGTH(token))+SUM(LENGTH(content))+SUM(LENGTH(title))+SUM(LENGTH(favicon)) AS total " //
                    + "FROM contents LEFT OUTER JOIN urls " //
                    + "ON contents.id=urls.id " //
                    + "WHERE contents.token=?")) {
                pstmt.setString(1, auth.getToken());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    } else {
                        return 0;
                    }
                }
            }
        } catch (final SQLException e) {
            throw new ContentReadException(e);
        }
    }

    @Override
    public INote get(INoteCoord noteCoord) throws ContentReadException {
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(//
                "SELECT " //
                    + "  contents.id AS content_id, "
                    + "  contents.created AS created, "//
                    + "  contents.content AS content, "// 
                    + "  urls.id AS url_id, "//
                    + "  urls.title AS url_title, "//
                    + "  urls.favicon AS url_favicon "//
                    + "FROM contents "//
                    + "LEFT OUTER JOIN urls "//
                    + "ON contents.id=urls.id " //
                    + "WHERE contents.token=? AND contents.id = ? " //
                    + "LIMIT 1")) {
                pstmt.setString(1, noteCoord.getOwner().getToken());
                pstmt.setInt(2, Integer.parseInt(noteCoord.getId()));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return parseNote(noteCoord.getOwner(), rs);
                    } else {
                        return null;
                    }
                }
            }
        } catch (final SQLException e) {
            throw new ContentReadException(e);
        }
    }

    @Override
    public int getSizeLimit() {
        return 64 * ContentTooLargeException.KB;
    }

    @Override
    public INote addNote(final AuthInfo auth, final String content)
            throws ContentWriteException {
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO contents(token, content) VALUES (?, ?) RETURNING id, created;--",
                Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, auth.getToken());
                pstmt.setString(2, content);
                pstmt.executeUpdate();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    final int id = rs.getInt("id");
                    final Timestamp created = rs.getTimestamp("created");
                    final INoteCoord noteCoord = new INoteCoord() {
                        @Override
                        public AuthInfo getOwner() {
                            return auth;
                        }

                        @Override
                        public String getId() {
                            return String.valueOf(id);
                        }
                    };
                    return new INote() {
                        @Override
                        public Date getCreationDate() {
                            return created;
                        }

                        @Override
                        public INoteCoord getCoord() {
                            return noteCoord;
                        }

                        @Override
                        public String asText() {
                            return content;
                        }
                    };
                } else {
                    throw new ContentWriteException("could not get id");
                }
            }
        } catch (final SQLException e) {
            throw new ContentWriteException(e);
        }
    }

    @Override
    public void makeLink(String id, String title, String favicon)
            throws ContentWriteException {
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO urls(id, title, favicon) VALUES (?, ?, ?)")) {
                pstmt.setInt(1, Integer.parseInt(id));
                pstmt.setString(2, title);
                pstmt.setString(3, favicon);
                pstmt.executeUpdate();
            }
        } catch (final SQLException e) {
            throw new ContentWriteException(e);
        }
    }
}
