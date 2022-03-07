package io.xpipe.api.test;

import io.xpipe.api.DataSource;
import io.xpipe.core.source.DataSourceId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class DataTableTest extends DaemonControl {

    @BeforeAll
    public static void setupStorage() throws Exception {
        DataSource.create(DataSourceId.fromString(":usernames"), "csv", Map.of(), DataTableTest.class.getResource("username.csv"));
    }

    @Test
    public void testGet() {
        var table = DataSource.getById(":usernames").asTable();
        var r = table.read(2);
        var a = 0;
    }
}
