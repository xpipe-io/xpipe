package io.xpipe.app.util;

import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.NativeWinWindowControl;
import io.xpipe.app.issue.TrackEvent;
import javafx.application.Platform;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

public class TerminalView {

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

        public abstract Rect queryBounds();

        public final void updateBoundsState() {
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
            front();
        }

        @Override
        public void minimize() {
            this.control.minimize();
        }

        @Override
        public void front() {
            this.control.alwaysInFront();
        }

        @Override
        public void back() {
            NativeWinWindowControl.MAIN_WINDOW.orderRelative(control.getWindowHandle());
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
        public Rect queryBounds() {
            return control.getBounds();
        }
    }

    private final List<Session> sessions = new ArrayList<>();
    private final List<TerminalInstance> terminalInstances = new ArrayList<>();

    private Rect viewBounds;
    private boolean viewActive;

    public synchronized void open(long pid) {
        var processHandle = ProcessHandle.of(pid);
        if (processHandle.isEmpty() || !processHandle.get().isAlive()) {
            return;
        }

        var shell = processHandle.get().parent();
        if (shell.isEmpty()) {
            return;
        }

        var terminal = shell.get().parent();
        if (terminal.isEmpty()) {
            return;
        }

        var session = new Session(shell.get(), terminal.get());
        sessions.add(session);

        var instance = terminalInstances.stream().filter(i -> i.terminal.equals(terminal.get())).findFirst();
        if (instance.isPresent()) {
            return;
        }

        var control = NativeWinWindowControl.byPid(terminal.get().pid());
        if (control.isEmpty()) {
            return;
        }
        terminalInstances.add(new WindowsTerminalInstance(terminal.get(), control.get()));
        Platform.runLater(() -> {
            AppLayoutModel.get().selectTerminal();
        });
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
        if (viewActive == active) {
            return;
        }

        this.viewActive = active;
        if (active) {
            terminalInstances.forEach(terminalInstance -> terminalInstance.front());
            updatePositions();
        }
    }

    public synchronized void onFocusLost() {
        terminalInstances.forEach(terminalInstance -> terminalInstance.back());
    }

    public synchronized void onFocusGain() {
        terminalInstances.forEach(terminalInstance -> terminalInstance.show());
    }

    public synchronized void onMinimize() {
        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }


            terminalInstance.minimize();
        });
    }

    public synchronized void onClose() {
        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.close();
        });
    }

    private void updatePositions() {
        terminalInstances.forEach(terminalInstance -> {
            terminalInstance.updateBoundsState();
            if (terminalInstance.isCustomBounds()) {
                return;
            }

            terminalInstance.updatePosition(viewBounds);
        });
    }

    public void resizeView(int x, int y, int w, int h) {
        this.viewBounds = new Rect(x,y,w,h);
        if (viewActive) {
            updatePositions();
        }
    }

    public void clickView() {
        terminalInstances.forEach(terminalInstance -> terminalInstance.updatePosition(viewBounds));
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
