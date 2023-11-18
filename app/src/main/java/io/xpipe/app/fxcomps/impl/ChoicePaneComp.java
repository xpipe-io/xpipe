package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
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

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ChoicePaneComp extends Comp<CompStructure<VBox>> {

    List<Entry> entries;
    Property<Entry> selected;
    Function<ComboBox<Entry>, Region> transformer = c -> c;

    @Override
    public CompStructure<VBox> createBase() {
        var list = FXCollections.observableArrayList(entries);
        var cb = new ComboBox<>(list);
        cb.getSelectionModel().select(selected.getValue());
        cb.setConverter(new StringConverter<>() {
            @Override
            public String toString(Entry object) {
                if (object == null || object.name() == null) {
                    return "";
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
        SimpleChangeListener.apply(cb.valueProperty(), n -> {
            if (n == null) {
                if (vbox.getChildren().size() > 1) {
                    vbox.getChildren().remove(1);
                }
            } else {
                var region = n.comp().createRegion();
                if (vbox.getChildren().size() == 1) {
                    vbox.getChildren().add(region);
                } else {
                    vbox.getChildren().set(1, region);
                }

                region.requestFocus();
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

    public record Entry(ObservableValue<String> name, Comp<?> comp) {}
}
