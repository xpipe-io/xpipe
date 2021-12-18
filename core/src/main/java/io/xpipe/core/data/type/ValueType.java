package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.callback.DataTypeCallback;
import lombok.EqualsAndHashCode;

@JsonTypeName("value")
@EqualsAndHashCode
public class ValueType implements DataType {

    public static ValueType of() {
        return new ValueType();
    }

    private ValueType() {

    }

    @Override
    public String getName() {
        return "value";
    }

    @Override
    public boolean matches(DataStructureNode node) {
        return node.isValue();
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
