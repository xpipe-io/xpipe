package io.xpipe.app.terminal;

import io.xpipe.app.auxw.WindowDockListener;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.auxw.NativeWinWindowControl;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.Rect;
import io.xpipe.app.util.ThreadHelper;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class TerminalDockView implements WindowDockListener {

    private final Set<TerminalView.ControllableTerminalSession> terminalInstances = new HashSet<>();

    private final UnaryOperator<Rect> windowBoundsFunction;

    private Rect viewBounds;
    private boolean viewActive;

    public TerminalDockView(UnaryOperator<Rect> windowBoundsFunction) {
        this.windowBoundsFunction = windowBoundsFunction;
    }

    public synchronized void clearDeadTerminals() {
        terminalInstances.removeIf(controllableTerminalSession ->
                !controllableTerminalSession.getTerminalProcess().isAlive());
    }

    public synchronized boolean isActive() {
        return viewActive && !terminalInstances.isEmpty() &&
                terminalInstances.stream().anyMatch(s -> s.getControllable().isActive() && !s.getControllable().isCustomBounds());
    }

    public synchronized boolean isRunning() {
        return terminalInstances.stream().anyMatch(terminal -> terminal.isRunning());
    }

    public synchronized boolean isCustomBounds() {
        return terminalInstances.stream().anyMatch(terminal -> terminal.getControllable().isCustomBounds());
    }

    public synchronized boolean isMinimized() {
        return terminalInstances.stream().noneMatch(terminal -> terminal.getControllable().isActive());
    }

    public synchronized void updateCustomBounds() {
        terminalInstances.forEach(terminal -> {
            var controllable = terminal.getControllable();
            var wasCustom = controllable.isCustomBounds();
            controllable.updateBoundsState();

            if (wasCustom && viewBounds != null && viewActive) {
                var currentBounds = controllable.getLastBounds();
                var targetBounds = windowBoundsFunction.apply(viewBounds);
                var sum = Math.abs(targetBounds.getX() - currentBounds.getX())
                        + Math.abs(targetBounds.getY() - currentBounds.getY())
                        + Math.abs(targetBounds.getW() - currentBounds.getW())
                        + Math.abs(targetBounds.getH() - currentBounds.getH());
                if (sum < 30) {
                    ThreadHelper.sleep(300);
                    trackTerminal(terminal, true);
                    return;
                }
            }

            if (!wasCustom && controllable.isCustomBounds()) {
                controllable.restoreIcon();
                controllable.disown();
                controllable.restoreStyle(terminal.manageBorders());
            }
        });
    }

    public synchronized void trackTerminal(TerminalView.ControllableTerminalSession terminal, boolean dock) {
        if (viewActive
                && dock
                && viewBounds != null
                && NativeWinWindowControl.MAIN_WINDOW.isVisible()
                && !NativeWinWindowControl.MAIN_WINDOW.isIconified()) {
            var controllable = terminal.getControllable();

            // Bring main window to foreground since initial launch
            NativeWinWindowControl.MAIN_WINDOW.activate();

            controllable.removeIcon();
            controllable.own(NativeWinWindowControl.MAIN_WINDOW);
            controllable.removeStyle(terminal.manageBorders());

            // The window might be minimized
            // We always want to show the terminal though
            controllable.show();

            // Move input focus to terminal
            controllable.focus();

            controllable.updatePosition(windowBoundsFunction.apply(viewBounds));
            updateCustomBounds();
        }

        var wasAdded = terminalInstances.add(terminal);
        if (wasAdded && viewActive && dock && viewBounds != null) {
            // Ugly fix for Windows Terminal instances using size constraints on first resize
            // This will cause the dock to interpret is as detached if we don't fix it again
            if (AppPrefs.get().terminalType().getValue() instanceof WindowsTerminalType) {
                GlobalTimer.delay(
                        () -> {
                            terminal.getControllable().updatePosition(windowBoundsFunction.apply(viewBounds));
                            updateCustomBounds();
                        },
                        Duration.ofMillis(100));
            }
        }
    }

    public synchronized boolean closeOtherTerminals(UUID request) {
        var others = terminalInstances.stream()
                .filter(terminal -> terminal.getTerminalProcess().isAlive())
                .filter(terminal -> TerminalView.get().getSessions().stream()
                        .noneMatch(shellSession -> shellSession.getRequest().equals(request)
                                && shellSession.getTerminal().equals(terminal)))
                .toList();
        for (TerminalView.ControllableTerminalSession other : others) {
            closeTerminal(other);
        }
        return others.size() > 0;
    }

    public synchronized void focus() {
        if (!viewActive) {
            return;
        }

        terminalInstances.forEach(terminalInstance -> {
            var controllable = terminalInstance.getControllable();
            if (!controllable.isActive()) {
                return;
            }

            controllable.updateBoundsState();
            if (controllable.isCustomBounds()) {
                return;
            }

            controllable.focus();
        });
    }

    public synchronized void closeTerminal(TerminalView.ControllableTerminalSession terminal) {
        if (!terminalInstances.contains(terminal)) {
            return;
        }

        var controllable =  terminal.getControllable();

        // Reset style in case close is blocked by terminal
        controllable.restoreIcon();
        controllable.disown();
        controllable.restoreStyle(terminal.manageBorders());

        controllable.close();
        // If the process blocked the exit, still don't track it anymore
        terminalInstances.remove(terminal);
    }

    public synchronized void activateView() {
        TrackEvent.withTrace("Terminal view activated").handle();
        if (viewActive) {
            return;
        }

        this.viewActive = true;
        terminalInstances.forEach(terminalInstance -> {
            var controllable = terminalInstance.getControllable();
            if (!controllable.isActive()) {
                return;
            }

            controllable.updateBoundsState();
            if (controllable.isCustomBounds()) {
                return;
            }

            controllable.removeIcon();
            controllable.own(NativeWinWindowControl.MAIN_WINDOW);
            controllable.removeStyle(terminalInstance.manageBorders());
            controllable.focus();
        });
        updatePositions();
    }

    public synchronized void deactivateView() {
        TrackEvent.withTrace("Terminal view deactivated").handle();
        if (!viewActive) {
            return;
        }

        this.viewActive = false;
        terminalInstances.forEach(terminalInstance -> {
            var controllable = terminalInstance.getControllable();
            controllable.disown();
            controllable.backOfWindow(NativeWinWindowControl.MAIN_WINDOW);
        });
        updatePositions();
    }

    public synchronized void onWindowShow() {
        TrackEvent.withTrace("Terminal view window shown").handle();
        terminalInstances.forEach(terminalInstance -> {
            var controllable = terminalInstance.getControllable();
            if (controllable.isActive()) {
                return;
            }

            controllable.updateBoundsState();
            if (controllable.isCustomBounds()) {
                return;
            }

            controllable.show();
            if (viewActive) {
                controllable.removeIcon();
                controllable.own(NativeWinWindowControl.MAIN_WINDOW);
                controllable.removeStyle(terminalInstance.manageBorders());
                controllable.focus();
            } else {
                controllable.restoreIcon();
                controllable.disown();
                controllable.backOfWindow(NativeWinWindowControl.MAIN_WINDOW);
            }
        });
    }

    public synchronized void onWindowMinimize() {
        TrackEvent.withTrace("Terminal view window minimized").handle();

        terminalInstances.forEach(terminalInstance -> {
            var controllable = terminalInstance.getControllable();
            controllable.updateBoundsState();
            if (controllable.isCustomBounds()) {
                return;
            }

            controllable.minimize();
        });
    }

    public synchronized void onClose() {
        TrackEvent.withTrace("Terminal view closed").handle();

        terminalInstances.forEach(terminalInstance -> {
            var controllable = terminalInstance.getControllable();
            controllable.updateBoundsState();
            if (controllable.isCustomBounds()) {
                return;
            }

            closeTerminal(terminalInstance);
        });
        terminalInstances.clear();
    }

    private void updatePositions() {
        if (viewBounds == null) {
            return;
        }

        terminalInstances.forEach(terminalInstance -> {
            var controllable = terminalInstance.getControllable();
            if (!controllable.isActive()) {
                return;
            }

            controllable.updateBoundsState();
            if (controllable.isCustomBounds()) {
                return;
            }

            controllable.updatePosition(windowBoundsFunction.apply(viewBounds));
        });
    }

    public synchronized void resizeView(int x, int y, int w, int h) {
        if (w < 100 || h < 100) {
            return;
        }

        this.viewBounds = new Rect(x, y, w, h);
        updatePositions();
    }

    public void attach() {
        if (viewBounds == null) {
            return;
        }

        TrackEvent.withTrace("Terminal view attached").handle();

        terminalInstances.forEach(terminalInstance -> {
            var controllable = terminalInstance.getControllable();
            controllable.show();
            controllable.removeIcon();
            controllable.own(NativeWinWindowControl.MAIN_WINDOW);
            controllable.removeStyle(terminalInstance.manageBorders());
            controllable.updatePosition(windowBoundsFunction.apply(viewBounds));
            controllable.focus();
        });
    }
}
