package io.xpipe.app.terminal;

import io.xpipe.app.core.window.NativeWinWindowControl;
import io.xpipe.app.util.Rect;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public final class WindowsTerminalSession extends ControllableTerminalSession {

    NativeWinWindowControl control;

    public WindowsTerminalSession(ProcessHandle terminal, NativeWinWindowControl control) {
        super(terminal);
        this.control = control;
    }

    @Override
    public void show() {
        this.control.show();
    }

    @Override
    public void minimize() {
        this.control.minimize();
    }

    @Override
    public void alwaysInFront() {
        this.control.alwaysInFront();
        // this.control.removeBorders();
    }

    @Override
    public void back() {
        control.defaultOrder();
        NativeWinWindowControl.MAIN_WINDOW.alwaysInFront();
        NativeWinWindowControl.MAIN_WINDOW.defaultOrder();
    }

    @Override
    public void frontOfMainWindow() {
        this.control.alwaysInFront();
        this.control.defaultOrder();
    }

    @Override
    public void focus() {
        this.control.activate();
    }

    @Override
    public void updatePosition(Rect bounds) {
        control.move(bounds);
        this.lastBounds = queryBounds();
        this.customBounds = false;
    }

    @Override
    public void close() {
        this.control.close();
    }

    @Override
    public boolean isActive() {
        return !control.isIconified();
    }

    @Override
    public Rect queryBounds() {
        return control.getBounds();
    }


    public void updateBoundsState() {
        if (!control.isIconified() || !control.isVisible()) {
            return;
        }

        var bounds = queryBounds();
        if (bounds.getX() == -32000 || bounds.getY() == -32000) {
            return;
        }

        if (lastBounds != null && !lastBounds.equals(bounds)) {
            customBounds = true;
        }
        lastBounds = bounds;
    }
}
