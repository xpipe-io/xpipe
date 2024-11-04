package io.xpipe.app.terminal;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.Rect;
import io.xpipe.core.process.OsType;

import java.util.ArrayList;
import java.util.List;

public class TerminalDockModel {

    public static boolean isSupported() {
        return OsType.getLocal() == OsType.WINDOWS;
    }

    private Rect viewBounds;
    private boolean viewActive;
    private final List<TerminalViewInstance> terminalInstances = new ArrayList<>();

    public synchronized void trackTerminal(TerminalViewInstance terminal) {
        terminalInstances.add(terminal);
    }

    public boolean isEnabled() {
        return isSupported() && AppPrefs.get().enableTerminalDocking().get();
    }

    public synchronized void toggleView(boolean active) {
        TrackEvent.withTrace("Terminal view toggled")
                .tag("active", active)
                .handle();
        if (viewActive == active) {
            return;
        }

        this.viewActive = active;
        if (active) {
            terminalInstances.forEach(terminalInstance -> terminalInstance.alwaysInFront());
            updatePositions();
        } else {
            terminalInstances.forEach(terminalInstance -> terminalInstance.back());
        }
    }

    public synchronized void onFocusGain() {
        if (!viewActive) {
            return;
        }

        TrackEvent.withTrace("Terminal view focus gained")
                .handle();
        terminalInstances.forEach(terminalInstance -> {
            if (!terminalInstance.isActive()) {
                return;
            }

            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.show();
            terminalInstance.alwaysInFront();
        });
    }

    public synchronized void onFocusLost() {
        if (!viewActive) {
            return;
        }

        TrackEvent.withTrace("Terminal view focus lost")
                .handle();
        terminalInstances.forEach(terminalInstance -> {
            if (!terminalInstance.isActive()) {
                return;
            }

            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.frontOfMainWindow();
        });
    }

    public synchronized void onWindowActivate() {
        TrackEvent.withTrace("Terminal view focus gained")
                .handle();
        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.show();
            if (viewActive) {
                terminalInstance.alwaysInFront();
            } else {
                terminalInstance.back();
            }
        });
    }

    public synchronized void onWindowMinimize() {
        TrackEvent.withTrace("Terminal view minimized")
                .handle();

        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.minimize();
        });
    }

    public synchronized void onClose() {
        TrackEvent.withTrace("Terminal view closed")
                .handle();

        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.close();
        });
    }

    private void updatePositions() {
        if (viewBounds == null) {
            return;
        }

        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.updatePosition(viewBounds);
        });
    }

    public void resizeView(int x, int y, int w, int h) {
        if (w < 100 || h < 100) {
            return;
        }

        this.viewBounds = new Rect(x,y,w,h);
        TrackEvent.withTrace("Terminal view resized")
                .tag("rect", viewBounds)
                .handle();
        updatePositions();
    }

    public void clickView() {
        TrackEvent.withTrace("Terminal view clicked")
                .handle();

        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.show();
            terminalInstance.alwaysInFront();
            terminalInstance.updatePosition(viewBounds);
        });
    }
}
