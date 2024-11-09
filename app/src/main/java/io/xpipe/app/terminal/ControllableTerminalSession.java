package io.xpipe.app.terminal;

import io.xpipe.app.util.Rect;

import lombok.Getter;

@Getter
public abstract class ControllableTerminalSession extends TerminalView.TerminalSession {

    protected Rect lastBounds;
    protected boolean customBounds;

    protected ControllableTerminalSession(ProcessHandle terminalProcess) {
        super(terminalProcess);
    }

    public abstract void show();

    public abstract void minimize();

    public abstract void alwaysInFront();

    public abstract void back();

    public abstract void frontOfMainWindow();

    public abstract void focus();

    public abstract void updatePosition(Rect bounds);

    public abstract void close();

    public abstract boolean isActive();

    public abstract Rect queryBounds();

    public final void updateBoundsState() {
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
