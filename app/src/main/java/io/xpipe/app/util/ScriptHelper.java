package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;
import io.xpipe.core.SecretValue;

import lombok.SneakyThrows;

import java.util.List;
import java.util.Random;

public class ScriptHelper {

    public static int getScriptHash(String content) {
        return Math.abs(content.hashCode());
    }

    @SneakyThrows
    public static FilePath createLocalExecScript(String content) {
        try (var l = LocalShell.getShell().start()) {
            return createExecScript(l, content);
        }
    }

    @SneakyThrows
    public static FilePath createExecScript(ShellControl processControl, String content) {
        return createExecScript(processControl.getShellDialect(), processControl, content);
    }

    @SneakyThrows
    public static FilePath createExecScript(ShellDialect type, ShellControl processControl, String content) {
        content = type.prepareScriptContent(processControl, content);
        var fileName = "xpipe-" + getScriptHash(content);
        var temp = processControl.getSystemTemporaryDirectory();
        var file = temp.join(fileName + "." + type.getScriptFileEnding());
        return createExecScriptRaw(processControl, file, content);
    }

    @SneakyThrows
    public static FilePath createExecScriptRaw(ShellControl processControl, FilePath file, String content) {
        if (processControl.view().fileExists(file)) {
            return file;
        }

        TrackEvent.withTrace("Writing exec script")
                .tag("file", file)
                .tag("content", content)
                .handle();
        processControl.view().writeScriptFile(file, content);
        return file;
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
            scriptType = parent.getOsType() == OsType.WINDOWS ? ShellDialects.CMD : ShellDialects.SH;
        }

        return createTerminalPreparedAskpassScript(pass, parent, scriptType);
    }

    private static FilePath createTerminalPreparedAskpassScript(
            List<SecretValue> pass, ShellControl parent, ShellDialect type) throws Exception {
        var fileName = "xpipe-" + Math.abs(new Random().nextInt());
        var temp = parent.getSystemTemporaryDirectory();
        var fileBase = temp.join(fileName);
        if (type != parent.getShellDialect()) {
            try (var sub = parent.subShell(type).start()) {
                var content = sub.getShellDialect()
                        .getAskpass()
                        .prepareFixedContent(
                                sub,
                                fileBase.toString(),
                                pass.stream()
                                        .map(secretValue -> secretValue.getSecretValue())
                                        .toList());
                content = sub.getShellDialect().prepareScriptContent(sub, content);
                return createExecScriptRaw(
                        sub,
                        sub.getSystemTemporaryDirectory()
                                .join(fileName + "." + sub.getShellDialect().getScriptFileEnding()),
                        content);
            }
        } else {
            var content = parent.getShellDialect()
                    .getAskpass()
                    .prepareFixedContent(
                            parent,
                            fileBase.toString(),
                            pass.stream()
                                    .map(secretValue -> secretValue.getSecretValue())
                                    .toList());
            content = parent.getShellDialect().prepareScriptContent(parent, content);
            return createExecScriptRaw(
                    parent,
                    parent.getSystemTemporaryDirectory()
                            .join(fileName + "." + parent.getShellDialect().getScriptFileEnding()),
                    content);
        }
    }
}
