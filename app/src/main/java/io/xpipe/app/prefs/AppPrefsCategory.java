package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.structure.Element;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Setting;
import io.xpipe.app.fxcomps.Comp;
import javafx.beans.property.Property;
import lombok.SneakyThrows;

public abstract class AppPrefsCategory {

    protected final AppPrefs prefs;

    public AppPrefsCategory(AppPrefs prefs) {
        this.prefs = prefs;
    }

    @SneakyThrows
    public static Setting<?, ?> lazyNode(String name, Comp<?> comp, Property<?> property) {
        var ctr = Setting.class.getDeclaredConstructor(String.class, Element.class, Property.class);
        ctr.setAccessible(true);
        return ctr.newInstance(name, new LazyNodeElement<>(() -> comp.createRegion()), property);
    }

    protected abstract Category create();
}
