package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.*;

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
     * Creates a new tuple type that represents a table data type.
     */
    public static TupleType tableType(List<String> names) {
        return TupleType.of(names, Collections.nCopies(names.size(), WildcardType.of()));
    }

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

    public boolean hasAllNames() {
        return names.stream().allMatch(Objects::nonNull);
    }

    public TupleType sub(List<String> subNames) {
        if (!hasAllNames()) {
            throw new UnsupportedOperationException();
        }

        return new TupleType(subNames, subNames.stream().map(s -> types.get(getNames().indexOf(s))).toList());
    }

    @Override
    public String getName() {
        return "tuple";
    }

    @Override
    public Optional<DataStructureNode> convert(DataStructureNode node) {
        if (matches(node)) {
            return Optional.of(node);
        }

        if (node.isValue() && types.size() == 1) {
            return types.get(0).convert(node);
        }

        if (node.size() != types.size()) {
            return Optional.empty();
        }

        List<DataStructureNode> nodes = new ArrayList<>(node.size());
        for (int i = 0; i < node.size(); i++) {
            var converted = types.get(i).convert(node.at(i));
            if (converted.isEmpty()) {
                return Optional.empty();
            }

            nodes.add(converted.get());
        }

        return Optional.of(TupleNode.of(names, nodes));
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
