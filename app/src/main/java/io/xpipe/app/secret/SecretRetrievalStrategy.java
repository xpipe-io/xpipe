package io.xpipe.app.secret;

import io.xpipe.app.ext.ValidationException;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SecretNoneStrategy.class),
    @JsonSubTypes.Type(value = SecretInPlaceStrategy.class),
    @JsonSubTypes.Type(value = SecretPromptStrategy.class),
    @JsonSubTypes.Type(value = SecretCustomCommandStrategy.class),
    @JsonSubTypes.Type(value = SecretPasswordManagerStrategy.class)
})
public interface SecretRetrievalStrategy {

    static List<Class<?>> getSubclasses() {
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
