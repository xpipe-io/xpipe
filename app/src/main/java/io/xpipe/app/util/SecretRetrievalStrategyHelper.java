package io.xpipe.app.util;

import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.LinkedHashMap;

public class SecretRetrievalStrategyHelper {

    private static OptionsBuilder inPlace(Property<SecretRetrievalStrategy.InPlace> p) {
        var secretProperty = new SimpleObjectProperty<>(
                p.getValue() != null ? p.getValue().getValue() : null);
        return new OptionsBuilder()
                .name("password")
                .addComp(new SecretFieldComp(secretProperty), secretProperty)
                .bind(
                        () -> {
                            return new SecretRetrievalStrategy.InPlace(secretProperty.getValue());
                        },
                        p);
    }

    public static OptionsBuilder comp(Property<SecretRetrievalStrategy> s) {
        var inPlace = new SimpleObjectProperty<>(s.getValue() instanceof SecretRetrievalStrategy.InPlace i ? i : null);
        var command = new SimpleObjectProperty<>(s.getValue() instanceof SecretRetrievalStrategy.Command c ? c : null);
        var map = new LinkedHashMap<String, OptionsBuilder>();
        map.put("none", new OptionsBuilder());
        map.put("password", inPlace(inPlace));
        map.put("prompt", new OptionsBuilder());
        // map.put("command", new OptionsBuilder());
        map.put("keepass", new OptionsBuilder());
        var selected = new SimpleIntegerProperty();
        return new OptionsBuilder()
                .choice(selected, map)
                .bindChoice(
                        () -> {
                            return switch (selected.get()) {
                                case 0 -> new SimpleObjectProperty<>(new SecretRetrievalStrategy.None());
                                case 1 -> inPlace;
                                case 2 -> new SimpleObjectProperty<>(new SecretRetrievalStrategy.Prompt());
                                // case 3 -> command;
                                case 3 -> new SimpleObjectProperty<>(new SecretRetrievalStrategy.KeePass("a"));
                                default -> new SimpleObjectProperty<>();
                            };
                        },
                        s);
    }
}
