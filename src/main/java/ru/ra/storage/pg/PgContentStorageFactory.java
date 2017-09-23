package ru.ra.storage.pg;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.postgresql.ds.PGSimpleDataSource;

import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.mchange.v2.c3p0.DataSources;

import ru.ra.errors.TableCreationException;
import ru.ra.storage.ContentStorageInitializationException;
import ru.ra.storage.IStorageUpgrade;
import ru.ra.storage.UpgradeException;

public class PgContentStorageFactory {

    private final String user;
    private final String password;

    public PgContentStorageFactory(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public PgContentStorage create()
            throws ContentStorageInitializationException, UpgradeException {
        try {
            ClassLoader cl = PgContentStorageFactory.class.getClassLoader();
            Class.forName("org.postgresql.Driver");
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setDatabaseName("crossclipboard");
            ds.setServerName("localhost");
            ds.setUser(user);
            ds.setPassword(password);
            Properties props = new Properties();
            props.load(cl.getResourceAsStream("c3p0.properties"));
            PgContentStorage storage =
                new PgContentStorage(DataSources.pooledDataSource(ds, props));
            storage.upgrade(getUpgrades());
            return storage;
        } catch (ClassNotFoundException | IOException | TableCreationException
                | SQLException | InstantiationException
                | IllegalAccessException e) {
            throw new ContentStorageInitializationException(e);
        }
    }

    private List<IStorageUpgrade<PgContentStorage>> cachedUpgrades;

    public synchronized List<? extends IStorageUpgrade<PgContentStorage>> getUpgrades()
            throws IOException, InstantiationException, IllegalAccessException {
        if (cachedUpgrades == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            cachedUpgrades = Lists.newArrayList();
            for (ClassInfo classInfo : ClassPath.from(cl).getTopLevelClasses(
                getClass().getPackage().getName() + ".upgrades")) {
                Class<?> cls = classInfo.load();
                if (IStorageUpgrade.class.isAssignableFrom(cls)) {
                    Class<IStorageUpgrade<PgContentStorage>> upCls =
                        (Class<IStorageUpgrade<PgContentStorage>>) cls;
                    cachedUpgrades.add(upCls.newInstance());
                }
            }
        }
        return cachedUpgrades;
    }
}
