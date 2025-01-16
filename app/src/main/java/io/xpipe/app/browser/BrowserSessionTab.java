package io.xpipe.app.browser;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.storage.DataColor;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import lombok.Getter;

@Getter
public abstract class BrowserSessionTab {

    protected final BooleanProperty busy = new SimpleBooleanProperty();
    protected final BrowserAbstractSessionModel<?> browserModel;
    protected final Property<BrowserSessionTab> splitTab = new SimpleObjectProperty<>();

    public BrowserSessionTab(BrowserAbstractSessionModel<?> browserModel) {
        this.browserModel = browserModel;
    }

    public abstract Comp<?> comp();

    public abstract boolean canImmediatelyClose();

    public abstract void init() throws Exception;

    public abstract void close();

    public abstract ObservableValue<String> getName();

    public abstract String getIcon();

    public abstract DataColor getColor();

    public boolean isCloseable() {
        return true;
    }
}
