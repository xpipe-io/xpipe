import com.fasterxml.jackson.databind.Module;
import io.xpipe.ext.jdbc.JdbcDialect;
import io.xpipe.ext.jdbc.JdbcJacksonModule;
import io.xpipe.ext.jdbc.mysql.MysqlStoreProvider;
import io.xpipe.ext.jdbc.postgres.PostgresDialect;
import io.xpipe.ext.jdbc.postgres.PostgresPsqlAction;
import io.xpipe.ext.jdbc.postgres.PostgresStoreProvider;
import io.xpipe.ext.jdbc.source.JdbcDynamicQuerySource;
import io.xpipe.ext.jdbc.source.JdbcStaticQuerySource;
import io.xpipe.ext.jdbc.source.JdbcTableSourceProvider;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.DataStoreActionProvider;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.util.ModuleLayerLoader;

import java.sql.Driver;

open module io.xpipe.ext.jdbc {
    exports io.xpipe.ext.jdbc;
    exports io.xpipe.ext.jdbc.auth;
    exports io.xpipe.ext.jdbc.source;
    exports io.xpipe.ext.jdbc.address;
    exports io.xpipe.ext.jdbc.mysql;
    exports io.xpipe.ext.jdbc.postgres;

    requires io.xpipe.core;
    requires io.xpipe.extension;
    requires static jarchivelib;
    requires static lombok;
    requires java.sql;
    requires com.fasterxml.jackson.databind;
    requires static net.synedra.validatorfx;
    requires javafx.base;
    requires javafx.graphics;
    requires com.microsoft.sqlserver.jdbc;
    requires org.postgresql.jdbc;
    requires io.xpipe.beacon;
    // requires mysql.connector.java;

    uses Driver;
    uses io.xpipe.ext.jdbc.JdbcDialect;

    provides ModuleLayerLoader with JdbcDialect.Loader;
    provides JdbcDialect with PostgresDialect;
    provides Module with
            JdbcJacksonModule;
    provides DataSourceProvider with
            JdbcDynamicQuerySource.Provider,
            JdbcStaticQuerySource.Provider,
            JdbcTableSourceProvider;
    provides DataStoreProvider with
            MysqlStoreProvider,
            PostgresStoreProvider;
    provides DataStoreActionProvider with
            PostgresPsqlAction;
}
