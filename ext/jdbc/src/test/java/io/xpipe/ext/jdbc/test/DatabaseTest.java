package io.xpipe.ext.jdbc.test;

import io.xpipe.ext.jdbc.JdbcConfig;
import io.xpipe.ext.jdbc.JdbcDialect;
import io.xpipe.ext.jdbc.JdbcHelper;
import io.xpipe.ext.jdbc.test.item.DatabaseItem;
import io.xpipe.extension.util.DaemonExtensionTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseTest extends DaemonExtensionTest {

    @BeforeEach
    public void prepare() throws SQLException {
        System.setProperty(JdbcConfig.CLOSE_CONNECTIONS_PROPERTY, "true");
        for (DatabaseItem item : DatabaseItem.getAll().map(testDatabaseItemNamed -> testDatabaseItemNamed.getPayload()).toList()) {
            try (Connection connection = item.getStore().createConnection()) {
                connection.setCatalog(item.getDatabase());
                var type = JdbcDialect.getDialect(connection);
                type.enableConstraints(connection);

                var tables = JdbcHelper.listAvailableTables(connection);
                Assertions.assertEquals(tables.size(), item.getExpectedTableCount());
            }
        }
    }

    @AfterEach
    public void check() throws SQLException {
        for (DatabaseItem item : DatabaseItem.getAll().map(testDatabaseItemNamed -> testDatabaseItemNamed.getPayload()).toList()) {
            var tables = JdbcHelper.listAvailableTables(item.getStore(), item.getDatabase());
            Assertions.assertEquals(tables.size(), item.getExpectedTableCount());
        }
    }
}
