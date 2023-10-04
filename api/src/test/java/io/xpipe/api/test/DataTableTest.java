package io.xpipe.api.test;

import io.xpipe.api.DataSource;
import io.xpipe.core.store.DataStoreId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DataTableTest extends ApiTest {

    @BeforeAll
    public static void setupStorage() throws Exception {
        DataSource.create(
                DataStoreId.fromString(":usernames"), "csv", DataTableTest.class.getResource("username.csv"));
    }

    @Test
    public void testGet() {
        var table = DataSource.getById(":usernames").asTable();
        var r = table.read(2);
        var a = 0;
    }
}
