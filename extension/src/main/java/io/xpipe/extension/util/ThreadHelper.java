package io.xpipe.extension.util;

import org.apache.commons.lang3.function.FailableRunnable;

public class ThreadHelper {

    public static void await(FailableRunnable<InterruptedException> r) {
        try {
            r.run();
        } catch (InterruptedException e) {
        }
    }

    public static Thread runAsync(Runnable r) {
        var t = new Thread(r);
        t.setDaemon(true);
        t.start();
        return t;
    }

    public static Thread create(String name, boolean daemon, Runnable r) {
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
