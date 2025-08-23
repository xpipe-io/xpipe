package io.xpipe.app.ext;

import io.xpipe.app.util.OptionsBuilder;

import javafx.beans.property.*;
import javafx.collections.FXCollections;

import javafx.collections.ListChangeListener;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;

@Builder
@Value
public class HostAddressChoice {

    Property<HostAddress> value;
    boolean allowMutation;
    String translationKey;
    boolean includeDescription;

    public OptionsBuilder build() {
        var existing = value.getValue();
        var val = new SimpleObjectProperty<>(existing != null ? existing.get() : null);
        var list = FXCollections.observableArrayList(existing != null ? existing.getAvailable() : new ArrayList<>());
        // For updating the options builder binding on list change, it doesn't support observable lists
        var sizeProp = new SimpleIntegerProperty(0);
        list.addListener((ListChangeListener<? super String>) c -> {
            sizeProp.set(c.getList().size());
        });
        var options = new OptionsBuilder();
        if (includeDescription) {
            options.nameAndDescription(this.translationKey);
        } else {
            options.name(translationKey);
        }
        options.addComp(new HostAddressChoiceComp(val, list, allowMutation)).addProperty(val).addProperty(sizeProp);
        options.bind(
                () -> {
                    var fullList = new ArrayList<>(list);
                    if (val.getValue() != null && !fullList.contains(val.getValue())) {
                        fullList.add(val.getValue());
                    }

                    var effectiveValue =
                            val.getValue() != null ? val.getValue() : fullList.size() > 0 ? fullList.getFirst() : null;
                    return HostAddress.of(effectiveValue, fullList);
                },
                value);
        return options;
    }
}
