package io.xpipe.app.terminal;

import com.sun.jna.platform.win32.User32;
import io.xpipe.app.platform.NativeWinWindowControl;
import io.xpipe.app.util.Rect;

import io.xpipe.app.util.User32Ex;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Objects;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public final class WindowsTerminalSession extends ControllableTerminalSession {

    NativeWinWindowControl control;

    public WindowsTerminalSession(ProcessHandle terminal, NativeWinWindowControl control) {
        super(terminal);
        this.control = control;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WindowsTerminalSession that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(control.getWindowHandle(), that.control.getWindowHandle());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), control.getWindowHandle());
    }

    @Override
    public boolean isRunning() {
        return super.isRunning() && control.isVisible();
    }

    @Override
    public void own() {
        control.takeOwnership(NativeWinWindowControl.MAIN_WINDOW.getWindowHandle());
    }

    @Override
    public void disown() {
        control.releaseOwnership();
    }

    @Override
    public void removeBorders() {
        control.removeBorders();
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
    public void backOfMainWindow() {
        getControl().orderRelative(NativeWinWindowControl.MAIN_WINDOW.getWindowHandle());
    }

    @Override
    public void frontOfMainWindow() {
        this.control.moveToFront();
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
        if (control.isIconified()) {
            return false;
        }

        if (!control.isVisible()) {
            return false;
        }

        var bounds = queryBounds();
        if (bounds.getX() == 0 && bounds.getY() == 0 && bounds.getW() == 0 && bounds.getH() == 0) {
            return false;
        }

        return true;
    }

    @Override
    public Rect queryBounds() {
        return control.getBounds();
    }

    public void updateBoundsState() {
        if (!isActive()) {
            return;
        }

        var bounds = queryBounds();
        if (bounds.getX() == -32000 || bounds.getY() == -32000) {
            return;
        }

        if (lastBounds != null && (lastBounds.getX() == -32000 || lastBounds.getY() == -32000)) {
            return;
        }

        if (lastBounds != null && !lastBounds.equals(bounds)) {
            customBounds = true;
        }
        lastBounds = bounds;
    }
}
