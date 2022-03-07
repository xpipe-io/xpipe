package io.xpipe.api.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class DaemonControl {

    @BeforeAll
    public static void setup() throws Exception {
        ConnectionFactory.start();
    }

    @AfterAll
    public static void teardown() throws Exception {
        ConnectionFactory.stop();
    }
}
