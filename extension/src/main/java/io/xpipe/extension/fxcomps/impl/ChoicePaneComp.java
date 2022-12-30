package io.xpipe.extension.fxcomps.impl;

import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.function.Function;

@FieldDefaults(
        makeFinal = true,
        level = AccessLevel.PRIVATE
)
@AllArgsConstructor
public class ChoicePaneComp extends Comp<CompStructure<VBox>> {

    List<Entry> entries;
    Property<Entry> selected;
    Function<ComboBox<Entry>, Region> transformer = c -> c;

    @Override
    public CompStructure<VBox> createBase() {
        var list = FXCollections.observableArrayList(entries);
        var cb = new ComboBox<Entry>(list);
        cb.setConverter(new StringConverter<>() {
            @Override
            public String toString(Entry object) {
                if (object == null) {
                    return I18n.get("extension.none");
                }

                return object.name().getValue();
            }

            @Override
            public Entry fromString(String string) {
                throw new UnsupportedOperationException();
            }
        });

        var vbox = new VBox(transformer.apply(cb));
        vbox.setFillWidth(true);
        cb.prefWidthProperty().bind(vbox.widthProperty());
        cb.valueProperty().addListener((c, o, n) -> {
            if (n == null) {
                vbox.getChildren().remove(1);
            } else {
                if (vbox.getChildren().size() == 1) {
                    vbox.getChildren().add(n.comp().createRegion());
                } else {
                    vbox.getChildren().set(1, n.comp().createRegion());
                }
            }
        });

        cb.valueProperty().addListener((observable, oldValue, newValue) -> {
            selected.setValue(newValue);
        });
        SimpleChangeListener.apply(selected, val -> {
            PlatformThread.runLaterIfNeeded(() -> cb.valueProperty().set(val));
        });

        vbox.getStyleClass().add("choice-pane-comp");

        return new SimpleCompStructure<>(vbox);
    }

    public record Entry(ObservableValue<String> name, Comp<?> comp) {
    }
}
