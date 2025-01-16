package io.xpipe.app.ext;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface UserBasedValue<T> {

    Class<T> getWrappedClass();

    T unwrap();

    boolean isPerUser();
}
