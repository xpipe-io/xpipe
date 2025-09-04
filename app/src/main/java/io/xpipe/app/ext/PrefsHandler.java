package io.xpipe.app.ext;

import io.xpipe.app.platform.OptionsBuilder;

import javafx.beans.property.Property;

import com.fasterxml.jackson.databind.JavaType;

public interface PrefsHandler {

    <T> void addSetting(
            String id, JavaType t, Property<T> property, OptionsBuilder builder, boolean requiresRestart, boolean log);
}
