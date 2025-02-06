package io.xpipe.ext.base.service;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.OptionsBuilder;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

import java.util.LinkedHashMap;

public class ServiceProtocolTypeHelper {

    private static OptionsBuilder custom(Property<ServiceProtocolType.Custom> p) {
        var firstFocus = new SimpleBooleanProperty(false);
        var path = new SimpleStringProperty(p.getValue() != null ? p.getValue().getCommandTemplate() : null);
        var comp = new TextFieldComp(path).apply(struc -> {
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!firstFocus.get()) {
                    struc.get().getParent().requestFocus();
                    firstFocus.set(true);
                }
            });
            struc.get().setPromptText("mycommand open localhost:$PORT");
        });
        return new OptionsBuilder()
                .nameAndDescription("serviceCommand")
                .addComp(comp, path)
                .bind(
                        () -> {
                            return new ServiceProtocolType.Custom(path.get());
                        },
                        p);
    }

    private static OptionsBuilder http(Property<ServiceProtocolType.Http> p) {
        var path = new SimpleStringProperty(p.getValue() != null ? p.getValue().getPath() : null);
        return new OptionsBuilder()
                .nameAndDescription("servicePath")
                .addString(path)
                .bind(
                        () -> {
                            return new ServiceProtocolType.Http(path.get());
                        },
                        p);
    }

    private static OptionsBuilder https(Property<ServiceProtocolType.Https> p) {
        var path = new SimpleStringProperty(p.getValue() != null ? p.getValue().getPath() : null);
        return new OptionsBuilder()
                .nameAndDescription("servicePath")
                .addString(path)
                .bind(
                        () -> {
                            return new ServiceProtocolType.Https(path.get());
                        },
                        p);
    }

    public static OptionsBuilder choice(Property<ServiceProtocolType> serviceProtocolType) {
        var ex = serviceProtocolType.getValue();
        var http = new SimpleObjectProperty<>(ex instanceof ServiceProtocolType.Http h ? h : null);
        var https = new SimpleObjectProperty<>(ex instanceof ServiceProtocolType.Https h ? h : null);
        var custom = new SimpleObjectProperty<>(ex instanceof ServiceProtocolType.Custom c ? c : null);
        var selected = new SimpleIntegerProperty(
                ex instanceof ServiceProtocolType.Undefined
                        ? 0
                        : ex instanceof ServiceProtocolType.Http
                                ? 1
                                : ex instanceof ServiceProtocolType.Https ? 2 :
                        ex instanceof ServiceProtocolType.Custom ? 3 : -1);
        var available = new LinkedHashMap<ObservableValue<String>, OptionsBuilder>();
        available.put(AppI18n.observable("undefined"), new OptionsBuilder());
        available.put(AppI18n.observable("http"), http(http));
        available.put(AppI18n.observable("https"), https(https));
        available.put(AppI18n.observable("custom"), custom(custom));
        return new OptionsBuilder()
                .nameAndDescription("serviceProtocolType")
                .choice(selected, available)
                .bindChoice(
                        () -> {
                            return switch (selected.get()) {
                                case 0 -> new SimpleObjectProperty<>(new ServiceProtocolType.Undefined());
                                case 1 -> http;
                                case 2 -> https;
                                case 3 -> custom;
                                default -> new SimpleObjectProperty<>();
                            };
                        },
                        serviceProtocolType);
    }
}
