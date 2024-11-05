package io.xpipe.app.browser;

import io.xpipe.app.browser.session.BrowserAbstractSessionModel;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.browser.session.BrowserSessionTab;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.terminal.TerminalDockComp;
import io.xpipe.app.terminal.TerminalDockModel;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.app.terminal.TerminalViewInstance;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.UUID;

public final class BrowserTerminalDockTabModel extends BrowserSessionTab {

    private final BrowserSessionTab origin;
    private final ObservableList<UUID> terminalRequests;
    private final TerminalDockModel dockModel = new TerminalDockModel();
    private TerminalView.Listener listener;

    public BrowserTerminalDockTabModel(BrowserAbstractSessionModel<?> browserModel, BrowserSessionTab origin, ObservableList<UUID> terminalRequests) {
        super(browserModel, AppI18n.get("terminal"), null);
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
                    ((BrowserSessionModel) browserModel).unsplitTab(BrowserTerminalDockTabModel.this);
                }
            }
        };
        TerminalView.get().addListener(listener);
        this.browserModel.getSelectedEntry().addListener((observable, oldValue, newValue) -> {
            dockModel.toggleView(newValue == origin);
        });
        AppLayoutModel.get().getSelected().addListener((observable, oldValue, newValue) -> {
            dockModel.toggleView(AppLayoutModel.get().getEntries().indexOf(newValue) == 1);
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
