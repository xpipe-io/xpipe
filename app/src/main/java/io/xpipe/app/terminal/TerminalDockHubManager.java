package io.xpipe.app.terminal;

import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.NativeWinWindowControl;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.Rect;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;

import lombok.Getter;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class TerminalDockHubManager {

    public static boolean isAvailable() {
        if (OsType.ofLocal() != OsType.WINDOWS) {
            return false;
        }

        return true;
    }

    public static boolean isSupported() {
        if (OsType.ofLocal() != OsType.WINDOWS) {
            return false;
        }

        var term = AppPrefs.get().terminalType().getValue();
        if (term == null) {
            return false;
        }

        var tabsSupported = term.getOpenFormat() != TerminalOpenFormat.NEW_WINDOW
                || TerminalMultiplexerManager.getEffectiveMultiplexer().isPresent();
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
        if (!isAvailable()) {
            return;
        }

        INSTANCE = new TerminalDockHubManager();

        INSTANCE.addLayoutListeners();
        INSTANCE.addDialogListeners();

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
    private final BooleanProperty enabled = new SimpleBooleanProperty();
    private final BooleanProperty showing = new SimpleBooleanProperty();
    private final BooleanProperty detached = new SimpleBooleanProperty();
    private final BooleanProperty minimized = new SimpleBooleanProperty();
    private final TerminalDockView dockModel = new TerminalDockView(rect -> {
        var term = AppPrefs.get().terminalType().getValue();
        var adjust = term instanceof TrackableTerminalType t && t.getDockMode() != TerminalDockMode.BORDERLESS;
        // Windows terminal has a tiny top bar in any scenario
        var topAdjust = term instanceof WindowsTerminalType ? 1 : 0;
        return adjust ? new Rect(rect.getX() - 9, rect.getY() - 1 - topAdjust, rect.getW() + 16, rect.getH() + 9 + topAdjust) :
                new Rect(rect.getX(), rect.getY() - topAdjust, rect.getW(), rect.getH() + topAdjust);
    });
    private final AppLayoutModel.QueueEntry queueEntry = new AppLayoutModel.QueueEntry(
            AppI18n.observable("toggleTerminalDock"), new LabelGraphic.IconGraphic("mdi2c-console"), () -> {
                refreshDockStatus();

                if (!enabled.get()) {
                    return false;
                }

                if (!showing.get()) {
                    // Run later to guarantee order of operations
                    Platform.runLater(() -> {
                        AppLayoutModel.get().selectConnections();
                        showDock();
                        attach();
                    });
                    return false;
                }

                if (minimized.get() || detached.get()) {
                    attach();
                    return false;
                }

                if (showing.get()) {
                    hideDock();
                    return false;
                }

                return false;
            });

    private void addDialogListeners() {
        AppDialog.getModalOverlays().addListener((ListChangeListener<? super ModalOverlay>) c -> {
            if (c.getList().size() > 0) {
                INSTANCE.hideDock();
            }
        });
    }

    private void addLayoutListeners() {
        var wasShowing = new SimpleBooleanProperty();
        var wasAttached = new SimpleBooleanProperty();
        AppLayoutModel.get().getSelected().addListener((observable, oldValue, newValue) -> {
            if (AppLayoutModel.get().getEntries().indexOf(newValue) == 0) {
                if (wasShowing.get()) {
                    INSTANCE.showDock();
                }
                if (wasAttached.get()) {
                    INSTANCE.attach();
                }
            } else if (AppLayoutModel.get().getEntries().indexOf(oldValue) == 0) {
                wasAttached.set(!INSTANCE.minimized.get() && !INSTANCE.detached.get() && INSTANCE.showing.get());
                wasShowing.set(INSTANCE.showing.get());
                INSTANCE.hideDock();
            }
        });
    }

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

                    if (t.getDockMode() == TerminalDockMode.BORDERLESS) {
                        controllable.get().removeBorders();
                    }
                }
                dockModel.trackTerminal(controllable.get(), !detached.get());
                dockModel.closeOtherTerminals(session.getRequest());
                enableDock();
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
                    disableDock();
                }
            }
        };
        return listener;
    }

    public void refreshDockStatus() {
        dockModel.clearDeadTerminals();
        dockModel.updateCustomBounds();

        var running = dockModel.isRunning();
        if (!running) {
            minimized.set(false);
            detached.set(false);
            return;
        }

        minimized.set(dockModel.isMinimized());
        detached.set(dockModel.isCustomBounds() || dockModel.isMinimized());
    }

    public void registerTerminal(UUID request) {
        if (!isSupported()) {
            return;
        }

        hubRequests.add(request);
    }

    public void openTerminal(UUID request) {
        if (!isSupported()) {
            return;
        }

        if (!hubRequests.contains(request)) {
            return;
        }

        if (!enabled.get()) {
            enableDock();
        } else if (!showing.get()) {
            showDock();
        }
    }

    public void enableDock() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (enabled.get()) {
                return;
            }

            dockModel.toggleView(true);
            enabled.set(true);
            showing.set(true);

            NativeWinWindowControl.MAIN_WINDOW.setWindowsTransitionsEnabled(false);
            AppLayoutModel.get().getQueueEntries().add(queueEntry);
        });
    }

    public void disableDock() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (!enabled.get()) {
                return;
            }

            dockModel.toggleView(false);
            enabled.set(false);
            showing.set(false);

            NativeWinWindowControl.MAIN_WINDOW.setWindowsTransitionsEnabled(true);
            AppLayoutModel.get().getQueueEntries().remove(queueEntry);
        });
    }

    public void showDock() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (showing.get()) {
                return;
            }

            dockModel.toggleView(true);
            showing.set(true);
            AppLayoutModel.get().selectConnections();
        });
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

    public void attach() {
        dockModel.attach();
        detached.set(false);
    }
}
