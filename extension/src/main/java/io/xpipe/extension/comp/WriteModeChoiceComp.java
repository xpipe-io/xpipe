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
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class WriteModeChoiceComp extends SimpleComp implements Validatable {

    Property<WriteMode> selected;
    List<WriteMode> available;
    Validator validator = new SimpleValidator();
    Check check;

    public WriteModeChoiceComp(Property<WriteMode> selected, List<WriteMode> available) {
        this.selected = selected;
        this.available = available;
        if (available.size() == 1) {
            selected.setValue(available.get(0));
        }
        check = Validators.nonNull(validator, I18n.observable("mode"), selected);
    }

    @Override
    protected Region createSimple() {
        var a = available;
        var map = new LinkedHashMap<WriteMode, ObservableValue<String>>();
        for (WriteMode writeMode : a) {
            map.put(writeMode,I18n.observable(writeMode.getId()));
        }

        return new ToggleGroupComp<>(selected, map)
                .apply(struc -> {
                    for (int i = 0; i < a.size(); i++) {
                        new FancyTooltipAugment<>(a.get(i).getId() + "Description")
                                .augment(struc.get().getChildren().get(i));
                    }
                })
                .apply(struc -> check.decorates(struc.get()))
                .createRegion();
    }
}
