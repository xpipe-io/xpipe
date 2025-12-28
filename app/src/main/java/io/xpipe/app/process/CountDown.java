package io.xpipe.app.process;

import lombok.Getter;
import lombok.Setter;

public class CountDown {

    private volatile long lastMillis = -1;
    private volatile long millisecondsLeft;

    @Setter
    private volatile boolean active;

    @Getter
    private volatile long maxMillis;

    private CountDown() {}

    public static CountDown of() {
        return new CountDown();
    }

    public synchronized CountDown start(long millisecondsLeft) {
        this.millisecondsLeft = millisecondsLeft;
        this.maxMillis = millisecondsLeft;
        lastMillis = System.currentTimeMillis();
        active = true;
        return this;
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

    public synchronized long getMillisecondsElapsed() {
        return maxMillis - millisecondsLeft;
    }
}
