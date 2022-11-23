package io.xpipe.extension.util;

import io.xpipe.core.util.JacksonMapper;
import io.xpipe.extension.XPipeServiceProviders;
import org.junit.jupiter.api.BeforeAll;

public class LocalExtensionTest extends ExtensionTest {

    @BeforeAll
    public static void setup() throws Exception {
        JacksonMapper.initModularized(ModuleLayer.boot());
        XPipeServiceProviders.load(ModuleLayer.boot());
    }
}
