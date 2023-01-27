package io.xpipe.ext.jdbc.test;

import io.xpipe.core.impl.InMemoryStore;
import io.xpipe.ext.jdbc.source.JdbcDynamicQuerySource;
import io.xpipe.ext.jdbc.source.JdbcStaticQuerySource;
import io.xpipe.ext.jdbc.source.JdbcTableSource;
import io.xpipe.ext.jdbc.test.item.DatabaseItem;
import io.xpipe.extension.util.DaemonExtensionTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ReadTests extends DaemonExtensionTest {

    @ParameterizedTest
    @MethodSource(value = "io.xpipe.ext.jdbc.test.item.DatabaseItem#getTableItems")
    public void testTableSourceRead(DatabaseItem.TableItem item) throws IOException {
        var source = getSource(JdbcTableSource.builder()
                        .store(item.getStore())
                        .database(item.getDatabase())
                        .table(item.getTable())
                        .build())
                .asTable();
        var count = source.countAndDiscard();
        System.out.println(String.format("Read table %s with length %s", item.getTable(), count));
    }

    @ParameterizedTest
    @MethodSource(value = "io.xpipe.ext.jdbc.test.item.DatabaseItem#getTableItems")
    public void testTableSourceReadOrder(DatabaseItem.TableItem item) throws IOException {
        var source = getSource(JdbcTableSource.builder()
                        .store(item.getStore())
                        .database(item.getDatabase())
                        .table(item.getTable())
                        .build())
                .asTable();
        var first = source.read(50);
        source = getSource(JdbcTableSource.builder()
                        .store(item.getStore())
                        .database(item.getDatabase())
                        .table(item.getTable())
                        .build())
                .asTable();
        var second = source.read(50);
        Assertions.assertEquals(first, second);
    }

    @ParameterizedTest
    @MethodSource(value = "io.xpipe.ext.jdbc.test.item.DatabaseItem#getTableItems")
    public void testStaticQuerySourceRead(DatabaseItem.TableItem item) throws IOException {
        var query = "SELECT * FROM " + item.getTable();
        var source = getSource(JdbcStaticQuerySource.builder()
                        .store(item.getStore())
                        .database(item.getDatabase())
                        .query(query)
                        .build())
                .asTable();
        var count = source.countAndDiscard();
        System.out.println(String.format("Read table %s with length %s", item.getTable(), count));
    }

    @ParameterizedTest
    @MethodSource(value = "io.xpipe.ext.jdbc.test.item.DatabaseItem#getTableItems")
    public void testDynamicQuerySourceRead(DatabaseItem.TableItem item) throws Exception {
        var query = "SELECT * FROM " + item.getTable();
        var text = getSource("text", new InMemoryStore(query.getBytes(StandardCharsets.UTF_8)));
        var source = getSource(JdbcDynamicQuerySource.builder()
                        .store(item.getStore())
                        .database(item.getDatabase())
                        .source(text.getInternalSource().asNeeded())
                        .build())
                .asTable();
        var count = source.countAndDiscard();
        System.out.println(String.format("Read table %s with length %s", item.getTable(), count));
    }
}
