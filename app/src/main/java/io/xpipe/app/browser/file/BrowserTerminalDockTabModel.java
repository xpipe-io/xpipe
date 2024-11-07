package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserAbstractSessionModel;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserSessionTab;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.terminal.TerminalDockComp;
import io.xpipe.app.terminal.TerminalDockModel;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.app.terminal.TerminalViewInstance;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.UUID;

public final class BrowserTerminalDockTabModel extends BrowserSessionTab {

    private final BrowserSessionTab origin;
    private final ObservableList<UUID> terminalRequests;
    private final TerminalDockModel dockModel = new TerminalDockModel();
    private TerminalView.Listener listener;
    private ObservableBooleanValue viewActive;

    public BrowserTerminalDockTabModel(BrowserAbstractSessionModel<?> browserModel, BrowserSessionTab origin, ObservableList<UUID> terminalRequests) {
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
        var sessions = new ArrayList<TerminalView.Session>();
        var terminals = new ArrayList<TerminalViewInstance>();
        listener = new TerminalView.Listener() {
            @Override
            public void onSessionOpened(TerminalView.Session session) {
                if (!terminalRequests.contains(session.getRequest())) {
                    return;
                }

                sessions.add(session);
                var tv = terminals.stream().filter(instance -> sessions.stream().anyMatch(s -> instance.getTerminalProcess().equals(s.getTerminal()))).toList();
                if (tv.isEmpty()) {
                    return;
                }

                for (int i = 0; i < tv.size() - 1; i++) {
                    dockModel.closeTerminal(tv.get(i));
                }

                var toTrack = tv.getLast();
                dockModel.trackTerminal(toTrack);
            }

            @Override
            public void onSessionClosed(TerminalView.Session session) {
                sessions.remove(session);
            }

            @Override
            public void onTerminalOpened(TerminalViewInstance instance) {
                terminals.add(instance);
            }

            @Override
            public void onTerminalClosed(TerminalViewInstance instance) {
                terminals.remove(instance);
                if (terminals.isEmpty()) {
                    ((BrowserFullSessionModel) browserModel).unsplitTab(BrowserTerminalDockTabModel.this);
                }
            }
        };
        TerminalView.get().addListener(listener);

        viewActive = Bindings.createBooleanBinding(() -> {
            return this.browserModel.getSelectedEntry().getValue() == origin && AppLayoutModel.get().getEntries().indexOf(AppLayoutModel.get().getSelected().getValue()) == 1;
        }, this.browserModel.getSelectedEntry(), AppLayoutModel.get().getSelected());
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
