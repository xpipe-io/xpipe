package io.xpipe.app.test;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.XPipeServiceProviders;
import io.xpipe.app.util.XPipeSession;
import io.xpipe.core.util.JacksonMapper;
import org.junit.jupiter.api.BeforeAll;

import java.util.UUID;

public class LocalExtensionTest extends ExtensionTest {

    @BeforeAll
    public static void setup() {
        JacksonMapper.initModularized(ModuleLayer.boot());
        XPipeServiceProviders.load(ModuleLayer.boot());
        AppProperties.init();
        XPipeSession.init(UUID.randomUUID());
    }
}
