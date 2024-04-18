package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.core.store.DataFlow;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.LinkedHashMap;

@Value
@EqualsAndHashCode(callSuper = true)
public class DataStoreFlowChoiceComp extends SimpleComp {

    Property<DataFlow> selected;
    DataFlow[] available;

    @Override
    protected Region createSimple() {
        var map = new LinkedHashMap<DataFlow, ObservableValue<String>>();
        map.put(DataFlow.INPUT, AppI18n.observable("app.input"));
        map.put(DataFlow.OUTPUT, AppI18n.observable("app.output"));
        map.put(DataFlow.INPUT_OUTPUT, AppI18n.observable("app.inout"));
        return new ToggleGroupComp<>(selected, new SimpleObjectProperty<>(map))
                .apply(struc -> {
                    new TooltipAugment<>("app.inputDescription")
                            .augment(struc.get().getChildren().get(0));
                    new TooltipAugment<>("app.outputDescription")
                            .augment(struc.get().getChildren().get(1));
                    new TooltipAugment<>("app.inoutDescription")
                            .augment(struc.get().getChildren().get(2));
                })
                .createRegion();
    }
}
