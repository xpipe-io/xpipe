package io.xpipe.extension.util;

import io.xpipe.api.DataSource;
import io.xpipe.api.util.XPipeDaemonController;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.extension.XPipeServiceProviders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class DaemonExtensionTest extends ExtensionTest {

    public static DataSource getSource(String type, DataStore store) {
        return DataSource.create(null, type, store);
    }

    public static DataSource getSource(String type, String file) {
        return DataSource.create(null, type, getResource(file));
    }

    public static DataSource getSource(io.xpipe.core.source.DataSource<?> source) {
        return DataSource.create(null, source);
    }

    @BeforeAll
    public static void setup() throws Exception {
        JacksonMapper.initModularized(ModuleLayer.boot());
        XPipeServiceProviders.load(ModuleLayer.boot());
        XPipeDaemonController.start();
    }

    @AfterAll
    public static void teardown() throws Exception {
        XPipeDaemonController.stop();
    }
}
