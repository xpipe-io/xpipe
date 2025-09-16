package io.xpipe.app.ext;

import io.xpipe.app.issue.TrackEvent;

import io.xpipe.app.platform.OptionsBuilder;
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
        if (existing != null) {
            list.remove(existing.get());
        }
        // For updating the options builder binding on list change, it doesn't support observable lists
        var listHashProp = new SimpleIntegerProperty(0);
        list.addListener((ListChangeListener<? super String>) c -> {
            listHashProp.set(c.getList().hashCode());
        });
        var options = new OptionsBuilder();
        if (includeDescription) {
            options.nameAndDescription(this.translationKey);
        } else {
            options.name(translationKey);
        }
        options.addComp(new HostAddressChoiceComp(val, list, allowMutation));
        options.addProperty(val);
        options.nonNull();
        options.addProperty(listHashProp);
        options.bind(
                () -> {
                    var fullList = new ArrayList<>(list);
                    if (val.getValue() != null && !fullList.contains(val.getValue())) {
                        fullList.add(val.getValue());
                    }

                    TrackEvent.withTrace("Host address update")
                            .tag("address", val.getValue())
                            .tag("list", fullList)
                            .handle();

                    return HostAddress.of(val.getValue(), fullList);
                },
                value);
        return options;
    }
}
