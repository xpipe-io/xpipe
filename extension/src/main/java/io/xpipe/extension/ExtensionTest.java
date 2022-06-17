package io.xpipe.extension;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.CharsetterContext;
import io.xpipe.core.util.JacksonHelper;
import io.xpipe.extension.util.ModuleHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ExtensionTest {

    @BeforeAll
    public static void setup() throws Exception {
        var mod = ModuleLayer.boot().modules().stream()
                .filter(m -> m.getName().contains(".test"))
                .findFirst().orElseThrow();
        var e = ModuleHelper.getEveryoneModule();
        for (var pkg : mod.getPackages()) {
            ModuleHelper.exportAndOpen(pkg, e);
        }

        var extMod = ModuleLayer.boot().modules().stream()
                .filter(m -> m.getName().equals(mod.getName().replace(".test", "")))
                .findFirst().orElseThrow();
        for (var pkg : extMod.getPackages()) {
            ModuleHelper.exportAndOpen(pkg, e);
        }

        JacksonHelper.initClassBased();
        Charsetter.init(CharsetterContext.empty());
    }

    @AfterAll
    public static void teardown() throws Exception {
    }
}
