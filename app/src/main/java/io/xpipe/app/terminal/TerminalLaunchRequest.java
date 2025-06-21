package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;

import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@Value
public class TerminalLaunchRequest {

    UUID request;
    ProcessControl processControl;
    TerminalInitScriptConfig config;
    FilePath workingDirectory;

    @Setter
    @NonFinal
    long shellPid;

    @Setter
    @NonFinal
    TerminalLaunchResult result;

    @Setter
    @NonFinal
    boolean setupCompleted;

    @NonFinal
    CountDownLatch latch;

    public Path waitForCompletion() throws BeaconServerException {
        while (true) {
            if (latch.getCount() > 0) {
                ThreadHelper.sleep(10);
                continue;
            }

            if (getResult() == null) {
                throw ErrorEventFactory.expected(new BeaconServerException("Launch request aborted"));
            }

            var r = getResult();
            if (r instanceof TerminalLaunchResult.ResultFailure failure) {
                var t = failure.getThrowable();
                throw new BeaconServerException(t);
            }

            return ((TerminalLaunchResult.ResultSuccess) r).getTargetScript();
        }
    }

    public void setupRequestAsync() {
        latch = new CountDownLatch(1);
        ThreadHelper.runAsync(() -> {
            setupRequest();
            latch.countDown();
        });
    }

    public void abort() {
        latch.countDown();
    }

    private void setupRequest() {
        var wd = new WorkingDirectoryFunction() {

            @Override
            public boolean isFixed() {
                return true;
            }

            @Override
            public boolean isSpecified() {
                return workingDirectory != null;
            }

            @Override
            public FilePath apply(ShellControl shellControl) {
                if (workingDirectory == null) {
                    return null;
                }

                return workingDirectory;
            }
        };

        try {
            var openCommand = processControl.prepareTerminalOpen(config, wd);
            var file = ScriptHelper.createLocalExecScript(openCommand);
            setResult(new TerminalLaunchResult.ResultSuccess(file.asLocalPath()));
        } catch (Exception e) {
            setResult(new TerminalLaunchResult.ResultFailure(e));
        }
    }
}
