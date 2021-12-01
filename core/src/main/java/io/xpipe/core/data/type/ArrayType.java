package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.type.callback.DataTypeCallback;

import java.util.List;

@JsonTypeName("array")
public class ArrayType implements DataType {

    public static ArrayType of(List<DataType> types) {
        if (types.size() == 0) {
            return new ArrayType(null);
        }

        var first = types.get(0);
        var eq = types.stream().allMatch(d -> d.equals(first));
        return new ArrayType(eq ? first : null);
    }

    private final DataType sharedType;

    public ArrayType(DataType sharedType) {
        this.sharedType = sharedType;
    }

    public boolean isSimple() {
        return hasSharedType() && getSharedType().isValue();
    }

    public boolean hasSharedType() {
        return sharedType != null;
    }

    public DataType getSharedType() {
        return sharedType;
    }

    @Override
    public boolean isTuple() {
        return false;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public boolean isValue() {
        return false;
    }

    @Override
    public void traverseType(DataTypeCallback cb) {
        cb.onArray(this);
    }
}
