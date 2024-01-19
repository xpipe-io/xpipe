package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.util.SecretValue;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Random;

public class ScriptHelper {

    public static int getScriptId() {
        // A deterministic approach can cause permission problems when two different users execute the same command on a
        // system
        // Therefore, use a random approach
        return new Random().nextInt(Integer.MAX_VALUE);
    }

    @SneakyThrows
    public static String createLocalExecScript(String content) {
        try (var l = LocalShell.getShell().start()) {
            return createExecScript(l, content);
        }
    }

    public static String constructInitFile(ShellControl processControl, List<String> init, String toExecuteInShell, TerminalInitScriptConfig config)
            throws Exception {
        return constructInitFile(processControl.getShellDialect(), processControl, init, toExecuteInShell, config);
    }

    public static String constructInitFile(ShellDialect t, ShellControl processControl, List<String> init, String toExecuteInShell, TerminalInitScriptConfig config)
            throws Exception {
        String nl = t.getNewLine().getNewLineString();
        var content = "";

        var clear = t.clearDisplayCommand();
        if (clear != null && config.isClearScreen()) {
            content += clear + nl;
        }

        var applyRcCommand = t.applyRcFileCommand();
        if (applyRcCommand != null) {
            content += nl + applyRcCommand + nl;
        }

        // We just apply the profile files always, as we can't be sure that they definitely have been applied.
        // Especially if we launch something that is not the system default shell
        var applyProfilesCommand = t.applyProfileFilesCommand();
        if (applyProfilesCommand != null) {
            content += nl + applyProfilesCommand + nl;
        }

        if (config.getDisplayName() != null) {
            content += nl + t.changeTitleCommand(config.getDisplayName())  + nl;
        }

        content += nl + String.join(nl, init.stream().filter(s -> s != null).toList()) + nl;

        if (toExecuteInShell != null) {
            // Normalize line endings
            content += String.join(nl, toExecuteInShell.lines().toList()) + nl;
            content += nl + t.getPassthroughExitCommand() + nl;
        }

        return createExecScript(t, processControl, t.initFileName(processControl), content);
    }

    @SneakyThrows
    public static String getExecScriptFile(ShellControl processControl) {
        return getExecScriptFile(
                processControl, processControl.getShellDialect().getScriptFileEnding());
    }

    @SneakyThrows
    public static String getExecScriptFile(ShellControl processControl, String fileEnding) {
        var fileName = "exec-" + getScriptId();
        var temp = processControl.getSubTemporaryDirectory();
        return FileNames.join(temp, fileName + "." + fileEnding);
    }

    @SneakyThrows
    public static String createExecScript(ShellControl processControl, String content) {
        return createExecScript(processControl.getShellDialect(), processControl, content);
    }

    @SneakyThrows
    public static String createExecScript(ShellDialect type, ShellControl processControl, String content) {
        var fileName = "exec-" + getScriptId();
        var temp = processControl.getSubTemporaryDirectory();
        var file = FileNames.join(temp, fileName + "." + type.getScriptFileEnding());
        return createExecScript(type, processControl, file, content);
    }

    @SneakyThrows
    public static String createExecScript(ShellDialect type, ShellControl processControl, String file, String content) {
        content = type.prepareScriptContent(content);

        TrackEvent.withTrace("proc", "Writing exec script")
                .tag("file", file)
                .tag("content", content)
                .handle();

        processControl
                .getShellDialect()
                .createScriptTextFileWriteCommand(processControl, content, file)
                .execute();
        return file;
    }

    public static String createAskpassScript(SecretValue pass, ShellControl parent, boolean forceExecutable, String errorMessage)
            throws Exception {
        return createAskpassScript(pass != null ? List.of(pass) : List.of(), parent, forceExecutable, errorMessage);
    }

    public static String createAskpassScript(List<SecretValue> pass, ShellControl parent, boolean forceExecutable, String errorMessage)
            throws Exception {
        var scriptType = parent.getShellDialect();

        // Fix for powershell as there are permission issues when executing a powershell askpass script
        if (forceExecutable && parent.getShellDialect().equals(ShellDialects.POWERSHELL)) {
            scriptType = parent.getOsType().equals(OsType.WINDOWS) ? ShellDialects.CMD : ShellDialects.SH;
        }

        return createAskpassScript(pass, parent, scriptType, errorMessage);
    }

    private static String createAskpassScript(List<SecretValue> pass, ShellControl parent, ShellDialect type, String errorMessage)
            throws Exception {
        var fileName = "exec-" + getScriptId() + "." + type.getScriptFileEnding();
        var temp = parent.getSubTemporaryDirectory();
        var file = FileNames.join(temp, fileName);
        if (type != parent.getShellDialect()) {
            try (var sub = parent.subShell(type).start()) {
                var content = sub.getShellDialect()
                        .prepareAskpassContent(
                                sub,
                                file,
                                pass.stream()
                                        .map(secretValue -> secretValue.getSecretValue())
                                        .toList(), errorMessage);
                return createExecScript(sub.getShellDialect(), sub, file, content);
            }
        } else {
            var content = parent.getShellDialect()
                    .prepareAskpassContent(
                            parent,
                            file,
                            pass.stream()
                                    .map(secretValue -> secretValue.getSecretValue())
                                    .toList(), errorMessage);
            return createExecScript(parent.getShellDialect(), parent, file, content);
        }
    }
}
