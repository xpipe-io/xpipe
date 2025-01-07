package io.xpipe.app.ext;

import com.fasterxml.jackson.databind.JavaType;
import io.xpipe.app.comp.Comp;

import javafx.beans.property.Property;

public interface PrefsHandler {

    <T> void addSetting(String id, JavaType t, Property<T> property, Comp<?> comp, boolean requiresRestart);
}
