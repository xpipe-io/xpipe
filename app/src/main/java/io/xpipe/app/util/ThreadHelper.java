package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.util.FailableRunnable;
import lombok.SneakyThrows;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadHelper {

    private static final AtomicInteger counter = new AtomicInteger();

    private static Runnable wrap(Runnable r) {
        return () -> {
            if (AppProperties.get().isDebugThreads()) {
                TrackEvent.trace("Started. Active threads: " + counter.incrementAndGet());
            }
            r.run();
            if (AppProperties.get().isDebugThreads()) {
                TrackEvent.trace("Finished. Active threads: " + counter.decrementAndGet());
            }
        };
    }

    public static Thread unstarted(Runnable r) {
        return AppProperties.get().isUseVirtualThreads()
                ? Thread.ofVirtual().unstarted(wrap(r))
                : Thread.ofPlatform().unstarted(wrap(r));
    }

    public static Thread runAsync(Runnable r) {
        var t = unstarted(r);
        t.setDaemon(true);
        t.start();
        return t;
    }

    public static Thread runFailableAsync(FailableRunnable<Throwable> r) {
        var t = unstarted(() -> {
            try {
                r.run();
            } catch (Throwable e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    public static Thread createPlatformThread(String name, boolean daemon, Runnable r) {
        var t = new Thread(r);
        t.setDaemon(daemon);
        t.setName(name);
        return t;
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SafeVarargs
    @SneakyThrows
    public static void load(boolean terminal, FailableRunnable<Throwable>... r) {
        var latch = new CountDownLatch(r.length);
        for (var i = 0; i < r.length; i++) {
            var runnable = r[i];
            var thread = ThreadHelper.createPlatformThread("init-" + i, false, () -> {
                try {
                    runnable.run();
                    latch.countDown();
                } catch (Throwable e) {
                    ErrorEvent.fromThrowable(e).terminal(terminal).handle();
                    latch.countDown();
                }
            });
            thread.start();
        }
        latch.await();
    }
}
