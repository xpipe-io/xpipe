package io.xpipe.app.util;

import io.xpipe.core.util.FailableRunnable;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

public class GlobalTimer {

    private static Timer TIMER;

    public static void init() {
        TIMER = new Timer("global-timer", true);
    }

    public static void reset() {
        if (TIMER == null) {
            return;
        }

        TIMER.cancel();
        TIMER = null;
    }

    public static void scheduleUntil(Duration interval, Supplier<Boolean> s) {
        var task = new TimerTask() {
            @Override
            public void run() {
                if (!s.get()) {
                    return;
                }

                cancel();
            }
        };
        TIMER.scheduleAtFixedRate(task, 0, interval.toMillis());
    }

    public static void delay(Runnable r, Duration delay) {
        TIMER.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        r.run();
                    }
                },
                delay.toMillis());
    }

    public static void delayAsync(Runnable r, Duration delay) {
        TIMER.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        ThreadHelper.runAsync(r);
                    }
                },
                delay.toMillis());
    }

    public static void delayFailableAsync(FailableRunnable<Throwable> r, Duration delay) {
        TIMER.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        ThreadHelper.runFailableAsync(r);
                    }
                },
                delay.toMillis());
    }
}
