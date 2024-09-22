package io.xpipe.app.util;

import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.core.process.ProcessControl;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.TerminalInitScriptConfig;
import io.xpipe.core.process.WorkingDirectoryFunction;
import io.xpipe.core.store.FilePath;

import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class TerminalLauncherManager {

    private static final SequencedMap<UUID, Entry> entries = new LinkedHashMap<>();

    private static void prepare(
            ProcessControl processControl, TerminalInitScriptConfig config, String directory, Entry entry) {
        var workingDirectory = new WorkingDirectoryFunction() {

            @Override
            public boolean isFixed() {
                return true;
            }

            @Override
            public boolean isSpecified() {
                return directory != null;
            }

            @Override
            public FilePath apply(ShellControl shellControl) {
                if (directory == null) {
                    return null;
                }

                return new FilePath(directory);
            }
        };

        try {
            var file = ScriptHelper.createLocalExecScript(processControl.prepareTerminalOpen(config, workingDirectory));
            entry.setResult(new ResultSuccess(Path.of(file.toString())));
        } catch (Exception e) {
            entry.setResult(new ResultFailure(e));
        }
    }

    public static CountDownLatch submitAsync(
            UUID request, ProcessControl processControl, TerminalInitScriptConfig config, String directory) {
        synchronized (entries) {
            var entry = entries.get(request);
            if (entry == null) {
                entry = new Entry(request, processControl, config, directory, null, false);
                entries.put(request, entry);
            } else {
                entry.setResult(null);
            }
            var latch = new CountDownLatch(1);
            Entry finalEntry = entry;
            ThreadHelper.runAsync(() -> {
                prepare(processControl, config, directory, finalEntry);
                latch.countDown();
            });
            return latch;
        }
    }

    public static Path waitForNextLaunch() throws BeaconClientException, BeaconServerException {
        Entry first;
        synchronized (entries) {
            first = entries.values().stream()
                    .filter(entry -> !entry.isLaunched())
                    .findFirst()
                    .orElse(null);
            if (first == null) {
                throw new BeaconClientException("Unknown launch request");
            }
        }
        return waitForCompletion(first);
    }

    public static Path waitForCompletion(UUID request) throws BeaconClientException, BeaconServerException {
        Entry e;
        synchronized (entries) {
            e = entries.get(request);
        }
        if (e == null) {
            throw new BeaconClientException("Unknown launch request " + request);
        }
        if (e.isLaunched()) {
            submitAsync(e.getRequest(), e.getProcessControl(), e.getConfig(), e.getWorkingDirectory());
        }
        return waitForCompletion(e);
    }

    public static Path waitForCompletion(Entry e) throws BeaconServerException {
        while (true) {
            if (e.result == null) {
                ThreadHelper.sleep(10);
                continue;
            }

            synchronized (entries) {
                var r = e.getResult();
                if (r instanceof ResultFailure failure) {
                    var t = failure.getThrowable();
                    throw new BeaconServerException(t);
                }

                return ((ResultSuccess) r).getTargetScript();
            }
        }
    }

    public static Path performLaunch(UUID request) throws BeaconClientException {
        synchronized (entries) {
            var e = entries.values().stream()
                    .filter(entry -> entry.getRequest().equals(request))
                    .findFirst()
                    .orElse(null);
            if (e == null) {
                throw new BeaconClientException("Unknown launch request " + request);
            }

            if (!(e.result instanceof ResultSuccess)) {
                throw new BeaconClientException("Invalid launch request state " + request);
            }

            e.setLaunched(true);
            return ((ResultSuccess) e.getResult()).getTargetScript();
        }
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

        @Setter
        @NonFinal
        boolean launched;
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
