package io.xpipe.app.browser;

import io.xpipe.app.browser.session.BrowserAbstractSessionModel;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.browser.session.BrowserSessionTab;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.DataColor;
import io.xpipe.core.store.*;

import javafx.beans.property.*;

public final class BrowserHomeModel extends BrowserSessionTab {

    public BrowserHomeModel(BrowserAbstractSessionModel<?> browserModel) {
        super(browserModel, AppI18n.get("overview"), null);
    }

    @Override
    public Comp<?> comp() {
        return new BrowserWelcomeComp((BrowserSessionModel) browserModel);
    }

    @Override
    public boolean canImmediatelyClose() {
        return true;
    }

    @Override
    public void init() throws Exception {}

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
