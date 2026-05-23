package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.Translatable;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ChoiceComp<T> extends RegionBuilder<ComboBox<T>> {

    Property<T> value;
    ObservableValue<Map<T, ObservableValue<String>>> range;
    boolean includeNone;

    public ChoiceComp(Property<T> value, Map<T, ObservableValue<String>> range, boolean includeNone) {
        this.value = value;
        this.range = new SimpleObjectProperty<>(range);
        this.includeNone = includeNone;
    }

    public static <T extends Translatable> ChoiceComp<T> ofTranslatable(
            Property<T> value, List<T> range, boolean includeNone) {
        var map = range.stream()
                .collect(
                        Collectors.toMap(o -> o, Translatable::toTranslatedString, (v1, v2) -> v2, LinkedHashMap::new));
        return new ChoiceComp<>(value, map, includeNone);
    }

    @Override
    public ComboBox<T> createSimple() {
        var cb = MenuHelper.<T>createComboBox();

        Supplier<StringConverter<T>> converter = () -> new StringConverter<>() {
            @Override
            public String toString(T object) {
                if (object == null) {
                    return AppI18n.get("none");
                }

                var found = range.getValue().get(object);
                if (found == null) {
                    return "";
                }

                return found.getValue();
            }

            @Override
            public T fromString(String string) {
                throw new UnsupportedOperationException();
            }
        };
        cb.setConverter(converter.get());

        // Reset converter on language change to force an update
        // This does not work properly in older JFX versions, see JDK-8384006
        var ref = new WeakReference<>(cb);
        AppI18n.activeLanguage().subscribe((v) -> {
            var refValue = ref.get();
            if (refValue != null) {
                Platform.runLater(() -> {
                    refValue.setConverter(converter.get());
                });
            }
        });

        range.subscribe(c -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var list = FXCollections.observableArrayList(c.keySet());
                if (!list.contains(null) && includeNone) {
                    list.addFirst(null);
                }

                cb.getItems().setAll(list);

                if (list.size() == 1) {
                    value.setValue(list.getFirst());
                } else if (list.isEmpty()) {
                    value.setValue(null);
                }
            });
        });

        cb.valueProperty().addListener((observable, oldValue, newValue) -> {
            value.setValue(newValue);
        });
        value.subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> cb.valueProperty().set(val));
        });

        cb.getStyleClass().add("choice-comp");
        cb.setMaxWidth(10000);
        return cb;
    }
}
