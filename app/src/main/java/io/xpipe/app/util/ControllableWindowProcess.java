package io.xpipe.app.util;

import lombok.Getter;

@Getter
public abstract class ControllableWindowProcess {

    protected final ProcessHandle process;
    protected Rect lastBounds;
    protected boolean customBounds;

    protected ControllableWindowProcess(ProcessHandle p) {
        this.process = p;
    }

    public abstract boolean isRunning();

    public abstract void own(NativeWinWindowControl window);

    public abstract void disown();

    public abstract void removeIcon();

    public abstract void hide();

    public abstract void restoreIcon();

    public abstract void removeStyle(boolean borders);

    public abstract void restoreStyle(boolean borders);

    public abstract void show();

    public abstract void minimize();

    public abstract void moveToFront();

    public abstract void backOfWindow(NativeWinWindowControl window);

    public abstract void focus();

    public abstract void updatePosition(Rect bounds);

    public abstract void close();

    public abstract boolean isActive();

    public abstract boolean isDestroyed();

    public abstract boolean isDialog();

    public abstract Rect queryBounds();

    public abstract Object getRawHandle();

    public void updateBoundsState() {
        if (!isActive()) {
            return;
        }

        var bounds = queryBounds();
        if (lastBounds != null && !lastBounds.equals(bounds)) {
            customBounds = true;
        }
        lastBounds = bounds;
    }
}
