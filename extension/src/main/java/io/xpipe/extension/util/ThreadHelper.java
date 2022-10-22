package io.xpipe.extension.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ThreadHelper {

    public static Thread run(Runnable r) {
        var t = new Thread(r);
        t.setDaemon(true);
        t.start();
        return t;
    }

    public static <T> T run(Supplier<T> r) {
        AtomicReference<T> ret = new AtomicReference<>();
        var t = new Thread(() -> ret.set(r.get()));
        t.setDaemon(true);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            return null;
        }
        return ret.get();
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
