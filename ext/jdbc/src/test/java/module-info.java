module io.xpipe.ext.jdbc.test {
    exports io.xpipe.ext.jdbc.test;
    exports io.xpipe.ext.jdbc.test.item;

    requires io.xpipe.ext.jdbcx;
    requires org.hamcrest;
    requires io.xpipe.core;
    requires io.xpipe.api;
    requires io.xpipe.ext.jdbc;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires java.sql;
    requires io.xpipe.extension;
    requires io.xpipe.ext.base;
    requires static lombok;
}
