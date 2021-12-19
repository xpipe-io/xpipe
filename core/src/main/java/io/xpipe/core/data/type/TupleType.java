package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.node.DataStructureNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A tuple type in the context of XPipe is defined as an ordered,
 * fixed-size sequence of elements that can be optionally assigned a name.
 * This permissive design allows for a very flexible usage of the tuple type.
 */
@JsonTypeName("tuple")
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class TupleType extends DataType {

    List<String> names;
    List<DataType> types;

    /**
     * Creates a new tuple type that contains no entries.
     */
    public static TupleType empty() {
        return new TupleType(List.of(), List.of());
    }

    /**
     * Creates a new tuple type for a list of names and types.
     * Any entry in {@code names} can be null, while any element in {@code types} must be not null.
     */
    @JsonCreator
    public static TupleType of(List<String> names, List<DataType> types) {
        return new TupleType(names, types);
    }

    /**
     * Creates a new tuple type for a list of types with no names.
     * Any element in {@code types} must be not null.
     */
    public static TupleType of(List<DataType> types) {
        return new TupleType(Collections.nCopies(types.size(), null), types);
    }

    @Override
    public String getName() {
        return "tuple";
    }

    @Override
    public boolean matches(DataStructureNode node) {
        if (!node.isTuple()) {
            return false;
        }

        var t = node.asTuple();
        if (t.size() != getSize()) {
            return false;
        }

        int counter = 0;
        for (var kv : t.getKeyValuePairs()) {
            if (!Objects.equals(kv.key(), names.get(counter))) {
                return false;
            }

            if (!types.get(counter).matches(kv.value())) {
                return false;
            }
            counter++;
        }

        return true;
    }

    @Override
    public boolean isTuple() {
        return true;
    }

    @Override
    public void visit(DataTypeVisitor visitor) {
        visitor.onTuple(this);
    }

    public int getSize() {
        return types.size();
    }
}
