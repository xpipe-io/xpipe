package io.xpipe.app.util;

import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.ServerException;
import io.xpipe.core.process.ProcessControl;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.TerminalInitScriptConfig;
import io.xpipe.core.util.FailableFunction;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class TerminalLauncherManager {

    private static final Map<UUID, Entry> entries = new ConcurrentHashMap<>();

    private static void prepare(
            ProcessControl processControl, TerminalInitScriptConfig config, String directory, Entry entry) {
        FailableFunction<ShellControl, String, Exception> workingDirectory = sc -> {
            if (directory == null) {
                return null;
            }

            if (!sc.getShellDialect().directoryExists(sc,directory).executeAndCheck()) {
                return sc.getOsType().getFallbackWorkingDirectory();
            }

            return directory;
        };

        try {
            var file = ScriptHelper.createLocalExecScript(
                    processControl.prepareTerminalOpen(config, workingDirectory));
            entry.setResult(new ResultSuccess(Path.of(file)));
        } catch (Exception e) {
            entry.setResult(new ResultFailure(e));
        }
    }

    public static void submitSync(
            UUID request, ProcessControl processControl, TerminalInitScriptConfig config, String directory) {
        var entry = new Entry(request, processControl, config, directory, null);
        entries.put(request, entry);
        prepare(processControl, config, directory, entry);
    }

    public static CountDownLatch submitAsync(
            UUID request, ProcessControl processControl, TerminalInitScriptConfig config, String directory) {
        var entry = new Entry(request, processControl, config, directory, null);
        entries.put(request, entry);
        var latch = new CountDownLatch(1);
        ThreadHelper.runAsync(() -> {
            prepare(processControl, config, directory, entry);
            latch.countDown();
        });
        return latch;
    }

    public static Path waitForCompletion(UUID request) throws ClientException, ServerException {
        var e = entries.get(request);
        if (e == null) {
            throw new ClientException("Unknown launch request " + request);
        }

        while (true) {
            if (e.result == null) {
                ThreadHelper.sleep(10);
                continue;
            }

            var r = e.getResult();
            if (r instanceof ResultFailure failure) {
                entries.remove(request);
                var t = failure.getThrowable();
                throw new ServerException(t);
            }

            return ((ResultSuccess) r).getTargetScript();
        }
    }

    public static Path performLaunch(UUID request) throws ClientException {
        var e = entries.remove(request);
        if (e == null) {
            throw new ClientException("Unknown launch request " + request);
        }

        if (!(e.result instanceof ResultSuccess)) {
            throw new ClientException("Invalid launch request state " + request);
        }

        return ((ResultSuccess) e.getResult()).getTargetScript();
    }

    public interface Result {}

    @Value
    public static class Entry {

        UUID request;
        ProcessControl processControl;
        TerminalInitScriptConfig config;
        String workingDirectory;

        @Setter
        @NonFinal
        Result result;
    }

    @Value
    public static class ResultSuccess implements Result {
        Path targetScript;
    }

    @Value
    public static class ResultFailure implements Result {
        Throwable throwable;
    }
}
