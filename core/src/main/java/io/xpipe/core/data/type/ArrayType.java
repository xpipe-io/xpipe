package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.callback.DataTypeCallback;
import lombok.EqualsAndHashCode;

import java.util.List;

@JsonTypeName("array")
@EqualsAndHashCode
public class ArrayType implements DataType {

    public static ArrayType ofWildcard() {
        return new ArrayType(WildcardType.of());
    }

    public static ArrayType of(List<DataType> types) {
        if (types.size() == 0) {
            return new ArrayType(WildcardType.of());
        }

        var first = types.get(0);
        var eq = types.stream().allMatch(d -> d.equals(first));
        return new ArrayType(eq ? first : WildcardType.of());
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
    public String getName() {
        return "array";
    }

    @Override
    public boolean matches(DataStructureNode node) {
        return node.isArray();
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public void traverseType(DataTypeCallback cb) {
        cb.onArray(this);
    }
}
