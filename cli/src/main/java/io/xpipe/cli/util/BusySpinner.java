package io.xpipe.cli.util;

import java.io.Closeable;

public class BusySpinner implements Closeable, AutoCloseable {

    private static Thread thread;
    private static volatile SpinnerAnimation INSTANCE;

    private static void init() {
        thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                    break;
                }

                if (INSTANCE != null) {
                    INSTANCE.update(500);
                }
            }
        });
        thread.setDaemon(true);

        if (!TerminalHelper.isDumb()) {
            thread.start();
        }
    }

    public static BusySpinner start(String text, boolean countSeconds) {
        if (thread == null) {
            init();
        }

        INSTANCE = new SpinnerAnimation(text, countSeconds);
        if (!TerminalHelper.isDumb()) {
            INSTANCE.update(0);
        }
        return new BusySpinner();
    }

    public static void stop() {
        if (!TerminalHelper.isDumb() && INSTANCE != null) {
            INSTANCE.clear();
        }
        INSTANCE = null;
    }

    @Override
    public void close() {
        stop();
    }
}
