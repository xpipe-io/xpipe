package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.data.type.callback.DataTypeCallback;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface DataType {

    boolean isTuple();

    boolean isArray();

    boolean isValue();

    void traverseType(DataTypeCallback cb);
}
