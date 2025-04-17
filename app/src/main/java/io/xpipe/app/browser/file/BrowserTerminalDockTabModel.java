package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserAbstractSessionModel;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserSessionTab;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.terminal.TerminalDockComp;
import io.xpipe.app.terminal.TerminalDockModel;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.app.terminal.WindowsTerminalType;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BrowserTerminalDockTabModel extends BrowserSessionTab {

    private final BrowserSessionTab origin;
    private final ObservableList<UUID> terminalRequests;
    private final TerminalDockModel dockModel = new TerminalDockModel();
    private final BooleanProperty opened = new SimpleBooleanProperty();
    private TerminalView.Listener listener;
    private ObservableBooleanValue viewActive;

    public BrowserTerminalDockTabModel(
            BrowserAbstractSessionModel<?> browserModel,
            BrowserSessionTab origin,
            ObservableList<UUID> terminalRequests) {
        super(browserModel);
        this.origin = origin;
        this.terminalRequests = terminalRequests;
    }

    @Override
    public Comp<?> comp() {
        return new TerminalDockComp(dockModel, opened);
    }

    @Override
    public boolean canImmediatelyClose() {
        return true;
    }

    @Override
    public void init() throws Exception {
        listener = new TerminalView.Listener() {
            @Override
            public void onSessionOpened(TerminalView.ShellSession session) {
                if (!terminalRequests.contains(session.getRequest())) {
                    return;
                }

                opened.set(true);
                var sessions = TerminalView.get().getSessions();
                var tv = sessions.stream()
                        .filter(s -> terminalRequests.contains(s.getRequest())
                                && s.getTerminal().isRunning())
                        .map(s -> s.getTerminal().controllable())
                        .flatMap(Optional::stream)
                        .toList();
                for (int i = 0; i < tv.size() - 1; i++) {
                    dockModel.closeTerminal(tv.get(i));
                }

                // Closing and opening windows at the same time might be problematic for some bad implementations
                if (tv.size() > 1) {
                    ThreadHelper.sleep(250);
                }

                var toTrack = tv.getLast();
                dockModel.trackTerminal(toTrack);
            }

            @Override
            public void onSessionClosed(TerminalView.ShellSession session) {
                if (!terminalRequests.contains(session.getRequest())) {
                    return;
                }

                // Ugly fix for Windows Terminal instances not closing properly if multiple windows exist
                if (AppPrefs.get().terminalType().getValue() instanceof WindowsTerminalType) {
                    var sessions = TerminalView.get().getSessions();
                    var others = sessions.stream()
                            .filter(shellSession -> shellSession.getTerminal().equals(session.getTerminal()))
                            .count();
                    if (others == 0) {
                        session.getTerminal().controllable().ifPresent(controllableTerminalSession -> {
                            controllableTerminalSession.close();
                        });
                    }
                }
            }

            @Override
            public void onTerminalClosed(TerminalView.TerminalSession instance) {
                refreshShowingState();
            }
        };
        TerminalView.get().addListener(listener);

        // If the terminal launch fails
        ThreadHelper.runAsync(() -> {
            ThreadHelper.sleep(5000);
            if (!opened.get()) {
                refreshShowingState();
            }
        });

        viewActive = Bindings.createBooleanBinding(
                () -> {
                    return this.browserModel.getSelectedEntry().getValue() == origin
                            && AppLayoutModel.get()
                                            .getEntries()
                                            .indexOf(AppLayoutModel.get()
                                                    .getSelected()
                                                    .getValue())
                                    == 1;
                },
                this.browserModel.getSelectedEntry(),
                AppLayoutModel.get().getSelected());
        viewActive.subscribe(aBoolean -> {
            Platform.runLater(() -> {
                dockModel.toggleView(aBoolean);
            });
        });
        AppDialog.getModalOverlays().addListener((ListChangeListener<? super ModalOverlay>) c -> {
            if (c.getList().size() > 0) {
                dockModel.toggleView(false);
            } else {
                dockModel.toggleView(viewActive.get());
            }
        });
    }

    private void refreshShowingState() {
        var sessions = TerminalView.get().getSessions();
        var remaining = sessions.stream()
                .filter(s -> terminalRequests.contains(s.getRequest())
                        && s.getTerminal().isRunning())
                .toList();
        if (remaining.isEmpty()) {
            ((BrowserFullSessionModel) browserModel).unsplitTab(BrowserTerminalDockTabModel.this);
        }
    }

    @Override
    public void close() {
        if (listener != null) {
            TerminalView.get().removeListener(listener);
        }
        dockModel.onClose();
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("terminal");
    }

    @Override
    public String getIcon() {
        return null;
    }

    @Override
    public DataStoreColor getColor() {
        return null;
    }
}
