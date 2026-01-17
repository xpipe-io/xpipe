package io.xpipe.app.terminal;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.core.OsType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class TerminalDockHubManager {

    public static boolean isSupported() {
        if (OsType.ofLocal() != OsType.WINDOWS) {
            return false;
        }

        var term = AppPrefs.get().terminalType().getValue();
        if (term == null) {
            return false;
        }

        var tabsSupported = term.getOpenFormat() != TerminalOpenFormat.NEW_WINDOW || TerminalMultiplexerManager.getEffectiveMultiplexer().isPresent();
        if (!tabsSupported) {
            return false;
        }

        var modeSupported = term instanceof TrackableTerminalType t && t.getDockMode() != TerminalDockMode.UNSUPPORTED;
        if (!modeSupported) {
            return false;
        }

        return true;
    }

    private static TerminalDockHubManager INSTANCE;

    public static void init() {
        INSTANCE = new TerminalDockHubManager();

        AppLayoutModel.get().getSelected().addListener((observable, oldValue, newValue) -> {
            if (AppLayoutModel.get().getEntries().indexOf(newValue) == 0) {
                INSTANCE.selectHub();
            } else {
                INSTANCE.unselectHub();
            }
        });

        TerminalView.get().addListener(INSTANCE.createListener());

        GlobalTimer.scheduleUntil(Duration.ofSeconds(1), false, () -> {
            INSTANCE.refreshDockStatus();
            return false;
        });
    }

    public static TerminalDockHubManager get() {
        return INSTANCE;
    }

    private final Set<UUID> hubRequests = new HashSet<>();
    private final BooleanProperty showing = new SimpleBooleanProperty();
    private final BooleanProperty detached = new SimpleBooleanProperty();
    private final BooleanProperty minimized = new SimpleBooleanProperty();
    private final TerminalDockView dockModel = new TerminalDockView();
    private final AppLayoutModel.QueueEntry queueEntry = new AppLayoutModel.QueueEntry(
            AppI18n.observable("toggleTerminalDock"),
            new LabelGraphic.IconGraphic("mdi2c-console"), () -> {
                refreshDockStatus();

                if (minimized.get() || detached.get()) {
                    attach();
                    return false;
                }

                if (showing.get()) {
                    hideDock();
                    return false;
                }

                if (!showing.get()) {
                    showDock();
                    return false;
                }

                return false;
    });

    private TerminalView.Listener createListener() {
        var listener = new TerminalView.Listener() {
            @Override
            public void onSessionOpened(TerminalView.ShellSession session) {
                if (!hubRequests.contains(session.getRequest())) {
                    return;
                }

                var controllable = session.getTerminal().controllable();
                if (controllable.isEmpty()) {
                    return;
                }

                var term = AppPrefs.get().terminalType().getValue();
                if (term instanceof TrackableTerminalType t) {
                    if (t.getDockMode() == TerminalDockMode.UNSUPPORTED) {
                        return;
                    }

                    controllable.get().removeShadow();
                    if (t.getDockMode() == TerminalDockMode.BORDERLESS) {
                        controllable.get().removeBorders();
                    }
                }
                dockModel.trackTerminal(controllable.get(), !detached.get());
                dockModel.closeOtherTerminals(session.getRequest());
                openDock();
            }

            @Override
            public void onSessionClosed(TerminalView.ShellSession session) {
                if (!hubRequests.contains(session.getRequest())) {
                    return;
                }

                hubRequests.remove(session.getRequest());
            }

            @Override
            public void onTerminalClosed(TerminalView.TerminalSession instance) {
                var sessions = TerminalView.get().getSessions();
                var remaining = sessions.stream()
                        .filter(s -> hubRequests.contains(s.getRequest())
                                && s.getTerminal().isRunning())
                        .toList();
                if (remaining.isEmpty()) {
                    closeDock();
                }
            }
        };
        return listener;
    }

    public void refreshDockStatus() {
        var running = dockModel.isRunning();
        if (!running) {
            minimized.set(false);
            detached.set(false);
            return;
        }

        minimized.set(dockModel.isMinimized());
        detached.set(dockModel.isCustomBounds());
    }

    public void selectHub() {
        dockModel.onFocusLost();
    }

    public void unselectHub() {
    }

    public void openTerminal(UUID request) {
        if (!isSupported()) {
            return;
        }

        hubRequests.add(request);
    }

    public void openDock() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (showing.get()) {
                return;
            }

            dockModel.toggleView(true);
            showing.set(true);

            AppLayoutModel.get().getQueueEntries().add(queueEntry);
        });
    }

    public void showDock() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (showing.get()) {
                return;
            }

            dockModel.toggleView(true);
            showing.set(true);
        });
    }

    public void attach() {
        dockModel.attach();
        detached.set(false);
    }

    public void hideDock() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (!showing.get()) {
                return;
            }

            dockModel.toggleView(false);
            showing.set(false);
        });
    }

    public void closeDock() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (!showing.get()) {
                return;
            }

            dockModel.toggleView(false);
            showing.set(false);
            AppLayoutModel.get().getQueueEntries().remove(queueEntry);
        });
    }
}
