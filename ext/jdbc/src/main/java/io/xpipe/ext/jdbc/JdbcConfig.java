package io.xpipe.ext.jdbc;

public class JdbcConfig {

    public static final String CLOSE_CONNECTIONS_PROPERTY = "io.xpipe.jdbc.closeConnections";

    public static boolean closeConnections() {
        if (System.getProperty(CLOSE_CONNECTIONS_PROPERTY) != null) {
            return Boolean.parseBoolean(System.getProperty(CLOSE_CONNECTIONS_PROPERTY));
        }
        return true;
    }
}
