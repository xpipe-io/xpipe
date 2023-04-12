package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.util.SecretValue;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Random;

public class ScriptHelper {

    public static String createDetachCommand(ShellControl pc, String command) {
        if (pc.getOsType().equals(OsType.WINDOWS)) {
            return "start \"\" " + command;
        } else {
            return "nohup " + command + " </dev/null &>/dev/null & disown";
        }
    }

    public static int getScriptId() {
        // A deterministic approach can cause permission problems when two different users execute the same command on a
        // system
        // Therefore, use a random approach
        return new Random().nextInt(Integer.MAX_VALUE);
    }

    @SneakyThrows
    public static String createLocalExecScript(String content) {
        try (var l = LocalStore.getShell().start()) {
            return createExecScript(l, content);
        }
    }

    public static String constructInitFile(
            ShellControl processControl, List<String> init, String toExecuteInShell,boolean login) {
        ShellDialect t = processControl.getShellDialect();
        String nl = t.getNewLine().getNewLineString();
        var content = String.join(nl, init) + nl;

        if (login) {
            var applyProfilesCommand = t.applyProfileFilesCommand();
            if (applyProfilesCommand != null) {
                content = applyProfilesCommand + "\n" + content;
            }
        }

        var applyRcCommand = t.applyRcFileCommand();
        if (applyRcCommand != null) {
            content = applyRcCommand + "\n" + content;
        }

        if (toExecuteInShell != null) {
            // Normalize line endings
            content += String.join(nl, toExecuteInShell.lines().toList()) + nl;
            content += t.getExitCommand() + nl;
        }

        var initFile = createExecScript(processControl, content);
        return initFile;
    }

    @SneakyThrows
    public static String getExecScriptFile(ShellControl processControl) {
        return getExecScriptFile(processControl, processControl.getShellDialect().getScriptFileEnding());
    }

    @SneakyThrows
    public static String getExecScriptFile(ShellControl processControl, String fileEnding) {
        var fileName = "exec-" + getScriptId();
        var temp = processControl.getTemporaryDirectory();
        var file = FileNames.join(temp, fileName + "." + fileEnding);
        return file;
    }

    @SneakyThrows
    public static String createExecScript(ShellControl processControl, String content) {
        var fileName = "exec-" + getScriptId();
        ShellDialect type = processControl.getShellDialect();
        var temp = processControl.getTemporaryDirectory();
        var file = FileNames.join(temp, fileName + "." + type.getScriptFileEnding());
        return createExecScript(processControl, file, content);
    }

    @SneakyThrows
    private static String createExecScript(ShellControl processControl, String file, String content) {
        ShellDialect type = processControl.getShellDialect();
        content = type.prepareScriptContent(content);

        TrackEvent.withTrace("proc", "Writing exec script")
                .tag("file", file)
                .tag("content", content)
                .handle();

        // processControl.executeSimpleCommand(type.getFileTouchCommand(file), "Failed to create script " + file);
        processControl.getShellDialect().createTextFileWriteCommand(processControl, content, file).execute();
        var e = type.getMakeExecutableCommand(file);
        if (e != null) {
            processControl.executeSimpleCommand(e, "Failed to make script " + file + " executable");
        }
        return file;
    }

    public static String createAskPassScript(SecretValue pass, ShellControl parent) throws Exception {
        var scriptType = parent.getShellDialect();
        return createAskPassScript(pass, parent, scriptType);
    }

    private static String createAskPassScript(SecretValue pass, ShellControl parent, ShellDialect type) throws Exception {
        var content = type.getSelfdeleteEchoScriptContent(pass.getSecretValue());
        var temp = parent.getTemporaryDirectory();
        var file = FileNames.join(temp, "askpass-" + getScriptId() + "." + type.getScriptFileEnding());
        return createExecScript(parent, file, content);
    }
}
