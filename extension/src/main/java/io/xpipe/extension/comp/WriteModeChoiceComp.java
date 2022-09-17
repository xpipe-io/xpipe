package io.xpipe.extension.comp;

import io.xpipe.core.source.WriteMode;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.SimpleValidator;
import io.xpipe.extension.util.Validatable;
import io.xpipe.extension.util.Validator;
import io.xpipe.extension.util.Validators;
import io.xpipe.fxcomps.SimpleComp;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.synedra.validatorfx.Check;

import java.util.LinkedHashMap;

@Value
@EqualsAndHashCode(callSuper = true)
public class WriteModeChoiceComp extends SimpleComp implements Validatable {

    Property<WriteMode> selected;
    WriteMode[] available;
    Validator validator = new SimpleValidator();
    Check check;

     public WriteModeChoiceComp(Property<WriteMode> selected, WriteMode[] available) {
         this.selected = selected;
         this.available = available;
         check = Validators.nonNull(validator, I18n.observable("mode"), selected);
     }

    @Override
    protected Region createSimple() {
        var map = new LinkedHashMap<WriteMode, ObservableValue<String>>();
        map.put(WriteMode.REPLACE, I18n.observable("replace"));
        map.put(WriteMode.APPEND, I18n.observable("append"));
        map.put(WriteMode.PREPEND, I18n.observable("prepend"));
        return new ToggleGroupComp<>(selected, map).apply(struc -> {
            new FancyTooltipAugment<>("extension.replaceDescription").augment(struc.get().getChildren().get(0));
            new FancyTooltipAugment<>("extension.appendDescription").augment(struc.get().getChildren().get(1));
            new FancyTooltipAugment<>("extension.prependDescription").augment(struc.get().getChildren().get(2));
        }).apply(struc -> check.decorates(struc.get())).createRegion();
    }
}
