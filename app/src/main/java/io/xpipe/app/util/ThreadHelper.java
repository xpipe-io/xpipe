package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import org.apache.commons.lang3.function.FailableRunnable;

public class ThreadHelper {

    private static Thread unstarted(Runnable r) {
        return AppProperties.get().isUseVirtualThreads() ? Thread.ofVirtual().unstarted(r) : Thread.ofPlatform().unstarted(r);
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
}
