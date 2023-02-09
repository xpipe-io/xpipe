package test.item;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellProcessControl;
import lombok.Getter;
import org.apache.commons.lang3.function.FailableConsumer;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.UUID;

@Getter
public enum ShellCheckTestItem {
    OS_NAME(shellProcessControl -> {
        var os = shellProcessControl.getOsType();
        os.determineOperatingSystemName(shellProcessControl);
    }),

    RESTART(shellProcessControl -> {
        var s1 = shellProcessControl.executeStringSimpleCommand("echo hi");
        Assertions.assertEquals("hi", s1);
        shellProcessControl.restart();
        var s2 = shellProcessControl.executeStringSimpleCommand("echo world");
        Assertions.assertEquals("world", s2);
    }),

    INIT_FILE(shellProcessControl -> {
        var content = "<contentß>";
        try (var c = shellProcessControl
                .subShell(shellProcessControl.getShellType())
                .initWith(List.of(shellProcessControl.getShellType().getSetEnvironmentVariableCommand("testVar", content)))
                .start()) {
            var output = c.executeStringSimpleCommand(
                    shellProcessControl.getShellType().getPrintEnvironmentVariableCommand("testVar"));
            Assertions.assertEquals(content, output);
        }
    }),

    STREAM_WRITE(shellProcessControl -> {
        var content = "hello\nworldß";
        var fileOne = FileNames.join(shellProcessControl.getOsType().getTempDirectory(shellProcessControl), UUID.randomUUID().toString());
        try (var c = shellProcessControl
                .command(shellProcessControl.getShellType().getStreamFileWriteCommand(fileOne))
                .start()) {
            c.discardOut();
            c.discardErr();
            c.getStdin().write(content.getBytes(shellProcessControl.getCharset()));
            c.closeStdin();
        }

        shellProcessControl.restart();

        var fileTwo = FileNames.join(shellProcessControl.getOsType().getTempDirectory(shellProcessControl), UUID.randomUUID().toString());
        try (var c = shellProcessControl
                .subShell(shellProcessControl.getShellType())
                .command(shellProcessControl.getShellType().getStreamFileWriteCommand(fileTwo))
                .start()) {
            c.discardOut();
            c.discardErr();
            c.getStdin().write(content.getBytes(shellProcessControl.getCharset()));
            c.closeStdin();
        }

        shellProcessControl.restart();

        var s1 = shellProcessControl.executeStringSimpleCommand(
                shellProcessControl.getShellType().getFileReadCommand(fileOne));
        var s2 = shellProcessControl.executeStringSimpleCommand(
                shellProcessControl.getShellType().getFileReadCommand(fileTwo));
        Assertions.assertEquals(content, s1);
        Assertions.assertEquals(content, s2);
    }),

    SIMPLE_WRITE(shellProcessControl -> {
        var content = "hello worldß";
        var fileOne = FileNames.join(shellProcessControl.getOsType().getTempDirectory(shellProcessControl), UUID.randomUUID().toString());
        shellProcessControl.executeSimpleCommand(
                shellProcessControl.getShellType().getTextFileWriteCommand(content, fileOne));

        var fileTwo = FileNames.join(shellProcessControl.getOsType().getTempDirectory(shellProcessControl), UUID.randomUUID().toString());
        shellProcessControl.executeSimpleCommand(
                shellProcessControl.getShellType().getTextFileWriteCommand(content, fileTwo));

        var s1 = shellProcessControl.executeStringSimpleCommand(
                shellProcessControl.getShellType().getFileReadCommand(fileOne));
        var s2 = shellProcessControl.executeStringSimpleCommand(
                shellProcessControl.getShellType().getFileReadCommand(fileTwo));
        Assertions.assertEquals(content, s1);
        Assertions.assertEquals(content, s2);
    }),

    TERMINAL_OPEN(shellProcessControl -> {
        shellProcessControl.prepareIntermediateTerminalOpen(null);
    }),

    COMMAND_TERMINAL_OPEN(shellProcessControl -> {
        for (CommandCheckTestItem v : CommandCheckTestItem.values()) {
            shellProcessControl.prepareIntermediateTerminalOpen(v.getCommandFunction().apply(shellProcessControl));
        }
    }),

    ECHO(shellProcessControl -> {
        shellProcessControl.executeSimpleCommand(shellProcessControl.getShellType().getEchoCommand("test", false));
    });

    private final FailableConsumer<ShellProcessControl, Exception> shellCheck;

    ShellCheckTestItem(FailableConsumer<ShellProcessControl, Exception> shellCheck) {
        this.shellCheck = shellCheck;
    }
}
