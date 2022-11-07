package io.xpipe.extension.comp;

import io.xpipe.core.source.WriteMode;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.SimpleValidator;
import io.xpipe.extension.util.Validatable;
import io.xpipe.extension.util.Validator;
import io.xpipe.extension.util.Validators;
import io.xpipe.fxcomps.SimpleComp;
import io.xpipe.fxcomps.util.PlatformThread;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.synedra.validatorfx.Check;

import java.util.LinkedHashMap;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = true)
public class WriteModeChoiceComp extends SimpleComp implements Validatable {

    Property<WriteMode> selected;
    ObservableList<WriteMode> available;
    Validator validator = new SimpleValidator();
    Check check;

    public WriteModeChoiceComp(Property<WriteMode> selected, ObservableList<WriteMode> available) {
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
        Property<Map<WriteMode, ObservableValue<String>>> map = new SimpleObjectProperty<>(new LinkedHashMap<WriteMode, ObservableValue<String>>());
        for (WriteMode writeMode : a) {
            map.getValue().put(writeMode,I18n.observable(writeMode.getId()));
        }

        PlatformThread.sync(available).addListener((ListChangeListener<? super WriteMode>) c -> {
            var newMap = new LinkedHashMap<WriteMode, ObservableValue<String>>();
            for (WriteMode writeMode : c.getList()) {
                newMap.put(writeMode,I18n.observable(writeMode.getId()));
            }
            map.setValue(newMap);

            if (c.getList().size() == 1) {
                selected.setValue(c.getList().get(0));
            }
        });

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
