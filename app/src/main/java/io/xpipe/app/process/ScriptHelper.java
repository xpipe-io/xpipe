package io.xpipe.app.process;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;
import io.xpipe.core.SecretValue;

import lombok.SneakyThrows;

import java.util.List;
import java.util.Objects;
import java.util.Random;

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
        content = type.prepareScriptContent(processControl, content);
        var fileName = "xpipe-" + getScriptHash(processControl, content);
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
}
