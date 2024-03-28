package io.xpipe.app.util;

import javafx.animation.AnimationTimer;
import javafx.beans.value.ObservableBooleanValue;

import java.util.concurrent.atomic.AtomicReference;

public class BooleanAnimationTimer {

    private final ObservableBooleanValue value;
    private final int duration;
    private final Runnable toExecute;

    public BooleanAnimationTimer(ObservableBooleanValue value, int duration, Runnable toExecute) {
        this.value = value;
        this.duration = duration;
        this.toExecute = toExecute;
    }

    public void start() {
        var timer = new AtomicReference<AnimationTimer>();
        value.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                if (timer.get() == null) {
                    timer.set(new AnimationTimer() {

                        long init = 0;

                        @Override
                        public void handle(long now) {
                            if (init == 0) {
                                init = now;
                            }

                            var nowMs = now;
                            if ((nowMs - init) > duration * 1_000_000L) {
                                toExecute.run();
                                stop();
                            }
                        }
                    });
                    timer.get().start();
                }
            } else {
                if (timer.get() != null) {
                    timer.get().stop();
                    timer.set(null);
                }
            }
        });
    }
}
