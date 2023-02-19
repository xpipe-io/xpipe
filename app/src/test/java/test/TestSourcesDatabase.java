package test;

import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TestSourcesDatabase {

    public static List<Supplier<DataSource<?>>> TEST_SOURCES = new ArrayList<>();
    public static List<Supplier<DataStore>> TEST_STORES = new ArrayList<>();

    private static StreamDataStore resource(String name) {
        return FileStore.local(System.getProperty("user.dir") + "/src/test/resources/" + name);
    }

    private static void addStore(Supplier<DataStore> source) {
        TEST_STORES.add(source);
    }

    private static void addSource(Supplier<DataSource<?>> source) {
        TEST_SOURCES.add(source);
    }

    @SneakyThrows
    private static void addDefault(String name, Supplier<DataStore> store) {
        TEST_STORES.add(store);

        TEST_SOURCES.add(() -> {
            try {
                return DataSourceProviders.byName(name).orElseThrow().createDefaultSource(store.get());
            } catch (Exception e) {
                throw new RuntimeException("Unknown provider " + name);
            }
        });
    }

    public static void init() {
        addDefault("json", () -> resource("example_2.json"));
        addDefault("csv", () -> resource("business-price-indexes-june-2022-quarter-csv.csv"));
        addDefault("csv", () -> resource("machine-readable-business-employment-data-mar-2022-quarter.csv"));
        addDefault("text", () -> resource("sample-2mb-text-file.txt"));
        addDefault("xml-table", () -> resource("table.xml"));
        addDefault("xml", () -> resource("books.xml"));
    }
}
