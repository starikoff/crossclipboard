package ru.ra;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.ra.storage.ContentStorageInitializationException;
import ru.ra.storage.IContentStorage;
import ru.ra.storage.ILogicalStorage;
import ru.ra.storage.LogicalStorage;
import ru.ra.storage.UpgradeException;
import ru.ra.storage.pg.PgContentStorage;
import ru.ra.storage.pg.PgContentStorageFactory;
import ru.ra.util.Futurizer;

@WebListener
public class ContextListener implements ServletContextListener {
    private final Logger log = LoggerFactory.getLogger(ContextListener.class);

    @Override
    public void contextDestroyed(final ServletContextEvent evt) {
        Environment.dispose();
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        Thread
            .setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    log.error("uncaught exception", e);
                }
            });
        try {
            String globalConfigRoot = System.getProperty("configurationRoot");

            Properties authProps = new Properties();
            FileInputStream authConfigIn =
                    new FileInputStream(new File(new File(globalConfigRoot),
                            "crossclipboard-auth.properties"));
            authProps.load(authConfigIn);

            String tokenHashSalt = authProps.getProperty("auth.token-hash-salt");
            String fullTokenSalt = authProps.getProperty("auth.full-token-salt");
            String encryptionPassword = authProps.getProperty("auth.encryption-password");

            Environment.publish(IAuthService.class, new BaseAuthService(tokenHashSalt, fullTokenSalt, encryptionPassword));

            Properties dbProps = new Properties();
            FileInputStream dbConfigIn =
                new FileInputStream(new File(new File(globalConfigRoot),
                    "crossclipboard-db.properties"));
            dbProps.load(dbConfigIn);

            String user = dbProps.getProperty("db.user");
            String password = dbProps.getProperty("db.password");

            PgContentStorage storage =
                new PgContentStorageFactory(user, password).create();
            //            SqliteContentStorage storage =
            //                new SqliteContentStorageFactory(user, password).create();
            Environment.publish(IContentStorage.class, storage);
            Environment.publish(ILogicalStorage.class, new LogicalStorage(
                storage));

            Environment.publish(Futurizer.class, new Futurizer());

            log.info("application started");
        } catch (ContentStorageInitializationException | UpgradeException
                | IOException e) {
            throw new RuntimeException("error initializing db", e);
        }
    }
}
