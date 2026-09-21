package io.xpipe.app.util;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppOpenArguments;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.ext.ProcModuleProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.*;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Getter
public class ElevatedExec {

    private static final List<ElevatedExec> execs = new ArrayList<>();
    private static boolean daemonRunning = false;

    private static synchronized void startElevatedDaemonIfNeeded() throws Exception {
        if  (daemonRunning) {
            return;
        }

        var cmd = AppInstallation.ofCurrent().getDaemonExecutablePath();
        ProcModuleProvider.get().runElevatedCommand(CommandBuilder.of().addFile(cmd)
                .add("-Dio.xpipe.app.dataDir=\"" + AppProperties.get().getDataDir() + "\"")
                .add("-Dio.xpipe.app.elevatedExecMode=true")
                .add("-Dio.xpipe.app.beaconPort=" + AppBeaconServer.get().getPort()));

        var exec = scheduleElevated(CommandBuilder.of().add(LocalShell.getDialect().getPrintWorkingDirectoryCommand()));
        var success = exec.getLatch().await(30, TimeUnit.SECONDS);
        if (!success) {
            throw ErrorEventFactory.expected(new IllegalStateException("Elevated daemon did not start up in time"));
        }
        daemonRunning = true;
    }

    public static ElevatedExec runElevated(CommandBuilder b) throws Exception {
        startElevatedDaemonIfNeeded();

        var exec = scheduleElevated(b);
        for (int i = 0; i < 30; i++) {
            if (exec.isFinished()) {
                return exec;
            }

            Thread.sleep(1000);
        }
        throw ErrorEventFactory.expected(new IllegalStateException("Elevated command did not complete in time"));
    }

    private static ElevatedExec scheduleElevated(CommandBuilder b) throws Exception {
        var full = b.buildFull(LocalShell.getShell());
        var id = UUID.randomUUID();
        var exec = new ElevatedExec(id, LocalShell.getDialect(), ShellScript.of(full));
        synchronized (execs) {
            execs.add(exec);
        }
        return exec;
    }

    public static void completeElevatedExec(UUID id, String stdout, String stderr, long exitCode) {
        var e = getElevatedExec(id);
        if (e.isEmpty()) {
            return;
        }

        e.get().stdout = stdout;
        e.get().stderr = stderr;
        e.get().exitCode = exitCode;
        e.get().latch.countDown();
    }

    public static Optional<ElevatedExec> getNextElevatedExec() {
        while (!AppOperationMode.isInShutdown()) {
            Optional<ElevatedExec> e;
            synchronized (execs) {
                e = execs.stream().filter(x -> x.isScheduled()).findFirst();
            }
            if (e.isPresent()) {
                e.get().getLatch().countDown();
                return e;
            } else {
                ThreadHelper.sleep(100);
            }
        }
        return Optional.empty();
    }

    public static Optional<ElevatedExec> getElevatedExec(UUID id) {
        synchronized (execs) {
            var e = execs.stream().filter(x -> x.getId().equals(id)).findFirst();
            return e;
        }
    }

    public ElevatedExec(UUID id, ShellDialect dialect, ShellScript command) {
        this.id = id;
        this.dialect = dialect;
        this.command = command;
    }

    private final UUID id;
    private final ShellDialect dialect;
    private final ShellScript command;
    private final CountDownLatch latch = new CountDownLatch(2);

    private String stdout;
    private String stderr;
    private long exitCode;

    public void throwIfFailed() throws ProcessOutputException {
        if (exitCode != 0) {
            throw ProcessOutputException.of(exitCode, stdout, stderr);
        }
    }

    private boolean isScheduled() {
        return latch.getCount() == 2;
    }

    private boolean isActive() {
        return latch.getCount() == 1;
    }

    private boolean isFinished() {
        return latch.getCount() == 0;
    }
}
