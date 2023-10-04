package io.xpipe.ext.base.script;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.ToggleGroupComp;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Arrays;
import java.util.LinkedHashMap;

@Value
@EqualsAndHashCode(callSuper = true)
public class ScriptStoreTypeChoiceComp extends SimpleComp {

    Property<SimpleScriptStore.ExecutionType> selected;
    SimpleScriptStore.ExecutionType[] available = SimpleScriptStore.ExecutionType.values();

    @Override
    protected Region createSimple() {
        var map = new LinkedHashMap<SimpleScriptStore.ExecutionType, ObservableValue<String>>();
        Arrays.stream(available).forEach(executionType -> {
            map.put(executionType, AppI18n.observable(executionType.getId()));
        });
        return new ToggleGroupComp<>(selected, new SimpleObjectProperty<>(map))
                .createRegion();
    }
}
