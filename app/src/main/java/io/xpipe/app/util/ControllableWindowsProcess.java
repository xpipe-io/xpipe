package io.xpipe.app.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Objects;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public final class ControllableWindowsProcess extends ControllableWindowProcess {

    NativeWinWindowControl control;

    public ControllableWindowsProcess(ProcessHandle p, NativeWinWindowControl control) {
        super(p);
        this.control = control;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ControllableWindowsProcess that)) {
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
        return process.isAlive();
    }

    @Override
    public void own(NativeWinWindowControl window) {
        control.takeOwnership(window.getWindowHandle());
    }

    @Override
    public void disown() {
        control.releaseOwnership();
    }

    @Override
    public void removeIcon() {
        control.removeIcon();
    }

    @Override
    public void hide() {
        control.hide();
    }

    @Override
    public void restoreIcon() {
        control.restoreIcon();
    }

    @Override
    public void removeStyle(boolean borders) {
        control.setWindowsTransitionsEnabled(false);
        if (borders) {
            control.removeBorders();
        }
    }

    @Override
    public void restoreStyle(boolean borders) {
        control.setWindowsTransitionsEnabled(true);
        if (borders) {
            control.restoreBorders();
        }
    }

    @Override
    public void show() {
        this.control.restore();
    }

    @Override
    public void minimize() {
        this.control.minimize();
    }

    @Override
    public void backOfWindow(NativeWinWindowControl window) {
        getControl().orderRelative(window.getWindowHandle());
    }

    @Override
    public void moveToFront() {
        this.control.moveToFront();
    }

    @Override
    public void focus() {
        this.control.activate();
    }

    @Override
    public void updatePosition(Rect bounds) {
        // In case it is maximized, make it normal again
        control.restore();
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
    public boolean isDestroyed() {
        return control.isDestroyed();
    }

    @Override
    public boolean isDialog() {
        return control.isDialog();
    }

    @Override
    public Rect queryBounds() {
        return control.getBounds();
    }

    @Override
    public Object getRawHandle() {
        return control.getWindowHandle();
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
