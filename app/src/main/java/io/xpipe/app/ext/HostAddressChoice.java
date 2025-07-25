package io.xpipe.app.ext;

import io.xpipe.app.util.OptionsBuilder;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import lombok.Builder;
import lombok.Value;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

@Builder
@Value
public class HostAddressChoice {

    Property<HostAddress> value;
    boolean allowAdd;
    String translationKey;
    boolean includeDescription;

    public OptionsBuilder build() {
        var existing = value.getValue();
        var val = new SimpleObjectProperty<>(existing.get());
        var list = FXCollections.observableArrayList(existing.getAvailable());
        var options = new OptionsBuilder();
        if (includeDescription) {
            options.nameAndDescription(this.translationKey);
        } else {
            options.name(translationKey);
        }
        options.addComp(new HostAddressChoiceComp(val, list, allowAdd))
                .addProperty(val);
        options.bind(
                () -> {
                    return HostAddress.of(val.get(), list);
                },
                value);
        return options;
    }
}