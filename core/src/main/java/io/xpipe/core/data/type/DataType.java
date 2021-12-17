package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.callback.DataTypeCallback;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataType {

    String getName();

    boolean matches(DataStructureNode node);

    default boolean isTuple() {
        return false;
    }

    default boolean isWildcard() {
        return false;
    }

    default boolean isArray() {
        return false;
    }

    default boolean isValue() {
        return false;
    }

    void traverseType(DataTypeCallback cb);
}
