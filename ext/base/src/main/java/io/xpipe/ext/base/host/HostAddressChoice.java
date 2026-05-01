package io.xpipe.ext.base.host;

import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IntFieldComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.ext.HostAddress;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.platform.OptionsBuilder;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Builder
@Value
public class HostAddressChoice {

    Property<HostAddress> addressProperty;
    Property<Integer> portProperty;
    boolean allowMutation;

    public OptionsBuilder build() {
        var existing = addressProperty.getValue();
        var val = new SimpleObjectProperty<>(existing != null ? existing.get() : null);
        var list = FXCollections.observableArrayList(existing != null ? existing.getAvailable() : new ArrayList<>());
        // For updating the options builder binding on list change, it doesn't support observable lists
        var listHashProp = new SimpleIntegerProperty(0);
        list.addListener((ListChangeListener<? super String>) c -> {
            listHashProp.set(c.getList().hashCode());
        });
        var options = new OptionsBuilder();
        var addressField = new HostAddressChoiceComp(val, list, allowMutation).hgrow();
        var sepLabel = new LabelComp(":")
                .apply(label -> AppFontSizes.xxl(label))
                .padding(new Insets(0, 0, 3, 0));
        var portField = new IntFieldComp(portProperty).maxWidth(63)
                .apply(textField -> textField.setAlignment(Pos.BASELINE_CENTER));
        var box = new HorizontalComp(List.of(addressField, sepLabel, portField)).spacing(5);
        options.nameAndDescription("connectionInformation");
        options.addComp(box);
        options.addProperty(portProperty);
        options.nonNull();
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
                }, addressProperty);
        return options;
    }
}
