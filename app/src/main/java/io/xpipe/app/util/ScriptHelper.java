package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellDialects;
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

    public static String unquote(String input) {
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }

        if (input.startsWith("'") && input.endsWith("'")) {
            return input.substring(1, input.length() - 1);
        }

        return input;
    }

    public static String constructInitFile(
            ShellControl processControl, List<String> init, String toExecuteInShell) {
        ShellDialect t = processControl.getShellDialect();

        // We always want to generate and init file
        if (init.size() == 0 && toExecuteInShell == null) {
            return createExecScript(processControl, processControl.getShellDialect().getNewLine().getNewLineString());
        }

        if (init.size() == 0) {
            // Check for special case of the command to be executed just being another shell script
            if (toExecuteInShell.endsWith(".sh") || toExecuteInShell.endsWith(".bat")) {
                return toExecuteInShell;
            }
        }

        String nl = t.getNewLine().getNewLineString();
        var content = String.join(nl, init) + nl;

        var applyCommand = t.applyRcFileCommand();
        if (applyCommand != null) {
            content = applyCommand + "\n" + content;
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
        processControl.executeSimpleCommand(
                type.getMakeExecutableCommand(file), "Failed to make script " + file + " executable");
        return file;
    }

    public static String createAskPassScript(SecretValue pass, ShellControl parent) throws Exception {
        var scriptType = parent.getShellDialect();

        // Fix for power shell as there are permission issues when executing a powershell askpass script
        if (parent.getShellDialect().equals(ShellDialects.POWERSHELL)) {
            scriptType = parent.getOsType().equals(OsType.WINDOWS) ? ShellDialects.CMD : ShellDialects.BASH;
        }

        return createAskPassScript(pass, parent, scriptType);
    }

    private static String createAskPassScript(SecretValue pass, ShellControl parent, ShellDialect type) throws Exception {
        var content = type.getSelfdeleteEchoScriptContent(pass.getSecretValue());
        var temp = parent.getTemporaryDirectory();
        var file = FileNames.join(temp, "askpass-" + getScriptId() + "." + type.getScriptFileEnding());
        return createExecScript(parent, file, content);
    }
}
