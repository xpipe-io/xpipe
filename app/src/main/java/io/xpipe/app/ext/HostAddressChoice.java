package io.xpipe.app.ext;

import io.xpipe.app.util.OptionsBuilder;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Builder
@Value
public class HostAddressChoice {

    Property<HostAddressValue> value;
    boolean allowStoreChoice;
    String translationKey;
    String translationDescriptionKey;

    public OptionsBuilder build() {
        var existing = value.getValue();
        var ref = new SimpleObjectProperty<>( existing instanceof HostAddressValue.Ref r ? r.getRef() : null);
        var inPlace = new SimpleObjectProperty<>(existing instanceof HostAddressValue.InPlace i ? i.getDefault() : null);
        var list = FXCollections.observableArrayList(existing instanceof HostAddressValue.InPlace i ? i.getAll() : List.of());
        var inPlaceSelected = ref.isNull();
        var refSelected = ref.isNotNull();
        var options = new OptionsBuilder()
                .name(translationKey)
                .description(translationDescriptionKey)
                .addComp(new HostAddressChoiceComp(ref, inPlace, list))
                .addProperty(inPlace)
                .addProperty(ref);
        options.bind(
                () -> {
                    if (ref.get() != null) {
                        return HostAddressValue.Ref.builder().ref(ref.get()).build();
                    } else if (inPlace.get() != null) {
                        var l = new ArrayList<HostAddress>();
                        l.add(inPlace.get());
                        l.addAll(list);
                        return HostAddressValue.InPlace.builder().addresses(l).build();
                    } else {
                        return null;
                    }
                },
                value);
        return options;
    }
}
