package test;

import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.SecretValue;
import io.xpipe.ext.proc.store.CommandStore;
import io.xpipe.ext.proc.store.WslStore;
import io.xpipe.extension.test.LocalExtensionTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

public class CommandStoreTest extends LocalExtensionTest {

    @Test
    public void testCmdRead() throws Exception {
        var store = CommandStore.builder()
                .host(ShellStore.local())
                .shell(ShellTypes.CMD)
                .flow(DataFlow.INPUT)
                .cmd("echo hi& echo there")
                .build();
        try (InputStream inputStream = store.openInput()) {
            var read = new String(inputStream.readAllBytes());
            Assertions.assertEquals("hi\r\nthere\r\n", read);
        }
    }

    @Test
    public void testpowershellReadAndWritea() throws Exception {
        try (ShellProcessControl pc = new WslStore(ShellStore.local(), null, null)
                .create()
                .subShell(ShellTypes.BASH)
                .start()) {
            try (var command = pc.command(List.of("echo", "hi")).start()) {
                var read = command.readOnlyStdout();
                Assertions.assertEquals("hi", read);
            }

            try (var command = pc.command(List.of("echo", "there")).start()) {
                var read = command.readOnlyStdout();
                Assertions.assertEquals("there", read);
            }
        }
    }

    @Test
    public void testWslElevation() throws Exception {
        try (ShellProcessControl pc = new WslStore(ShellStore.local(), null, null)
                .create()
                .subShell(ShellTypes.BASH)
                .elevation(SecretValue.encrypt("123"))
                .start()) {
            try (var command = pc.command(List.of("echo", "hi")).elevated().start()) {
                var read = command.readOrThrow();
                Assertions.assertEquals("hi", read);
            }
        }
    }
}
