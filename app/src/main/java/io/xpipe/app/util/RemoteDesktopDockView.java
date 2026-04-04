package io.xpipe.app.util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class RemoteDesktopDockView implements WindowDockListener {

    @Getter
    private final List<RemoteDesktopDockEntry> entries = new ArrayList<>();
    @Getter
    private RemoteDesktopDockEntry selected;

    private final UnaryOperator<Rect> windowBoundsFunction;
    private final Supplier<NativeWinWindowControl> parent;

    @Getter
    private Rect viewBounds;

    public RemoteDesktopDockView(UnaryOperator<Rect> windowBoundsFunction, Supplier<NativeWinWindowControl> parent) {
        this.windowBoundsFunction = windowBoundsFunction;
        this.parent = parent;
    }

    public synchronized void clearDead() {
        for (RemoteDesktopDockEntry entry : new ArrayList<>(entries)) {
            if (!entry.getProcess().isRunning()) {
                closeWindow(entry);
            }
        }
    }

    public synchronized void track(RemoteDesktopDockEntry p) {
        entries.add(p);
        select(p);
    }

    public synchronized void focus() {
        if (selected != null) {
            selected.getProcess().focus();
        }
    }

    public synchronized void select(RemoteDesktopDockEntry p) {
        if (!entries.contains(p)) {
            return;
        }

        if (p != null && p.equals(selected)) {
            return;
        }

        if (selected != null) {
            hide(selected);
        }
        selected = p;
        if (p != null) {
            show(p);
        }
    }

    private synchronized void show(RemoteDesktopDockEntry e) {
        var controllable = e.getProcess();

        parent.get().moveToFront();

        controllable.show();
        controllable.moveToFront();
        controllable.removeIcon();
        controllable.own(parent.get());
        controllable.removeStyle(true);
        controllable.focus();
        updatePositions();
    }

    private synchronized void hide(RemoteDesktopDockEntry e) {
        var controllable = e.getProcess();
        controllable.disown();
        controllable.backOfWindow(parent.get());
        updatePositions();
    }

    public synchronized void closeWindow(RemoteDesktopDockEntry e) {
        if (!entries.contains(e)) {
            return;
        }

        var p = e.getProcess();
        if (p.isRunning()) {
            p.close();
        }

        if (e.equals(selected)) {
            var index = entries.indexOf(e);
            var fallback = index == 0 ? (entries.size() > 1 ? entries.get(1) : null) : entries.get(index - 1);
            select(fallback);
        }
        entries.remove(e);
    }

    public synchronized void onWindowShow() {
        if (selected != null) {
            show(selected);
        }
    }

    public synchronized void onWindowMinimize() {
        entries.forEach(e -> {
            var controllable = e.getProcess();
            controllable.disown();
            controllable.hide();
        });
    }

    public synchronized void onClose() {
        for (RemoteDesktopDockEntry entry : new ArrayList<>(entries)) {
            closeWindow(entry);
        }
    }

    private void updatePositions() {
        if (viewBounds == null) {
            return;
        }

        entries.forEach(e -> {
            var p = e.getProcess();
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
