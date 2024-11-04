package io.xpipe.app.terminal;

import io.xpipe.app.core.window.NativeWinWindowControl;
import io.xpipe.app.util.Rect;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(
        makeFinal = true,
        level = AccessLevel.PRIVATE)
public final class WindowsTerminalViewInstance extends TerminalViewInstance {

    NativeWinWindowControl control;

    public WindowsTerminalViewInstance(ProcessHandle terminal, NativeWinWindowControl control) {
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
        this.control.removeBorders();
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
    public void updatePosition(Rect bounds) {
        control.move(bounds);
        this.lastBounds = bounds;
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
}
