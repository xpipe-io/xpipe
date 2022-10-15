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

import java.util.Arrays;
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
        if (available.length == 1) {
            selected.setValue(available[0]);
        }
        check = Validators.nonNull(validator, I18n.observable("mode"), selected);
    }

    @Override
    protected Region createSimple() {
        var a = Arrays.asList(available);
        var map = new LinkedHashMap<WriteMode, ObservableValue<String>>();
        var replaceIndex = -1;
        if (a.contains(WriteMode.REPLACE)) {
            map.put(WriteMode.REPLACE, I18n.observable("replace"));
            replaceIndex = 0;
        }

        var appendIndex = -1;
        if (a.contains(WriteMode.APPEND)) {
            map.put(WriteMode.APPEND, I18n.observable("append"));
            appendIndex = replaceIndex + 1;
        }

        var prependIndex = -1;
        if (a.contains(WriteMode.PREPEND)) {
            map.put(WriteMode.PREPEND, I18n.observable("prepend"));
            prependIndex = Math.max(replaceIndex, appendIndex) + 1;
        }

        int finalReplaceIndex = replaceIndex;
        int finalAppendIndex = appendIndex;
        int finalPrependIndex = prependIndex;
        return new ToggleGroupComp<>(selected, map)
                .apply(struc -> {
                    if (finalReplaceIndex != -1)
                        new FancyTooltipAugment<>("extension.replaceDescription")
                                .augment(struc.get().getChildren().get(0));
                    if (finalAppendIndex != -1)
                        new FancyTooltipAugment<>("extension.appendDescription")
                                .augment(struc.get().getChildren().get(finalAppendIndex));
                    if (finalPrependIndex != -1)
                        new FancyTooltipAugment<>("extension.prependDescription")
                                .augment(struc.get().getChildren().get(finalPrependIndex));
                })
                .apply(struc -> check.decorates(struc.get()))
                .createRegion();
    }
}
