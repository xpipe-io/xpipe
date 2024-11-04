package io.xpipe.app.browser;

import io.xpipe.app.browser.session.BrowserAbstractSessionModel;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.browser.session.BrowserSessionTab;
import io.xpipe.app.core.AppI18n;
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

    private final ObservableList<UUID> terminalRequests;
    private final TerminalDockModel dockModel = new TerminalDockModel();

    public BrowserTerminalDockTabModel(BrowserAbstractSessionModel<?> browserModel, ObservableList<UUID> terminalRequests) {
        super(browserModel, AppI18n.get("terminal"), null);
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
        var terminals = new ArrayList<TerminalViewInstance>();
        TerminalView.get().addListener(new TerminalView.Listener() {
            @Override
            public void onSessionOpened(TerminalView.Session session) {
                if (!terminalRequests.contains(session.getRequest())) {
                    return;
                }

                var tv = terminals.stream().filter(instance -> instance.getTerminalProcess().equals(session.getTerminal())).findFirst();
                tv.ifPresent(instance -> {
                    dockModel.trackTerminal(instance);
                });
            }

            @Override
            public void onSessionClosed(TerminalView.Session session) {

            }

            @Override
            public void onTerminalOpened(TerminalViewInstance instance) {
                terminals.add(instance);
            }

            @Override
            public void onTerminalClosed(TerminalViewInstance instance) {
terminals.remove(instance);
            }
        });
    }

    @Override
    public void close() {}

    @Override
    public String getIcon() {
        return null;
    }

    @Override
    public DataColor getColor() {
        return null;
    }

    @Override
    public boolean isCloseable() {
        return false;
    }
}
