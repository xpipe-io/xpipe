package io.xpipe.core.data.node;

import io.xpipe.core.data.type.ArrayType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class ArrayNode extends DataStructureNode {

    public static ArrayNode of(DataStructureNode... dsn) {
        return of(List.of(dsn));
    }

    public static ArrayNode of(List<DataStructureNode> nodes) {
        return new SimpleArrayNode(true, nodes);
    }

    public static ArrayNode of(boolean mutable, List<DataStructureNode> nodes) {
        return new SimpleArrayNode(mutable, nodes);
    }

    protected ArrayNode() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArrayNode that)) return false;
        return getNodes().equals(that.getNodes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNodes());
    }

    @Override
    public final boolean isArray() {
        return true;
    }

    @Override
    protected final String getName() {
        return "array node";
    }

    @Override
    public abstract ArrayNode immutableView();

    @Override
    public abstract ArrayNode mutableCopy();

    protected abstract String getIdentifier();

    @Override
    public final String toString(int indent) {
        var content = getNodes().stream().map(n -> n.toString(indent)).collect(Collectors.joining(", "));
        return "(" + getIdentifier() + ") [" + content + "]";
    }

    @Override
    public final ArrayType determineDataType() {
        return ArrayType.ofSharedType(getNodes().stream().map(DataStructureNode::determineDataType).toList());
    }
}
