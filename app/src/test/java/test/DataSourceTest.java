package test;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.impl.InMemoryStore;
import io.xpipe.core.source.*;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.DataSourceProviders;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static io.xpipe.core.source.DataSourceType.*;

public class DataSourceTest extends DaemonTest {

    public static final int READ_LIMIT = Integer.MAX_VALUE;

    @BeforeAll
    public static void setupStorage() throws Exception {
        TestSourcesDatabase.init();
    }

    @Test
    public void testEquality() {
        for (var testSource : TestSourcesDatabase.TEST_SOURCES) {
            var first = testSource.get();
            var second = testSource.get();
            Assertions.assertEquals(first, second);

            var firstHash = first.hashCode();
            var secondHash = second.hashCode();
            Assertions.assertEquals(firstHash, secondHash);

            var firstString = first.toString();
            var secondString = second.toString();
            Assertions.assertEquals(firstString, secondString);
        }

        for (var testStore : TestSourcesDatabase.TEST_STORES) {
            var first = testStore.get();
            var second = testStore.get();
            Assertions.assertEquals(first, second);

            var firstHash = first.hashCode();
            var secondHash = second.hashCode();
            Assertions.assertEquals(firstHash, secondHash);

            var firstString = first.toString();
            var secondString = second.toString();
            Assertions.assertEquals(firstString, secondString);
        }
    }

    @Test
    public void testInputOutput() throws Exception {
        for (io.xpipe.core.source.DataSource<?> testSource :
                TestSourcesDatabase.TEST_SOURCES.stream().map(Supplier::get).toList()) {
            var provider = DataSourceProviders.byDataSourceClass(testSource.getClass());
            System.out.println(String.format("Doing input output for %s", provider.getId()));

            var memoryStore = new InMemoryStore(new byte[0]);
            var memorySource = provider.createDefaultSource(memoryStore);
            if (!memorySource.getFlow().hasInput() || !memorySource.getFlow().hasOutput()) {
                continue;
            }

            try (DataSourceReadConnection dataSourceReadConnection = testSource.openReadConnection()) {
                dataSourceReadConnection.init();
                try (DataSourceConnection out = memorySource.openWriteConnection(WriteMode.REPLACE)) {
                    out.init();
                    dataSourceReadConnection.forward(out);
                }
            }

            isExactlyEqual(testSource, memorySource);
        }
    }

    @Test
    public void testRoundabout() throws Exception {
        for (io.xpipe.core.source.DataSource<?> testSource :
                TestSourcesDatabase.TEST_SOURCES.stream().map(Supplier::get).toList()) {
            var provider = DataSourceProviders.byDataSourceClass(testSource.getClass());
            var compatibleProviders = DataSourceProviders.getAll().stream()
                    .filter(p -> p.getPrimaryType() == provider.getPrimaryType()
                            && p.getCategory() == DataSourceProvider.Category.STREAM)
                    .toList();
            for (DataSourceProvider<?> compatibleProvider : compatibleProviders) {
                var memoryStore = new InMemoryStore();
                var memorySource = compatibleProvider.createDefaultSource(memoryStore);
                if (!memorySource.getFlow().hasOutput()) {
                    continue;
                }

                System.out.println(
                        String.format("Doing translation from %s to %s", provider.getId(), compatibleProvider.getId()));
                try (DataSourceReadConnection dataSourceReadConnection = testSource.openReadConnection()) {
                    try (DataSourceConnection out = memorySource.openWriteConnection(WriteMode.REPLACE)) {
                        dataSourceReadConnection.init();
                        out.init();
                        dataSourceReadConnection.forward(out);
                    }
                }

                isContentEqual(testSource, memorySource);
            }
        }
    }

    @SneakyThrows
    private void isExactlyEqual(DataSource<?> first, DataSource<?> second) {
        isEqual(
                first,
                second,
                (dataStructureNode, dataStructureNode2) ->
                        Assertions.assertEquals(dataStructureNode, dataStructureNode2));
    }

    @SneakyThrows
    private void isContentEqual(DataSource<?> first, DataSource<?> second) {
        isEqual(first, second, (dataStructureNode, o2) -> {
            if (!(dataStructureNode instanceof DataStructureNode)) {
                Assertions.assertEquals(dataStructureNode, o2);
                return;
            }
            DataStructureNode dataStructureNode1 = (DataStructureNode) dataStructureNode;
            DataStructureNode dataStructureNode2 = (DataStructureNode) o2;
            dataStructureNode1.clearMetaAttributes();
            dataStructureNode2.clearMetaAttributes();
            Assertions.assertEquals(dataStructureNode1, dataStructureNode2);
        });
    }

    @SneakyThrows
    private void isEqual(DataSource<?> first, DataSource<?> second, BiConsumer<Object, Object> equalsCheck) {
        if (!first.getFlow().hasInput() || !second.getFlow().hasInput()) {
            return;
        }
        var firstProvider = DataSourceProviders.byDataSourceClass(first.getClass());
        var secondProvider = DataSourceProviders.byDataSourceClass(second.getClass());

        if (first.getType() == TABLE) {
            ArrayNode firstNode = null;
            try (TableReadConnection tableReadConnection = ((TableDataSource<?>) first).openReadConnection()) {
                tableReadConnection.init();
                firstNode = tableReadConnection.readRows(READ_LIMIT);
            }

            ArrayNode secondNode = null;
            try (TableReadConnection tableReadConnection = ((TableDataSource<?>) second).openReadConnection()) {
                tableReadConnection.init();
                secondNode = tableReadConnection.readRows(READ_LIMIT);
            }

            equalsCheck.accept(firstNode, secondNode);
        }

        if (first.getType() == STRUCTURE) {
            DataStructureNode firstNode = null;
            try (StructureReadConnection tableReadConnection = ((StructureDataSource<?>) first).openReadConnection()) {
                tableReadConnection.init();
                firstNode = tableReadConnection.read();
            }

            DataStructureNode secondNode = null;
            try (StructureReadConnection tableReadConnection = ((StructureDataSource<?>) second).openReadConnection()) {
                tableReadConnection.init();
                secondNode = tableReadConnection.read();
            }

            equalsCheck.accept(firstNode, secondNode);
        }

        if (first.getType() == TEXT) {
            String firstNode = null;
            try (TextReadConnection tableReadConnection = ((TextDataSource<?>) first).openReadConnection()) {
                tableReadConnection.init();
                firstNode = tableReadConnection.readAll();
            }

            String secondNode = null;
            try (TextReadConnection tableReadConnection = ((TextDataSource<?>) second).openReadConnection()) {
                tableReadConnection.init();
                secondNode = tableReadConnection.readAll();
            }

            equalsCheck.accept(firstNode, secondNode);
        }
    }
}
