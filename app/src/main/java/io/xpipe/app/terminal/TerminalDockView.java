package io.xpipe.app.terminal;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.Rect;

import lombok.Getter;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class TerminalDockView {

    @Getter
    private final Set<ControllableTerminalSession> terminalInstances = new HashSet<>();

    private final UnaryOperator<Rect> windowBoundsFunction;

    private Rect viewBounds;
    private boolean viewActive;

    public TerminalDockView(UnaryOperator<Rect> windowBoundsFunction) {
        this.windowBoundsFunction = windowBoundsFunction;
    }

    public synchronized void clearDeadTerminals() {
        terminalInstances.removeIf(controllableTerminalSession -> !controllableTerminalSession.getTerminalProcess().isAlive());
    }

    public synchronized boolean isRunning() {
        return terminalInstances.stream().anyMatch(terminal -> terminal.isRunning());
    }

    public synchronized boolean isCustomBounds() {
        return terminalInstances.stream().anyMatch(terminal -> terminal.isCustomBounds());
    }

    public synchronized boolean isMinimized() {
        return terminalInstances.stream().noneMatch(terminal -> terminal.isActive());
    }

    public synchronized void updateCustomBounds() {
        terminalInstances.forEach(terminal -> terminal.updateBoundsState());
    }

    public synchronized void trackTerminal(ControllableTerminalSession terminal, boolean dock) {
        if (!terminalInstances.add(terminal)) {
            return;
        }

        // The main window always loses focus when the terminal is opened,
        // so only put it in front
        // If we refocus the main window, it will get put always in front then
        terminal.frontOfMainWindow();
        if (dock && viewBounds != null) {
            terminal.updatePosition(windowBoundsFunction.apply(viewBounds));

            // Ugly fix for Windows Terminal instances using size constraints on first resize
            // This will cause the dock to interpret is as detached if we don't fix it again
            if (AppPrefs.get().terminalType().getValue() instanceof WindowsTerminalType) {
                GlobalTimer.delay(
                        () -> {
                            terminal.updatePosition(windowBoundsFunction.apply(viewBounds));
                        },
                        Duration.ofMillis(100));
                GlobalTimer.delay(
                        () -> {
                            terminal.updatePosition(windowBoundsFunction.apply(viewBounds));
                        },
                        Duration.ofMillis(1000));
            }
        }
    }

    public synchronized boolean closeOtherTerminals(UUID request) {
        var sessions = TerminalView.get().getSessions();
        var tv = sessions.stream()
                .filter(s -> request.equals(s.getRequest()) && s.getTerminal().isRunning())
                .map(s -> s.getTerminal().controllable())
                .flatMap(Optional::stream)
                .toList();
        for (int i = 0; i < tv.size() - 1; i++) {
            closeTerminal(tv.get(i));
        }
        return tv.size() > 1;
    }

    public synchronized void closeTerminal(ControllableTerminalSession terminal) {
        if (!terminalInstances.contains(terminal)) {
            return;
        }

        terminal.close();
        terminalInstances.remove(terminal);
    }

    public synchronized void toggleView(boolean active) {
        TrackEvent.withTrace("Terminal view toggled").tag("active", active).handle();
        if (viewActive == active) {
            return;
        }

        this.viewActive = active;
        if (active) {
            terminalInstances.forEach(terminalInstance -> {
                terminalInstance.frontOfMainWindow();
                terminalInstance.focus();
            });
            updatePositions();
        } else {
            terminalInstances.forEach(terminalInstance -> terminalInstance.back());
        }
    }

    public synchronized void onFocusGain() {
        if (!viewActive) {
            return;
        }

        TrackEvent.withTrace("Terminal view focus gained").handle();
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

        TrackEvent.withTrace("Terminal view focus lost").handle();
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
        TrackEvent.withTrace("Terminal view window activated").handle();
        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.show();
            if (viewActive) {
                terminalInstance.frontOfMainWindow();
                terminalInstance.focus();
            } else {
                terminalInstance.back();
            }
        });
    }

    public synchronized void onWindowMinimize() {
        TrackEvent.withTrace("Terminal view window minimized").handle();

        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.minimize();
        });
    }

    public synchronized void onClose() {
        TrackEvent.withTrace("Terminal view closed").handle();

        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.close();
        });
        terminalInstances.clear();
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

            terminalInstance.updatePosition(windowBoundsFunction.apply(viewBounds));
        });
    }

    public void resizeView(int x, int y, int w, int h) {
        if (w < 100 || h < 100) {
            return;
        }

        this.viewBounds = new Rect(x, y, w, h);
        updatePositions();
    }

    public void attach() {
        TrackEvent.withTrace("Terminal view attached").handle();

        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.show();
            terminalInstance.frontOfMainWindow();
            terminalInstance.focus();
            terminalInstance.updatePosition(windowBoundsFunction.apply(viewBounds));
        });
    }
}
