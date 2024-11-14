package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserAbstractSessionModel;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserSessionTab;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.terminal.TerminalDockComp;
import io.xpipe.app.terminal.TerminalDockModel;
import io.xpipe.app.terminal.TerminalView;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ObservableList;

import java.util.Optional;
import java.util.UUID;

public final class BrowserTerminalDockTabModel extends BrowserSessionTab {

    private final BrowserSessionTab origin;
    private final ObservableList<UUID> terminalRequests;
    private final TerminalDockModel dockModel = new TerminalDockModel();
    private TerminalView.Listener listener;
    private ObservableBooleanValue viewActive;

    public BrowserTerminalDockTabModel(
            BrowserAbstractSessionModel<?> browserModel,
            BrowserSessionTab origin,
            ObservableList<UUID> terminalRequests) {
        super(browserModel, AppI18n.get("terminal"));
        this.origin = origin;
        this.terminalRequests = terminalRequests;
    }

    @Override
    public Comp<?> comp() {
        return new TerminalDockComp(dockModel);
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

                var toTrack = tv.getLast();
                dockModel.trackTerminal(toTrack);
            }

            @Override
            public void onTerminalClosed(TerminalView.TerminalSession instance) {
                var sessions = TerminalView.get().getSessions();
                var remaining = sessions.stream()
                        .filter(s -> terminalRequests.contains(s.getRequest())
                                && s.getTerminal().isRunning())
                        .toList();
                if (remaining.isEmpty()) {
                    ((BrowserFullSessionModel) browserModel).unsplitTab(BrowserTerminalDockTabModel.this);
                }
            }
        };
        TerminalView.get().addListener(listener);

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
    }

    @Override
    public void close() {
        if (listener != null) {
            TerminalView.get().removeListener(listener);
        }
        dockModel.onClose();
    }

    @Override
    public String getIcon() {
        return null;
    }

    @Override
    public DataColor getColor() {
        return null;
    }
}
