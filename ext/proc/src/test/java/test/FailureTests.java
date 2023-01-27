package test;

import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.extension.util.LocalExtensionTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

public class FailureTests extends LocalExtensionTest {

    @ParameterizedTest
    @MethodSource("test.item.ShellTestItem#getAll")
    public void testFailedShellOpener(ShellProcessControl pc) throws Exception {
        pc.start();
        var sub = pc.subShell(
                pc.getShellType().executeCommandWithShell(pc.getShellType().getEchoCommand("hi", false)));

        Assertions.assertThrows(IOException.class, () -> {
            sub.start();
        });

        Assertions.assertFalse(pc.isRunning());
        Assertions.assertFalse(sub.isRunning());
    }

    @ParameterizedTest
    @MethodSource("test.item.ShellTestItem#getAll")
    public void testInvalidShellOpenerCommand(ShellProcessControl pc) throws Exception {
        pc.start();
        var sub = pc.subShell("abc");

        Assertions.assertThrows(IOException.class, () -> {
            sub.start();
        });

        Assertions.assertFalse(pc.isRunning());
        Assertions.assertFalse(sub.isRunning());
    }

    @ParameterizedTest
    @MethodSource("test.item.ShellTestItem#getAll")
    public void testLoopingRecoveryShellOpener(ShellProcessControl pc) throws Exception {
        pc.start();
        var sub = pc.subShell("for i in {1..150}; do echo -n \"a\"; sleep 0.1s; done");

        Assertions.assertThrows(IOException.class, () -> {
            sub.start();
        });

        Assertions.assertFalse(pc.isRunning());
        Assertions.assertFalse(sub.isRunning());
    }

    @ParameterizedTest
    @MethodSource("test.item.ShellTestItem#getAll")
    public void testLoopingShellOpener(ShellProcessControl pc) throws Exception {
        pc.start();
        var sub = pc.subShell("for i in {1..150}; do echo hi; sleep 0.03s; done");

        Assertions.assertThrows(IOException.class, () -> {
            sub.start();
        });

        Assertions.assertFalse(pc.isRunning());
        Assertions.assertFalse(sub.isRunning());
    }

    @ParameterizedTest
    @MethodSource("test.item.ShellTestItem#getAll")
    public void testFrozenShellOpener(ShellProcessControl pc) throws Exception {
        pc.start();
        var sub = pc.subShell("sleep 30");

        Assertions.assertThrows(IOException.class, () -> {
            sub.start();
        });

        Assertions.assertFalse(pc.isRunning());
        Assertions.assertFalse(sub.isRunning());
    }
}
