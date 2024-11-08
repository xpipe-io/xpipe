package io.xpipe.app.terminal;

import io.xpipe.app.core.window.NativeWinWindowControl;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.OsType;

import lombok.Getter;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TerminalView {

    public static boolean isSupported() {
        return OsType.getLocal() == OsType.WINDOWS;
    }

    @Value
    public static class Session {
        UUID request;
        ProcessHandle shell;
        ProcessHandle terminal;
    }

    public static interface Listener {

        void onSessionOpened(Session session);

        void onSessionClosed(Session session);

        void onTerminalOpened(TerminalViewInstance instance);

        void onTerminalClosed(TerminalViewInstance instance);
    }

    private final List<Session> sessions = new ArrayList<>();
    private final List<TerminalViewInstance> terminalInstances = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();

    public synchronized List<Session> getSessions() {
        return new ArrayList<>(sessions);
    }

    public synchronized List<TerminalViewInstance> getTerminalInstances() {
        return new ArrayList<>(terminalInstances);
    }

    public synchronized void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public synchronized void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public synchronized void open(UUID request, long pid) {
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

        var session = new Session(request, shell.get(), terminal.get());
        var instance = terminalInstances.stream()
                .filter(i -> i.getTerminalProcess().equals(terminal.get()))
                .findFirst();
        if (instance.isEmpty()) {
            var control = NativeWinWindowControl.byPid(terminal.get().pid());
            if (control.isEmpty()) {
                return;
            }
            var tv = new WindowsTerminalViewInstance(terminal.get(), control.get());
            terminalInstances.add(tv);
            listeners.forEach(listener -> listener.onTerminalOpened(tv));
        }

        sessions.add(session);
        listeners.forEach(listener -> listener.onSessionOpened(session));

        TrackEvent.withTrace("Terminal instance opened")
                .tag("terminalPid", terminal.get().pid())
                .handle();
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

    public synchronized Optional<Session> findSession(long pid) {
        var proc = ProcessHandle.of(pid);
        while (true) {
            if (proc.isEmpty()) {
                return Optional.empty();
            }

            var finalProc = proc;
            var found = TerminalView.get().getSessions().stream().filter(session -> session.getShell().equals(finalProc.get())).findFirst();
            if (found.isPresent()) {
                return found;
            }

            proc = proc.get().parent();
        }
    }

    public synchronized void tick() {
        for (Session session : new ArrayList<>(sessions)) {
            var alive = session.shell.isAlive() && session.terminal.isAlive();
            if (!alive) {
                sessions.remove(session);
                listeners.forEach(listener -> listener.onSessionClosed(session));
            }
        }

        for (TerminalViewInstance terminalInstance : new ArrayList<>(terminalInstances)) {
            var alive = terminalInstance.getTerminalProcess().isAlive();
            if (!alive) {
                terminalInstances.remove(terminalInstance);
                TrackEvent.withTrace("Terminal session is dead")
                        .tag("pid", terminalInstance.getTerminalProcess().pid())
                        .handle();
                listeners.forEach(listener -> listener.onTerminalClosed(terminalInstance));
            }
        }
    }

    private static TerminalView INSTANCE;

    public static void init() {
        var instance = new TerminalView();
        ThreadHelper.createPlatformThread("terminal-view", true, () -> {
                    while (true) {
                        instance.tick();
                        ThreadHelper.sleep(500);
                    }
                })
                .start();
        INSTANCE = instance;
    }

    public static TerminalView get() {
        return INSTANCE;
    }
}
