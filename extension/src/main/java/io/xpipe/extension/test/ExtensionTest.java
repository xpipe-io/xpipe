package io.xpipe.extension.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class ExtensionTest {

    @BeforeAll
    public static void setup() throws Exception {
        ExtensionTestConnector.start();
    }

    @AfterAll
    public static void teardown() throws Exception {
        ExtensionTestConnector.stop();
    }
}
