package io.xpipe.ext.base.service;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.OptionsBuilder;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.LinkedHashMap;

public class ServiceProtocolTypeHelper {

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
        var selected = new SimpleIntegerProperty(ex instanceof ServiceProtocolType.None ? 0 :
                ex instanceof ServiceProtocolType.Http ? 1 : ex instanceof ServiceProtocolType.Https ? 2 : -1);
        var available = new LinkedHashMap<ObservableValue<String>, OptionsBuilder>();
        available.put(AppI18n.observable("none"), new OptionsBuilder());
        available.put(AppI18n.observable("http"), http(http));
        available.put(AppI18n.observable("https"), https(https));
        return new OptionsBuilder()
                .nameAndDescription("serviceProtocolType")
                .choice(selected, available)
                .bindChoice(() -> {
                    return switch (selected.get()) {
                        case 0 -> new SimpleObjectProperty<>(new ServiceProtocolType.None());
                        case 1 -> http;
                        case 2 -> https;
                        default -> new SimpleObjectProperty<>();
                    };
                }, serviceProtocolType);
    }
}
