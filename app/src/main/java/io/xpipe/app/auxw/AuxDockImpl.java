package io.xpipe.app.auxw;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.Rect;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class AuxDockImpl implements WindowDockListener {

    @Getter
    private final Set<AuxEntry> entries = new HashSet<>();
    @Getter
    private AuxEntry selected;

    private final UnaryOperator<Rect> windowBoundsFunction;
    private final Supplier<NativeWinWindowControl> parent;

    @Getter
    private Rect viewBounds;

    public AuxDockImpl(UnaryOperator<Rect> windowBoundsFunction, Supplier<NativeWinWindowControl> parent) {
        this.windowBoundsFunction = windowBoundsFunction;
        this.parent = parent;
    }

    public synchronized void track(AuxEntry p) {
        entries.add(p);
        select(p);
    }

    public synchronized void select(AuxEntry p) {
        if (!entries.contains(p)) {
            return;
        }

        if (selected != null) {
            hide(selected);
        }
        selected = p;
        show(selected);
    }

    private synchronized void show(AuxEntry e) {
        var controllable = e.getProcess();
        if (!controllable.isActive()) {
            return;
        }

        controllable.updateBoundsState();
        if (controllable.isCustomBounds()) {
            return;
        }

        controllable.removeIcon();
        controllable.own(parent.get());
        controllable.removeStyle(true);
        controllable.focus();
        updatePositions();
    }

    public synchronized void hide(AuxEntry e) {
        var controllable = e.getProcess();
        controllable.disown();
        controllable.backOfWindow(parent.get());
        updatePositions();
    }

    public synchronized void closeTerminal(AuxEntry e) {
        if (!entries.contains(e)) {
            return;
        }

        var p = e.getProcess();
        // Reset style in case close is blocked by terminal
        p.restoreIcon();
        p.disown();
        p.restoreStyle(true);

        p.close();
        // If the process blocked the exit, still don't track it anymore
        entries.remove(e);
    }

    public synchronized void onWindowShow() {
    }

    public synchronized void onWindowMinimize() {
    }

    public synchronized void onClose() {
        TrackEvent.withTrace("Terminal view closed").handle();
        entries.forEach(terminalInstance -> {
            closeTerminal(terminalInstance);
        });
        entries.clear();
    }

    private void updatePositions() {
        if (viewBounds == null) {
            return;
        }

        entries.forEach(e -> {
            var p = e.getProcess();
            if (!p.isActive()) {
                return;
            }

            p.updateBoundsState();
            if (p.isCustomBounds()) {
                return;
            }

            p.updatePosition(windowBoundsFunction.apply(viewBounds));
        });
    }

    public synchronized void resizeView(int x, int y, int w, int h) {
        if (w < 100 || h < 100) {
            return;
        }

        this.viewBounds = new Rect(x, y, w, h);
        updatePositions();
    }
}
