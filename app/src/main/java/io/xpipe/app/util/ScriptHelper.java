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
        if (pc.getShellDialect().equals(ShellDialects.POWERSHELL)) {
            var script = ScriptHelper.createExecScript(pc, command);
            return String.format("Start-Process -WindowStyle Minimized -FilePath powershell.exe -ArgumentList \"-NoProfile\", \"-File\", %s", ShellDialects.POWERSHELL.fileArgument(script));
        }

        if (pc.getOsType().equals(OsType.WINDOWS)) {
            return "start \"\" /MIN " + command;
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
            ShellControl processControl, List<String> init, String toExecuteInShell, boolean login, String displayName) throws Exception {
        ShellDialect t = processControl.getShellDialect();
        String nl = t.getNewLine().getNewLineString();
        var content = String.join(nl, init.stream().filter(s -> s != null).toList()) + nl;

        if (displayName != null) {
            content = t.changeTitleCommand(displayName) + "\n" + content;
        }

        var applyRcCommand = t.applyRcFileCommand();
        if (applyRcCommand != null) {
            content = content + "\n" + applyRcCommand + "\n";
        }

        // We just apply the profile files always, as we can't be sure that they definitely have been applied.
        // Especially if we launch something that is not the system default shell
        var applyProfilesCommand = t.applyProfileFilesCommand();
        if (applyProfilesCommand != null) {
            content = content + "\n" + applyProfilesCommand + "\n";
        }

        if (toExecuteInShell != null) {
            // Normalize line endings
            content += String.join(nl, toExecuteInShell.lines().toList()) + nl;
            content += t.getExitCommand() + nl;
        }

        var initFile = createExecScript(processControl, t.initFileName(processControl), content);
        return initFile;
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
        var file = FileNames.join(temp, fileName + "." + fileEnding);
        return file;
    }

    @SneakyThrows
    public static String createExecScript(ShellControl processControl, String content) {
        var fileName = "exec-" + getScriptId();
        ShellDialect type = processControl.getShellDialect();
        var temp = processControl.getSubTemporaryDirectory();
        var file = FileNames.join(temp, fileName + "." + type.getScriptFileEnding());
        return createExecScript(processControl, file, content);
    }

    @SneakyThrows
    public static String createExecScript(ShellControl processControl, String file, String content) {
        ShellDialect type = processControl.getShellDialect();
        content = type.prepareScriptContent(content);

        TrackEvent.withTrace("proc", "Writing exec script")
                .tag("file", file)
                .tag("content", content)
                .handle();

        // processControl.executeSimpleCommand(type.getFileTouchCommand(file), "Failed to create script " + file);
        processControl
                .getShellDialect()
                .createTextFileWriteCommand(processControl, content, file)
                .execute();
        var e = type.getMakeExecutableCommand(file);
        if (e != null) {
            processControl.executeSimpleCommand(e, "Failed to make script " + file + " executable");
        }
        return file;
    }

    public static String createAskPassScript(SecretValue pass, ShellControl parent, boolean forceExecutable)
            throws Exception {
        return createAskPassScript(pass != null ? List.of(pass) : List.of(), parent, forceExecutable);
    }

    public static String createAskPassScript(List<SecretValue> pass, ShellControl parent, boolean forceExecutable)
            throws Exception {
        var scriptType = parent.getShellDialect();

        // Fix for powershell as there are permission issues when executing a powershell askpass script
        if (forceExecutable && parent.getShellDialect().equals(ShellDialects.POWERSHELL)) {
            scriptType = parent.getOsType().equals(OsType.WINDOWS) ? ShellDialects.CMD : ShellDialects.SH;
        }

        return createAskPassScript(pass, parent, scriptType);
    }

    private static String createAskPassScript(List<SecretValue> pass, ShellControl parent, ShellDialect type)
            throws Exception {
        var fileName = "askpass-" + getScriptId() + "." + type.getScriptFileEnding();
        var temp = parent.getSubTemporaryDirectory();
        var file = FileNames.join(temp, fileName);
        if (type != parent.getShellDialect()) {
            try (var sub = parent.subShell(type).start()) {
                var content = sub.getShellDialect()
                        .prepareAskpassContent(
                                sub, file, pass.stream().map(secretValue -> secretValue.getSecretValue()).toList());
                var exec = createExecScript(sub, file, content);
                return exec;
            }
        } else {
            var content = parent.getShellDialect()
                    .prepareAskpassContent(
                            parent, file, pass.stream().map(secretValue -> secretValue.getSecretValue()).toList());
            var exec = createExecScript(parent, file, content);
            return exec;
        }
    }
}
