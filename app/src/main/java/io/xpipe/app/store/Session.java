package io.xpipe.app.store;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Session implements AutoCloseable {

    protected SessionListener listener = running -> {};

    private final AtomicInteger aliveListenerGeneration = new AtomicInteger();

    public synchronized void addListener(SessionListener n) {
        var current = this.listener;
        this.listener = running -> {
            current.onStateChange(running);
            n.onStateChange(running);
        };
    }

    protected void startAliveListener() {
        // Each start registers a new listener, older ones must not act on the newer control
        var generation = aliveListenerGeneration.incrementAndGet();
        GlobalTimer.scheduleUntil(Duration.ofMillis(10000), false, () -> {
            if (generation != aliveListenerGeneration.get() || !isRunning()) {
                return true;
            }

            ThreadHelper.runAsync(() -> {
                if (generation != aliveListenerGeneration.get() || !isRunning()) {
                    return;
                }

                var dead = !checkAliveQuiet() || checkInactive();
                if (!dead) {
                    return;
                }

                synchronized (this) {
                    // The session might have been restarted while we were checking
                    if (generation != aliveListenerGeneration.get()) {
                        return;
                    }

                    handleSessionDeath();
                }
            });
            return false;
        });
    }

    protected void handleSessionDeath() {
        try {
            stop();
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).omit().handle();
        }
    }

    protected abstract boolean isRunning();

    public abstract void start() throws Exception;

    public abstract void stop() throws Exception;

    public boolean checkAliveQuiet() {
        try {
            return checkAlive();
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).omit().handle();
            return false;
        }
    }

    public abstract boolean checkInactive();

    public abstract boolean checkAlive() throws Exception;

    @Override
    public void close() throws Exception {
        stop();
    }
}
