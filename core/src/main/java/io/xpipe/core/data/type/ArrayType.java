package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.node.DataStructureNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

/**
 * An array type represents an array of {@link DataStructureNode} of a certain shared type.
 * The shared type should be the most specific data type possible.
 */
@JsonTypeName("array")
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class ArrayType extends DataType {

    DataType sharedType;

    /**
     * Creates a new array type for a given shared data type.
     */
    public static ArrayType of(DataType type) {
        return new ArrayType(type);
    }

    /**
     * Creates a new array type using either the shared type of {@code types}
     * or a wildcard type if the elements do not share a common type.
     */
    public static ArrayType ofSharedType(List<DataType> types) {
        if (types.size() == 0) {
            return new ArrayType(WildcardType.of());
        }

        var first = types.get(0);
        var eq = types.stream().allMatch(d -> d.equals(first));
        return new ArrayType(eq ? first : WildcardType.of());
    }

    @Override
    public String getName() {
        return "array";
    }

    @Override
    public boolean matches(DataStructureNode node) {
        if (!node.isArray()) {
            return false;
        }

        var a = node.asArray();
        for (var n : a) {
            if (!sharedType.matches(n)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public void visit(DataTypeVisitor visitor) {
        visitor.onArray(this);
    }
}
