package test;

import io.xpipe.app.core.mode.OperationMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class DaemonTest {

    @BeforeAll
    static void setup() throws Exception {
        OperationMode.init(new String[0]);
    }

    @AfterAll
    static void teardown() throws Throwable {
        OperationMode.BACKGROUND.finalTeardown();
    }
}
