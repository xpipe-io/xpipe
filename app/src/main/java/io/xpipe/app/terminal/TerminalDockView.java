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
        terminalInstances.removeIf(controllableTerminalSession ->
                !controllableTerminalSession.getTerminalProcess().isAlive());
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
        terminalInstances.forEach(terminal -> {
            terminal.updateBoundsState();
            if (terminal.isCustomBounds()) {
                terminal.disown();
            }
        });
    }

    public synchronized void trackTerminal(ControllableTerminalSession terminal, boolean dock) {
        if (viewActive && dock && viewBounds != null) {
            // The window might be minimized
            // We always want to show the terminal though
            terminal.show();

            terminal.own();

            terminal.updatePosition(windowBoundsFunction.apply(viewBounds));
            updateCustomBounds();
        }

        var wasAdded = terminalInstances.add(terminal);
        if (wasAdded && viewActive && dock && viewBounds != null) {
            // Ugly fix for Windows Terminal instances using size constraints on first resize
            // This will cause the dock to interpret is as detached if we don't fix it again
            if (AppPrefs.get().terminalType().getValue() instanceof WindowsTerminalType) {
                GlobalTimer.delay(
                        () -> {
                            terminal.updatePosition(windowBoundsFunction.apply(viewBounds));
                            updateCustomBounds();
                        },
                        Duration.ofMillis(100));
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

    public synchronized void activateView() {
        TrackEvent.withTrace("Terminal view activated").handle();
        if (viewActive) {
            return;
        }

        this.viewActive = true;
        terminalInstances.forEach(terminalInstance -> {
            if (!terminalInstance.isActive()) {
                return;
            }

            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.own();
            terminalInstance.focus();
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
            terminalInstance.disown();
            terminalInstance.backOfMainWindow();
        });
        updatePositions();
    }

    public synchronized void onWindowShow() {
        TrackEvent.withTrace("Terminal view window shown").handle();
        terminalInstances.forEach(terminalInstance -> {
            if (terminalInstance.isActive()) {
                return;
            }

            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.show();
            if (viewActive) {
                terminalInstance.own();
                terminalInstance.focus();
            } else {
                terminalInstance.disown();
                terminalInstance.backOfMainWindow();
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
            if (!terminalInstance.isActive()) {
                return;
            }

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
            terminalInstance.updatePosition(windowBoundsFunction.apply(viewBounds));
            terminalInstance.own();
            terminalInstance.focus();
        });
    }
}
