package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;

import java.time.Duration;

public abstract class Session implements AutoCloseable {

    protected SessionListener listener = running -> {};

    public synchronized void addListener(SessionListener n) {
        var current = this.listener;
        this.listener = running -> {
            current.onStateChange(running);
            n.onStateChange(running);
        };
    }

    protected void startAliveListener() {
        GlobalTimer.scheduleUntil(Duration.ofMillis(5000), () -> {
            if (!isRunning()) {
                return true;
            }

            ThreadHelper.runAsync(() -> {
                try {
                    var r = checkAlive();
                    if (r) {
                        return;
                    }
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).omit().handle();
                }

                try {
                    stop();
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).omit().handle();
                }
            });
            return false;
        });
    }

    public abstract boolean isRunning();

    public abstract void start() throws Exception;

    public abstract void stop() throws Exception;

    public abstract boolean checkAlive() throws Exception;

    @Override
    public void close() throws Exception {
        stop();
    }
}
