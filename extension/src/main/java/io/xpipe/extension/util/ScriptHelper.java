package io.xpipe.extension.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.SecretValue;
import io.xpipe.extension.event.TrackEvent;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Random;

public class ScriptHelper {

    public static int getScriptId() {
        // A deterministic approach can cause permission problems when two different users execute the same command on a system
        // Therefore, use a random approach
        return new Random().nextInt(Integer.MAX_VALUE);
    }

    @SneakyThrows
    public static String createLocalExecScript(String content) {
        try (var l = ShellStore.local().create().start()) {
            return createExecScript(l, content, false);
        }
    }

    public static String constructOpenWithInitScriptCommand(ShellProcessControl processControl, List<String> init, String toExecuteInShell) {
        ShellType t = processControl.getShellType();
        if (init.size() == 0 && toExecuteInShell == null) {
            return t.getNormalOpenCommand();
        }

        String nl = t.getNewLine().getNewLineString();
        var content = String.join(nl, init)
                + nl;

        if (processControl.getOsType().equals(OsType.LINUX)
                || processControl.getOsType().equals(OsType.MAC)) {
            content = "if [ -f ~/.bashrc ]; then . ~/.bashrc; fi\n" + content;
        }

        if (toExecuteInShell != null) {
            // Normalize line endings
            content += String.join(nl, toExecuteInShell.lines().toList()) + nl;
            content += t.getExitCommand() + nl;
        }

        var initFile = createExecScript(processControl, content, true);
        return t.getInitFileOpenCommand(initFile);
    }

    @SneakyThrows
    public static String createExecScript(ShellProcessControl processControl, String content, boolean restart) {
        var fileName = "exec-" + getScriptId();
        ShellType type = processControl.getShellType();
        var temp = processControl.getTemporaryDirectory();
        var file = FileNames.join(temp, fileName + "." + type.getScriptFileEnding());
        return createExecScript(processControl, file, content, restart);
    }

    @SneakyThrows
    private static String createExecScript(
            ShellProcessControl processControl, String file, String content, boolean restart) {
        ShellType type = processControl.getShellType();
        content = type.prepareScriptContent(content);

        TrackEvent.withTrace("proc", "Writing exec script")
                .tag("file", file)
                .tag("content", content)
                .handle();

        // processControl.executeSimpleCommand(type.getFileTouchCommand(file), "Failed to create script " + file);
        processControl.executeSimpleCommand(type.getTextFileWriteCommand(content, file));
        processControl.executeSimpleCommand(
                type.getMakeExecutableCommand(file), "Failed to make script " + file + " executable");
        return file;
    }

    @SneakyThrows
    public static String createAskPassScript(
            SecretValue pass, ShellProcessControl parent, ShellType type, boolean restart) {
        var content = type.getScriptEchoCommand(pass.getSecretValue());
        var temp = parent.getTemporaryDirectory();
        var file = FileNames.join(temp, "askpass-" + getScriptId() + "." + type.getScriptFileEnding());
        return createExecScript(parent, file, content, restart);
    }
}
