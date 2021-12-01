package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.type.callback.DataTypeCallback;

@JsonTypeName("value")
public class ValueType implements DataType {

    @Override
    public boolean isTuple() {
        return false;
    }

    @JsonIgnore
    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isValue() {
        return true;
    }

    @Override
    public void traverseType(DataTypeCallback cb) {
        cb.onValue();
    }
}
