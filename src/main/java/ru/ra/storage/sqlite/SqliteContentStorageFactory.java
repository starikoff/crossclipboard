package ru.ra.storage.sqlite;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.Encoding;
import org.sqlite.SQLiteDataSource;

import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import ru.ra.errors.TableCreationException;
import ru.ra.storage.ContentStorageInitializationException;
import ru.ra.storage.IStorageUpgrade;
import ru.ra.storage.UpgradeException;

public class SqliteContentStorageFactory {

    private final String user;
    private final String password;

    public SqliteContentStorageFactory(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public SqliteContentStorage create()
            throws ContentStorageInitializationException, UpgradeException {
        try {
            String dataRootPath = System.getProperty("webAppDataRoot");

            Class.forName("org.sqlite.JDBC");
            final File dataFolder =
                new File(new File(dataRootPath), "data/sqlite");
            if (!dataFolder.isDirectory() && !dataFolder.mkdirs()) {
                throw new IOException(
                    "failed creating data folder " + dataFolder);
            }
            final SQLiteConfig config = new SQLiteConfig();
            config.setEncoding(Encoding.UTF_8);
            final SQLiteDataSource ds = new SQLiteDataSource(config);
            ds.setUrl("jdbc:sqlite:" + dataFolder.getCanonicalPath()
                + "/crossclipboard");
            SqliteContentStorage sqliteContentStorage =
                new SqliteContentStorage(ds);
            sqliteContentStorage.upgrade(getUpgrades());
            return sqliteContentStorage;
        } catch (ClassNotFoundException | IOException | TableCreationException
                | InstantiationException | IllegalAccessException e) {
            throw new ContentStorageInitializationException(e);
        }
    }

    private List<IStorageUpgrade<SqliteContentStorage>> cachedUpgrades;

    public synchronized List<? extends IStorageUpgrade<SqliteContentStorage>> getUpgrades()
            throws IOException, InstantiationException, IllegalAccessException {
        if (cachedUpgrades == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            cachedUpgrades = Lists.newArrayList();
            for (ClassInfo classInfo : ClassPath.from(cl).getTopLevelClasses(
                SqliteContentStorageFactory.class.getPackage().getName()
                    + ".upgrades")) {
                Class<?> cls = classInfo.load();
                if (IStorageUpgrade.class.isAssignableFrom(cls)) {
                    Class<IStorageUpgrade<SqliteContentStorage>> upCls =
                        (Class<IStorageUpgrade<SqliteContentStorage>>) cls;
                    cachedUpgrades.add(upCls.newInstance());
                }
            }
        }
        return cachedUpgrades;
    }
}
