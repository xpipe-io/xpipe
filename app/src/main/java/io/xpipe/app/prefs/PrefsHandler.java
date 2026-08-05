package io.xpipe.app.prefs;

import io.xpipe.app.platform.OptionsBuilder;

import javafx.beans.property.Property;

import tools.jackson.databind.JavaType;

public interface PrefsHandler {

    <T> void addSetting(
            String id, JavaType t, Property<T> property, OptionsBuilder builder, boolean requiresRestart, boolean log);
}
