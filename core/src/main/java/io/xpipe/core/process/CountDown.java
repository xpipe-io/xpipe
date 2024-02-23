package io.xpipe.core.process;

import lombok.Getter;
import lombok.Setter;

public class CountDown {

    private long lastMillis = -1;
    private long millisecondsLeft;

    @Setter
    private boolean active;

    @Getter
    private long maxMillis;

    private CountDown() {}

    public static CountDown of() {
        return new CountDown();
    }

    public synchronized void start(long millisecondsLeft) {
        this.millisecondsLeft = millisecondsLeft;
        this.maxMillis = millisecondsLeft;
        lastMillis = System.currentTimeMillis();
        active = true;
    }

    public void pause() {
        lastMillis = System.currentTimeMillis();
        setActive(false);
    }

    public void resume() {
        lastMillis = System.currentTimeMillis();
        setActive(true);
    }

    public synchronized boolean countDown() {
        var ml = System.currentTimeMillis();
        if (!active) {
            lastMillis = ml;
            return true;
        }

        var diff = ml - lastMillis;
        lastMillis = ml;
        millisecondsLeft -= diff;
        if (millisecondsLeft < 0) {
            return false;
        }
        return true;
    }
}
