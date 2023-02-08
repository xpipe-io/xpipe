package io.xpipe.cli.test;

import io.xpipe.extension.test.DaemonExtensionTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class MiscCommandTest extends DaemonExtensionTest {

    @Test
    public void version() throws IOException {
        CliTestHelper.exec(List.of("version"));
    }

    @Test
    public void help() throws IOException {
        CliTestHelper.exec(List.of("help", "store"));
        CliTestHelper.exec(List.of("--help"));
        CliTestHelper.exec(List.of("-h"));
        CliTestHelper.exec(List.of("store", "help", "add"));
        CliTestHelper.exec(List.of("store", "--help"));
        CliTestHelper.exec(List.of("store", "add", "--help"));
    }

    @Test
    public void status() throws IOException {
        CliTestHelper.exec(List.of("daemon", "status"));
    }

    @Test
    public void mode() throws IOException {
        CliTestHelper.exec(List.of("daemon", "mode", "background"));
    }
}
