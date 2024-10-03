package io.xpipe.app.util;

import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.NativeWinWindowControl;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.terminal.DockableTerminalType;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.core.process.OsType;
import javafx.application.Platform;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TerminalView {

    public static boolean isSupported() {
        return OsType.getLocal() == OsType.WINDOWS;
    }

    @Value
    public static class Session {
        ProcessHandle shell;
        ProcessHandle terminal;
    }

    @Getter
    public static abstract class TerminalInstance {

        private final ProcessHandle terminal;

        protected Rect lastBounds;
        protected boolean customBounds;

        protected TerminalInstance(ProcessHandle terminal) {this.terminal = terminal;}

        public abstract void show();

        public abstract void minimize();

        public abstract void front();

        public abstract void back();

        public abstract void updatePosition(Rect bounds);

        public abstract void close();

        public abstract boolean isActive();

        public abstract Rect queryBounds();

        public final void updateBoundsState() {
            if (!isActive()) {
                return;
            }

            var bounds = queryBounds();
            if (lastBounds != null && !lastBounds.equals(bounds)) {
                customBounds = true;
            }
            lastBounds = bounds;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public static final class WindowsTerminalInstance extends TerminalInstance {

        NativeWinWindowControl control;

        public WindowsTerminalInstance(ProcessHandle terminal, NativeWinWindowControl control) {
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
        public void front() {
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

    private final List<Session> sessions = new ArrayList<>();
    private final List<TerminalInstance> terminalInstances = new ArrayList<>();

    private Rect viewBounds;
    private boolean viewActive;

    public boolean isEnabled() {
        return isSupported() && AppPrefs.get().enableTerminalDocking().get();
    }

    public synchronized void open(long pid) {
        var processHandle = ProcessHandle.of(pid);
        if (processHandle.isEmpty() || !processHandle.get().isAlive()) {
            return;
        }

        var shell = processHandle.get().parent();
        if (shell.isEmpty()) {
            return;
        }

        var terminal = getTerminalProcess(shell.get());
        if (terminal.isEmpty()) {
            return;
        }

        var session = new Session(shell.get(), terminal.get());
        sessions.add(session);

        var instance = terminalInstances.stream().filter(i -> i.terminal.equals(terminal.get())).findFirst();
        if (instance.isEmpty()) {
            var control = NativeWinWindowControl.byPid(terminal.get().pid());
            if (control.isEmpty()) {
                return;
            }
            terminalInstances.add(new WindowsTerminalInstance(terminal.get(), control.get()));
        }

        TrackEvent.withTrace("Terminal instance opened")
                .tag("terminalPid", terminal.get().pid())
                .tag("viewEnabled", isEnabled())
                .handle();

        if (!isEnabled()) {
            return;
        }

        Platform.runLater(() -> {
            AppLayoutModel.get().selectTerminal();
        });
    }

    private Optional<ProcessHandle> getTerminalProcess(ProcessHandle shell) {
        var t = AppPrefs.get().terminalType().getValue();
        if (!(t instanceof DockableTerminalType dockableTerminalType)) {
            return Optional.empty();
        }

        var off = dockableTerminalType.getProcessHierarchyOffset();
        var current = Optional.of(shell);
        for (int i = 0; i < 1 + off; i++) {
            current = current.flatMap(processHandle -> processHandle.parent());
        }
        return current;
    }

    public synchronized void tick() {
        sessions.removeIf(session -> !session.shell.isAlive() || !session.terminal.isAlive());
        for (TerminalInstance terminalInstance : new ArrayList<>(terminalInstances)) {
            var alive = terminalInstance.terminal.isAlive();
            if (!alive) {
                terminalInstances.remove(terminalInstance);
                TrackEvent.withTrace("Terminal session is dead").tag("pid", terminalInstance.getTerminal().pid()).handle();
            }
        }
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
            terminalInstances.forEach(terminalInstance -> terminalInstance.front());
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
                terminalInstance.front();
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
            terminalInstance.front();
            terminalInstance.updatePosition(viewBounds);
        });
    }

    private static TerminalView INSTANCE;

    public static void init() {
        var instance = new TerminalView();
        ThreadHelper.createPlatformThread("terminal-view", true, () -> {
            while (true) {
                instance.tick();
                ThreadHelper.sleep(1000);
            }
        }).start();
        INSTANCE = instance;
    }

    public static TerminalView get() {
        return INSTANCE;
    }
}
