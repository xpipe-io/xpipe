package io.xpipe.core.store;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public abstract class JdbcStore implements DataStore {

    String hostname;
    int port;

    public void checkConnect() throws Exception {
        try (Connection con = createConnection()) {
            return;
        }
    }

    public Connection createConnection() throws SQLException {
        return DriverManager.getConnection(toUrl(), toProperties());
    }

    public abstract String toUrl();

    public abstract Properties toProperties();
}
