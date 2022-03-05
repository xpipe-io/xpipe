package io.xpipe.api.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class DaemonControl {

    @BeforeAll
    static void setup() throws Exception {
        ConnectionFactory.start();
    }

    @AfterAll
    static void teardown() throws Exception {
        ConnectionFactory.stop();
    }
}
