package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserAbstractSessionModel;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserSessionTab;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataColor;

import javafx.beans.value.ObservableValue;

public final class BrowserHistoryTabModel extends BrowserSessionTab {

    public BrowserHistoryTabModel(BrowserAbstractSessionModel<?> browserModel) {
        super(browserModel);
    }

    @Override
    public Comp<?> comp() {
        return new BrowserHistoryTabComp((BrowserFullSessionModel) browserModel);
    }

    @Override
    public boolean canImmediatelyClose() {
        return true;
    }

    @Override
    public void init() {}

    @Override
    public void close() {}

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("history").map(s -> " " + s + " ");
    }

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
