package io.xpipe.extension.comp;

import io.xpipe.core.store.DataFlow;
import io.xpipe.extension.I18n;
import io.xpipe.fxcomps.SimpleComp;
import javafx.beans.property.Property;
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
        map.put(DataFlow.INPUT, I18n.observable("extension.input"));
        map.put(DataFlow.OUTPUT, I18n.observable("extension.output"));
        map.put(DataFlow.INPUT_OUTPUT, I18n.observable("extension.inout"));
        return new ToggleGroupComp<>(selected, map).apply(struc -> {
            new FancyTooltipAugment<>("extension.inputDescription").augment(struc.get().getChildren().get(0));
            new FancyTooltipAugment<>("extension.outputDescription").augment(struc.get().getChildren().get(1));
            new FancyTooltipAugment<>("extension.inoutDescription").augment(struc.get().getChildren().get(2));
        }).createRegion();
    }
}
