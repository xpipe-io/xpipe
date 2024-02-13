package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.ChoiceComp;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;

public class ElevationAccessChoiceComp extends SimpleComp {

    private final Property<ElevationAccess> value;

    public ElevationAccessChoiceComp(Property<ElevationAccess> value) {this.value = value;}

    @Override
    protected Region createSimple() {
        var map = new LinkedHashMap<ElevationAccess, ObservableValue<String>>();
        map.put(ElevationAccess.ALLOW, AppI18n.observable("allow"));
        map.put(ElevationAccess.ASK, AppI18n.observable("ask"));
        map.put(ElevationAccess.DENY, AppI18n.observable("deny"));
        var c = new ChoiceComp<>(value, map, false);
        return c.createRegion();
    }
}
