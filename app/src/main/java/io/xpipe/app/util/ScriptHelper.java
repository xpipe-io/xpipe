package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.SecretValue;

import lombok.SneakyThrows;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScriptHelper {

    public static int getScriptId() {
        // A deterministic approach can cause permission problems when two different users execute the same command on a
        // system
        // Therefore, use a random approach
        return new Random().nextInt(Integer.MAX_VALUE);
    }

    @SneakyThrows
    public static FilePath createLocalExecScript(String content) {
        try (var l = LocalShell.getShell().start()) {
            return createExecScript(l, content);
        }
    }

    public static FilePath constructTerminalInitFile(
            ShellDialect t,
            ShellControl processControl,
            WorkingDirectoryFunction workingDirectory,
            List<String> preInit,
            List<String> postInit,
            TerminalInitScriptConfig config,
            boolean exit)
            throws Exception {
        String nl = t.getNewLine().getNewLineString();
        var content = "";

        var clear = t.clearDisplayCommand();
        if (clear != null && config.isClearScreen()) {
            content += clear + nl;
        }

        // Normalize line endings
        content += nl + preInit.stream().flatMap(s -> s.lines()).collect(Collectors.joining(nl)) + nl;

        // We just apply the profile files always, as we can't be sure that they definitely have been applied.
        // Especially if we launch something that is not the system default shell
        var applyCommand = t.applyInitFileCommand(processControl);
        if (applyCommand != null) {
            content += nl + applyCommand + nl;
        }

        if (config.getDisplayName() != null) {
            content += nl + t.changeTitleCommand(config.getDisplayName()) + nl;
        }

        if (workingDirectory != null && workingDirectory.isSpecified()) {
            var wd = workingDirectory.apply(processControl);
            if (wd != null) {
                content += t.getCdCommand(wd.toString()) + nl;
            }
        }

        // Normalize line endings
        content += nl + postInit.stream().flatMap(s -> s.lines()).collect(Collectors.joining(nl)) + nl;

        if (exit) {
            content += nl + t.getPassthroughExitCommand();
        }

        return createExecScript(t, processControl, new FilePath(t.initFileName(processControl)), content);
    }

    @SneakyThrows
    public static FilePath getExecScriptFile(ShellControl processControl) {
        return getExecScriptFile(
                processControl, processControl.getShellDialect().getScriptFileEnding());
    }

    @SneakyThrows
    public static FilePath getExecScriptFile(ShellControl processControl, String fileEnding) {
        var fileName = "exec-" + getScriptId();
        var temp = processControl.getSystemTemporaryDirectory();
        return temp.join(fileName + "." + fileEnding);
    }

    @SneakyThrows
    public static FilePath createExecScript(ShellControl processControl, String content) {
        return createExecScript(processControl.getShellDialect(), processControl, content);
    }

    @SneakyThrows
    public static FilePath createExecScript(ShellDialect type, ShellControl processControl, String content) {
        var fileName = "exec-" + getScriptId();
        var temp = processControl.getSystemTemporaryDirectory();
        var file = temp.join(fileName + "." + type.getScriptFileEnding());
        return createExecScript(type, processControl, file, content);
    }

    @SneakyThrows
    public static FilePath createExecScript(
            ShellDialect type, ShellControl processControl, FilePath file, String content) {
        content = type.prepareScriptContent(content);

        TrackEvent.withTrace("Writing exec script")
                .tag("file", file)
                .tag("content", content)
                .handle();

        processControl.view().writeScriptFile(file, content);
        file = fixScriptPermissions(processControl, file);
        return file;
    }

    public static FilePath fixScriptPermissions(ShellControl processControl, FilePath file) throws Exception {
        // Check if file system has disabled execution in temp
        // This might happen in limited containers
        if (processControl.getOsType() == OsType.LINUX
                && ShellDialects.SH
                        .getClass()
                        .isAssignableFrom(processControl.getShellDialect().getClass())
                && !processControl
                        .command(CommandBuilder.of().add("test", "-x").addFile(file))
                        .executeAndCheck()) {
            var homeFile =
                    processControl.view().userHome().join(".xpipe", "scripts").join(file.getFileName());
            processControl.executeSimpleCommand(processControl
                    .getShellDialect()
                    .getMkdirsCommand(homeFile.getParent().toString()));
            processControl
                    .getShellDialect()
                    .getFileMoveCommand(processControl, file.toString(), homeFile.toString())
                    .execute();
            return homeFile;
        } else {
            return file;
        }
    }

    public static FilePath createRemoteAskpassScript(ShellControl parent, UUID requestId, String prefix)
            throws Exception {
        var type = parent.getShellDialect();

        // Fix for powershell as there are permission issues when executing a powershell askpass script
        if (ShellDialects.isPowershell(parent)) {
            type = parent.getOsType().equals(OsType.WINDOWS) ? ShellDialects.CMD : ShellDialects.SH;
        }

        var fileName = "exec-" + getScriptId() + "." + type.getScriptFileEnding();
        var temp = parent.getSystemTemporaryDirectory();
        var file = temp.join(fileName);
        if (type != parent.getShellDialect()) {
            try (var sub = parent.subShell(type).start()) {
                var content =
                        sub.getShellDialect().getAskpass().prepareStderrPassthroughContent(sub, requestId, prefix);
                return createExecScript(sub.getShellDialect(), sub, file, content);
            }
        } else {
            var content =
                    parent.getShellDialect().getAskpass().prepareStderrPassthroughContent(parent, requestId, prefix);
            return createExecScript(parent.getShellDialect(), parent, file, content);
        }
    }

    public static FilePath createTerminalPreparedAskpassScript(
            SecretValue pass, ShellControl parent, boolean forceExecutable) throws Exception {
        return createTerminalPreparedAskpassScript(pass != null ? List.of(pass) : List.of(), parent, forceExecutable);
    }

    public static FilePath createTerminalPreparedAskpassScript(
            List<SecretValue> pass, ShellControl parent, boolean forceExecutable) throws Exception {
        var scriptType = parent.getShellDialect();

        // Fix for powershell as there are permission issues when executing a powershell askpass script
        if (forceExecutable && ShellDialects.isPowershell(parent)) {
            scriptType = parent.getOsType().equals(OsType.WINDOWS) ? ShellDialects.CMD : ShellDialects.SH;
        }

        return createTerminalPreparedAskpassScript(pass, parent, scriptType);
    }

    private static FilePath createTerminalPreparedAskpassScript(
            List<SecretValue> pass, ShellControl parent, ShellDialect type) throws Exception {
        var fileName = "exec-" + getScriptId() + "." + type.getScriptFileEnding();
        var temp = parent.getSystemTemporaryDirectory();
        var file = temp.join(fileName);
        if (type != parent.getShellDialect()) {
            try (var sub = parent.subShell(type).start()) {
                var content = sub.getShellDialect()
                        .getAskpass()
                        .prepareFixedContent(
                                sub,
                                file.toString(),
                                pass.stream()
                                        .map(secretValue -> secretValue.getSecretValue())
                                        .toList());
                return createExecScript(sub.getShellDialect(), sub, file, content);
            }
        } else {
            var content = parent.getShellDialect()
                    .getAskpass()
                    .prepareFixedContent(
                            parent,
                            file.toString(),
                            pass.stream()
                                    .map(secretValue -> secretValue.getSecretValue())
                                    .toList());
            return createExecScript(parent.getShellDialect(), parent, file, content);
        }
    }
}
