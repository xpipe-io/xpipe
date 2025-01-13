package io.xpipe.app.ext;

import io.xpipe.app.comp.Comp;

import javafx.beans.property.Property;

import com.fasterxml.jackson.databind.JavaType;

public interface PrefsHandler {

    <T> void addSetting(String id, JavaType t, Property<T> property, Comp<?> comp, boolean requiresRestart);
}
