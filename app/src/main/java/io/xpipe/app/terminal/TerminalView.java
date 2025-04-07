package io.xpipe.app.terminal;

import io.xpipe.app.core.window.NativeWinWindowControl;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialects;

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
    public static class ShellSession {
        UUID request;
        ProcessHandle shell;
        TerminalSession terminal;
    }

    @Getter
    public static class TerminalSession {

        protected final ProcessHandle terminalProcess;

        protected TerminalSession(ProcessHandle terminalProcess) {
            this.terminalProcess = terminalProcess;
        }

        public boolean isRunning() {
            return terminalProcess.isAlive();
        }

        public Optional<ControllableTerminalSession> controllable() {
            return Optional.ofNullable(this instanceof ControllableTerminalSession c ? c : null);
        }
    }

    public interface Listener {

        default void onSessionOpened(ShellSession session) {}

        default void onSessionClosed(ShellSession session) {}

        default void onTerminalOpened(TerminalSession instance) {}

        default void onTerminalClosed(TerminalSession instance) {}
    }

    private final List<ShellSession> sessions = new ArrayList<>();
    private final List<TerminalSession> terminalInstances = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();

    public synchronized List<ShellSession> getSessions() {
        return new ArrayList<>(sessions);
    }

    public synchronized List<TerminalSession> getTerminalInstances() {
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
        TrackEvent.withTrace("Shell session opened")
                .tag("pid", shell.map(p -> p.pid()).orElse(-1L))
                .handle();
        if (shell.isEmpty()) {
            return;
        }

        var terminal = getTerminalProcess(shell.get());
        TrackEvent.withTrace("Terminal session opened")
                .tag("pid", terminal.map(p -> p.pid()).orElse(-1L))
                .tag("exec", terminal.flatMap(p -> p.info().command()).orElse("?"))
                .handle();
        if (terminal.isEmpty()) {
            return;
        }

        var tv = createTerminalSession(terminal.get());
        if (tv.isEmpty()) {
            return;
        }

        if (!terminalInstances.contains(tv.get())) {
            terminalInstances.add(tv.get());
            listeners.forEach(listener -> listener.onTerminalOpened(tv.get()));
        }

        var session = new ShellSession(request, shell.get(), tv.get());
        sessions.add(session);
        listeners.forEach(listener -> listener.onSessionOpened(session));

        TrackEvent.withTrace("Terminal instance opened")
                .tag("terminalPid", terminal.get().pid())
                .handle();
    }

    private Optional<TerminalSession> createTerminalSession(ProcessHandle terminalProcess) {
        return switch (OsType.getLocal()) {
            case OsType.Linux linux -> Optional.of(new TerminalSession(terminalProcess));
            case OsType.MacOs macOs -> Optional.of(new TerminalSession(terminalProcess));
            case OsType.Windows windows -> {
                var controls = NativeWinWindowControl.byPid(terminalProcess.pid());
                if (controls.isEmpty()) {
                    yield Optional.empty();
                }

                var existing = terminalInstances.stream()
                        .map(terminalSession -> ((WindowsTerminalSession) terminalSession).getControl())
                        .toList();
                controls.removeAll(existing);
                if (controls.isEmpty()) {
                    yield Optional.empty();
                }

                yield Optional.of(new WindowsTerminalSession(terminalProcess, controls.getFirst()));
            }
        };
    }

    private Optional<ProcessHandle> getTerminalProcess(ProcessHandle shell) {
        var t = AppPrefs.get().terminalType().getValue();
        if (!(t instanceof TrackableTerminalType trackableTerminalType)) {
            return Optional.empty();
        }

        var off = trackableTerminalType.getProcessHierarchyOffset();
        var current = Optional.of(shell);
        for (int i = 0; i < 1 + off; i++) {
            current = current.flatMap(processHandle -> processHandle.parent());
        }
        return current;
    }

    public synchronized Optional<ShellSession> findSession(long pid) {
        var proc = ProcessHandle.of(pid);
        while (true) {
            if (proc.isEmpty()) {
                return Optional.empty();
            }

            var finalProc = proc;
            var found = TerminalView.get().getSessions().stream()
                    .filter(session -> session.getShell().equals(finalProc.get()))
                    .findFirst();
            if (found.isPresent()) {
                return found;
            }

            proc = proc.get().parent();
        }
    }

    public synchronized void tick() {
        for (ShellSession session : new ArrayList<>(sessions)) {
            var alive = session.shell.isAlive() && session.getTerminal().isRunning();
            if (!alive) {
                sessions.remove(session);
                listeners.forEach(listener -> listener.onSessionClosed(session));
            }
        }

        for (TerminalSession terminalInstance : new ArrayList<>(terminalInstances)) {
            var alive = terminalInstance.isRunning();
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
