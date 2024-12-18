package io.xpipe.app.util;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.App;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorageSecret;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class SecretRetrievalStrategyHelper {

    private static OptionsBuilder inPlace(Property<SecretRetrievalStrategy.InPlace> p) {
        var original = p.getValue() != null ? p.getValue().getValue() : null;
        var secretProperty = new SimpleObjectProperty<>(
                p.getValue() != null && p.getValue().getValue() != null
                        ? p.getValue().getValue().getInternalSecret().inPlace()
                        : null);
        return new OptionsBuilder()
                .addComp(new SecretFieldComp(secretProperty, true), secretProperty)
                .bind(
                        () -> {
                            var newSecret = secretProperty.get();
                            var changed = !Arrays.equals(
                                    newSecret != null ? newSecret.getSecret() : new char[0],
                                    original != null ? original.getSecret() : new char[0]);
                            return new SecretRetrievalStrategy.InPlace(
                                    changed ? DataStorageSecret.ofCurrentSecret(secretProperty.getValue()) : original);
                        },
                        p);
    }

    private static OptionsBuilder passwordManager(Property<SecretRetrievalStrategy.PasswordManager> p) {
        var keyProperty =
                new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getKey() : null);
        var content = new HorizontalComp(List.of(
                        new TextFieldComp(keyProperty)
                                .apply(struc -> struc.get().setPromptText("$KEY"))
                                .hgrow(),
                        new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                                    AppPrefs.get().selectCategory("passwordManager");
                                    App.getApp().getStage().requestFocus();
                                })
                                .grow(false, true)))
                .apply(struc -> struc.get().setSpacing(10))
                .apply(struc -> struc.get().focusedProperty().addListener((c, o, n) -> {
                    if (n) {
                        struc.get().getChildren().getFirst().requestFocus();
                    }
                }));
        return new OptionsBuilder()
                .addComp(content, keyProperty)
                .nonNull()
                .bind(
                        () -> {
                            return new SecretRetrievalStrategy.PasswordManager(keyProperty.getValue());
                        },
                        p);
    }

    private static OptionsBuilder customCommand(Property<SecretRetrievalStrategy.CustomCommand> p) {
        var cmdProperty =
                new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getCommand() : null);
        var content = new TextFieldComp(cmdProperty);
        return new OptionsBuilder()
                .addComp(content, cmdProperty)
                .bind(
                        () -> {
                            return new SecretRetrievalStrategy.CustomCommand(cmdProperty.getValue());
                        },
                        p);
    }

    public static OptionsBuilder comp(Property<SecretRetrievalStrategy> s, boolean allowNone) {
        SecretRetrievalStrategy strat = s.getValue();
        var inPlace = new SimpleObjectProperty<>(strat instanceof SecretRetrievalStrategy.InPlace i ? i : null);
        var passwordManager =
                new SimpleObjectProperty<>(strat instanceof SecretRetrievalStrategy.PasswordManager i ? i : null);
        var customCommand =
                new SimpleObjectProperty<>(strat instanceof SecretRetrievalStrategy.CustomCommand i ? i : null);
        var map = new LinkedHashMap<ObservableValue<String>, OptionsBuilder>();
        if (allowNone) {
            map.put(AppI18n.observable("app.none"), new OptionsBuilder());
        }
        map.put(AppI18n.observable("app.password"), inPlace(inPlace));
        map.put(AppI18n.observable("app.passwordManager"), passwordManager(passwordManager));
        map.put(AppI18n.observable("app.customCommand"), customCommand(customCommand));
        map.put(AppI18n.observable("app.prompt"), new OptionsBuilder());

        int offset = allowNone ? 0 : -1;
        var selected = new SimpleIntegerProperty(
                strat instanceof SecretRetrievalStrategy.None
                        ? offset
                        : strat instanceof SecretRetrievalStrategy.InPlace
                                ? offset + 1
                                : strat instanceof SecretRetrievalStrategy.PasswordManager
                                        ? offset + 2
                                        : strat instanceof SecretRetrievalStrategy.CustomCommand
                                                ? offset + 3
                                                : strat instanceof SecretRetrievalStrategy.Prompt
                                                        ? offset + 4
                                                        : strat == null ? -1 : 0);
        return new OptionsBuilder()
                .choice(selected, map)
                .bindChoice(
                        () -> {
                            return switch (selected.get() - offset) {
                                case 0 -> new SimpleObjectProperty<>(
                                        allowNone ? new SecretRetrievalStrategy.None() : null);
                                case 1 -> inPlace;
                                case 2 -> passwordManager;
                                case 3 -> customCommand;
                                case 4 -> new SimpleObjectProperty<>(new SecretRetrievalStrategy.Prompt());
                                default -> new SimpleObjectProperty<>();
                            };
                        },
                        s);
    }
}
