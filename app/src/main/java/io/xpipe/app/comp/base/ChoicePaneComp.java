package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import lombok.Setter;

import java.util.List;
import java.util.function.Function;

public class ChoicePaneComp extends Comp<CompStructure<VBox>> {

    private final List<Entry> entries;
    private final Property<Entry> selected;

    @Setter
    private Function<ComboBox<Entry>, Region> transformer = c -> c;

    public ChoicePaneComp(List<Entry> entries, Property<Entry> selected) {
        this.entries = entries;
        this.selected = selected;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var list = FXCollections.observableArrayList(entries);
        var cb = MenuHelper.<Entry>createComboBox();
        cb.setItems(list);
        cb.setOnKeyPressed(event -> {
            if (!cb.isShowing() && event.getCode().equals(KeyCode.ENTER)) {
                cb.show();
                event.consume();
            }
        });
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
        vbox.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                vbox.getChildren().getFirst().requestFocus();
            }
        });

        cb.prefWidthProperty().bind(vbox.widthProperty());
        cb.valueProperty().subscribe(n -> {
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
            }
        });

        cb.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && vbox.getChildren().size() > 1) {
                vbox.getChildren().get(1).requestFocus();
            }
        });

        cb.valueProperty().addListener((observable, oldValue, newValue) -> {
            selected.setValue(newValue);
        });
        selected.subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> cb.valueProperty().set(val));
        });

        vbox.getStyleClass().add("choice-pane-comp");

        return new SimpleCompStructure<>(vbox);
    }

    public record Entry(ObservableValue<String> name, Comp<?> comp) {

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
