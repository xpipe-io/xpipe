package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.SimpleValidator;
import io.xpipe.app.util.Validatable;
import io.xpipe.app.util.Validator;
import io.xpipe.core.source.WriteMode;
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
        check = Validator.nonNull(validator, AppI18n.observable("mode"), selected);
    }

    @Override
    protected Region createSimple() {
        var a = available;
        Property<Map<WriteMode, ObservableValue<String>>> map =
                new SimpleObjectProperty<>(new LinkedHashMap<WriteMode, ObservableValue<String>>());
        for (WriteMode writeMode : a) {
            map.getValue().put(writeMode, AppI18n.observable(writeMode.getId()));
        }

        PlatformThread.sync(available).addListener((ListChangeListener<? super WriteMode>) c -> {
            var newMap = new LinkedHashMap<WriteMode, ObservableValue<String>>();
            for (WriteMode writeMode : c.getList()) {
                newMap.put(writeMode, AppI18n.observable(writeMode.getId()));
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
