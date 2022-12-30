package io.xpipe.extension.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.XPipeTempDirectory;
import lombok.SneakyThrows;

import java.util.Objects;

public class ExecScriptHelper {

    public static int getConnectionHash(String command) {
        return Math.abs(Objects.hash(command));
    }

    @SneakyThrows
    public static String createLocalExecScript(String content) {
        try (var l = ShellStore.local().create().start()) {
            return createExecScript(l, content);
        }
    }

    @SneakyThrows
    public static String createExecScript(ShellProcessControl processControl, String content) {
        var fileName = "exec-" + getConnectionHash(content);
        content = processControl.getShellType().createInitFileContent(content);
        var temp = XPipeTempDirectory.get(processControl);
        var file = FileNames.join(
                temp, fileName + "." + processControl.getShellType().getScriptFileEnding());

        if (processControl.executeBooleanSimpleCommand(processControl.getShellType().createFileExistsCommand(file))) {
            return file;
        }

        try (var c = processControl.command(processControl.getShellType()
                                                    .joinCommands(
                                                            processControl.getShellType().createFileWriteCommand(file),
                                                            processControl.getShellType().getMakeExecutableCommand(file)
                                                    ))
                .start()) {
            c.discardOut();
            c.discardErr();
            c.getStdin().write(content.getBytes(processControl.getCharset()));
            c.closeStdin();
        }

        processControl.restart();

        return file;
    }
}
