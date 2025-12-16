package io.xpipe.app.secret;

import io.xpipe.app.ext.ValidationException;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface SecretRetrievalStrategy {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(SecretNoneStrategy.class);
        l.add(SecretInPlaceStrategy.class);
        l.add(SecretPromptStrategy.class);
        l.add(SecretPasswordManagerStrategy.class);
        l.add(SecretCustomCommandStrategy.class);
        return l;
    }

    default void checkComplete() throws ValidationException {}

    SecretQuery query();

    default boolean expectsQuery() {
        return true;
    }
}
