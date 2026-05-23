package io.xpipe.app.terminal;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.NativeWinWindowControl;
import io.xpipe.app.util.Rect;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

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

        if (NativeWinWindowControl.MAIN_WINDOW == null) {
            return false;
        }

        if (AppOperationMode.get() != AppOperationMode.GUI) {
            return false;
        }

        if (AppMainWindow.get() == null
                || !AppMainWindow.get().getStage().isShowing()
                || AppMainWindow.get().getStage().isIconified()) {
            return false;
        }

        return true;
    }

    private static TerminalDockHubManager INSTANCE;

    public static void init() {
        INSTANCE = new TerminalDockHubManager();
        if (!isAvailable()) {
            return;
        }

        INSTANCE.addLayoutListeners();
        INSTANCE.addDialogListeners();

        TerminalView.get().addListener(INSTANCE.createListener());

        GlobalTimer.scheduleUntil(Duration.ofMillis(500), false, () -> {
            INSTANCE.refreshDockStatus();
            return false;
        });
    }

    private static void showDialogIfNeeded() {
        var shown = AppCache.getBoolean("terminalDockDialog", false);
        if (shown) {
            return;
        }

        var modal = ModalOverlay.of("terminalDockDialogTitle", AppDialog.dialogTextKey("terminalDockDialogContent"));
        modal.addButton(
                new ModalButton("openSettings", () -> AppPrefs.get().selectCategory("connectionHub"), true, false));
        modal.addButton(new ModalButton("keepEnabled", null, true, true));
        modal.show();
        AppCache.update("terminalDockDialog", true);
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
        return adjust
                ? new Rect(rect.getX() - 8, rect.getY() - 1 - topAdjust, rect.getW() + 16, rect.getH() + 9 + topAdjust)
                : new Rect(rect.getX(), rect.getY() - topAdjust, rect.getW(), rect.getH() + topAdjust);
    });
    private final AppLayoutModel.QueueEntry queueEntry = new AppLayoutModel.QueueEntry(
            AppI18n.observable(
                    "toggleTerminalDock",
                    new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN).getDisplayText()),
            new LabelGraphic.NodeGraphic(() -> {
                var inner = new FontIcon();
                inner.iconCodeProperty()
                        .bind(PlatformThread.sync(Bindings.createObjectBinding(
                                () -> {
                                    return detached.get() || minimized.get() || !showing.get()
                                            ? MaterialDesignC.CONSOLE_LINE
                                            : MaterialDesignC.CONSOLE;
                                },
                                detached,
                                minimized,
                                showing)));
                inner.getStyleClass().add("graphic");
                inner.getStyleClass().add("terminal-dock-button");
                return inner;
            }),
            () -> {
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
            },
            true);

    private void addDialogListeners() {
        var wasShowing = new SimpleBooleanProperty();
        var wasAttached = new SimpleBooleanProperty();
        AppDialog.getModalOverlays().addListener((ListChangeListener<? super ModalOverlay>) c -> {
            if (c.getList().isEmpty()) {
                if (wasShowing.get()) {
                    showDock();
                }
                if (wasAttached.get()) {
                    attach();
                }
            } else {
                wasAttached.set(!minimized.get() && !detached.get() && showing.get());
                wasShowing.set(showing.get());
                hideDock();
            }
        });
    }

    private void addLayoutListeners() {
        var wasShowing = new SimpleBooleanProperty();
        var wasAttached = new SimpleBooleanProperty();
        AppLayoutModel.get().getSelected().addListener((observable, oldValue, newValue) -> {
            if (AppLayoutModel.get().getEntries().indexOf(newValue) == 0) {
                if (wasShowing.get()) {
                    showDock();
                }
                if (wasAttached.get()) {
                    attach();
                }
            } else if (AppLayoutModel.get().getEntries().indexOf(oldValue) == 0) {
                wasAttached.set(!minimized.get() && !detached.get() && showing.get());
                wasShowing.set(showing.get());
                hideDock();
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

                if (!(session.getTerminal() instanceof TerminalView.ControllableTerminalSession t)) {
                    return;
                }

                var term = t.getTerminalType();
                if (term instanceof TrackableTerminalType trackableType) {
                    if (trackableType.getDockMode() == TerminalDockMode.UNSUPPORTED) {
                        return;
                    }
                }

                var dock = !detached.get();
                enableDock();
                showDock();
                Platform.runLater(() -> {
                    dockModel.trackTerminal(t, dock);
                    dockModel.closeOtherTerminals(session.getRequest());
                });
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
                dockModel.removeTerminal(instance);
                refreshDockStatus();
            }
        };
        return listener;
    }

    public void refreshDockStatus() {
        dockModel.updateCustomBounds();

        var running = dockModel.isRunning();
        if (!running) {
            minimized.set(false);
            detached.set(false);
            disableDock();
            return;
        }

        minimized.set(dockModel.isMinimized());
        detached.set(!dockModel.isMinimized()
                && (dockModel.isCustomBounds() || AppMainWindow.get().getStage().isIconified()));
    }

    public void openTerminal(UUID request) {
        if (!isSupported()) {
            return;
        }

        if (!shouldOpen()) {
            return;
        }

        hubRequests.add(request);
    }

    private boolean shouldOpen() {
        // Check if we are in the hub interface
        if (!AppLayoutModel.get()
                .getEntries()
                .getFirst()
                .equals(AppLayoutModel.get().getSelected().getValue())) {
            return false;
        }

        return true;
    }

    public void enableDock() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (enabled.get()) {
                return;
            }

            dockModel.activateView();
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

            dockModel.deactivateView();
            enabled.set(false);
            showing.set(false);

            showDialogIfNeeded();

            NativeWinWindowControl.MAIN_WINDOW.setWindowsTransitionsEnabled(true);
            AppLayoutModel.get().getQueueEntries().remove(queueEntry);
        });
    }

    public void triggerDock() {
        if (!enabled.get()) {
            return;
        }

        if (showing.get() && (minimized.get() || detached.get())) {
            attach();
        }
    }

    public void showDock() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (showing.get()) {
                return;
            }

            dockModel.activateView();
            showing.set(true);
            AppLayoutModel.get().selectConnections();
        });
    }

    public void hideDock() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (!showing.get()) {
                return;
            }

            dockModel.deactivateView();
            showing.set(false);
        });
    }

    public void attach() {
        dockModel.attach();
        detached.set(false);
        minimized.set(false);
    }
}
