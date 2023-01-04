package io.xpipe.extension.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.SecretValue;
import io.xpipe.core.util.XPipeSession;
import io.xpipe.core.util.XPipeTempDirectory;
import io.xpipe.extension.event.TrackEvent;
import lombok.SneakyThrows;

import java.util.Objects;

public class ScriptHelper {

    public static int getConnectionHash(String command) {
        return Math.abs(Objects.hash(command, XPipeSession.get().getSystemSessionId()));
    }

    @SneakyThrows
    public static String createLocalExecScript(String content) {
        try (var l = ShellStore.local().create().start()) {
            return createExecScript(l, content, false);
        }
    }

    @SneakyThrows
    public static String createExecScript(ShellProcessControl processControl, String content, boolean restart) {
        var fileName = "exec-" + getConnectionHash(content);
        ShellType type = processControl.getShellType();
        var temp = XPipeTempDirectory.get(processControl);
        var file = FileNames.join(temp, fileName + "." + type.getScriptFileEnding());
        return createExecScript(processControl, file, content, restart);
    }

    @SneakyThrows
    private static String createExecScript(ShellProcessControl processControl, String file, String content, boolean restart) {
        ShellType type = processControl.getShellType();
        content = type.prepareScriptContent(content);

        if (processControl.executeBooleanSimpleCommand(type.getFileExistsCommand(file))) {
            return file;
        }

        TrackEvent.withTrace("proc", "Writing exec script")
                .tag("file", file)
                .tag("content", content)
                .handle();

        processControl.executeSimpleCommand(type.getFileTouchCommand(file), "Failed to create script " + file);
        processControl.executeSimpleCommand(type.getMakeExecutableCommand(file), "Failed to make script " + file + " executable");

        if (!content.contains("\n")) {
            processControl.executeSimpleCommand(type.getSimpleFileWriteCommand(content, file));
            return file;
        }

        try (var c = processControl.command(type.getStreamFileWriteCommand(file)).start()) {
            c.discardOut();
            c.discardErr();
            c.getStdin().write(content.getBytes(processControl.getCharset()));
            c.closeStdin();
        }

        if (restart) {
            processControl.restart();
        }

        return file;
    }

    @SneakyThrows
    public static String createAskPassScript(SecretValue pass, ShellProcessControl parent, ShellType type, boolean restart) {
        var content = type.getScriptEchoCommand(pass.getSecretValue());
        var temp = XPipeTempDirectory.get(parent);
        var file = FileNames.join(temp, "askpass-" + getConnectionHash(content) + "." + type.getScriptFileEnding());
        return createExecScript(parent,file, content, restart);
    }
}
