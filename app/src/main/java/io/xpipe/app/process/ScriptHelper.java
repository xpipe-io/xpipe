package io.xpipe.app.process;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.FilePath;

import lombok.SneakyThrows;

import java.util.Objects;

public class ScriptHelper {

    public static int getScriptHash(ShellControl sc, String content) throws Exception {
        return Math.abs(Objects.hash(content, sc.view().user()));
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
        return createExecScript(type, processControl, content, true);
    }

    @SneakyThrows
    public static FilePath createExecScript(
            ShellDialect type, ShellControl processControl, String content, boolean log) {
        content = type.prepareScriptContent(processControl, content);
        var fileName = "xpipe-" + getScriptHash(processControl, content);
        var temp = processControl.getSystemTemporaryDirectory();
        var file = temp.join(fileName + "." + type.getScriptFileEnding());
        return createExecScriptRaw(processControl, file, content, log);
    }

    @SneakyThrows
    public static FilePath createExecScriptRaw(
            ShellControl processControl, FilePath file, String content, boolean log) {
        var exists = processControl.view().fileExists(file);

        if (log) {
            TrackEvent.withTrace("Creating exec script")
                    .tag("file", file)
                    .tag("exists", exists)
                    .tag("content", content)
                    .handle();
        }

        if (exists) {
            return file;
        }

        processControl.view().writeScriptFile(file, content);
        return file;
    }
}
