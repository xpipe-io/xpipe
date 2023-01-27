package io.xpipe.ext.jdbc.auth;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface AuthMethod {

    default boolean validate() {
        return true;
    }
}
