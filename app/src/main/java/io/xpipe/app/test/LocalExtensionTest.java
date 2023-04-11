package io.xpipe.app.test;

import io.xpipe.app.ext.XPipeServiceProviders;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.app.util.XPipeSession;
import org.junit.jupiter.api.BeforeAll;

import java.util.UUID;

public class LocalExtensionTest extends ExtensionTest {

    @BeforeAll
    public static void setup() throws Exception {
        JacksonMapper.initModularized(ModuleLayer.boot());
        XPipeServiceProviders.load(ModuleLayer.boot());
        XPipeSession.init(UUID.randomUUID());
    }
}
