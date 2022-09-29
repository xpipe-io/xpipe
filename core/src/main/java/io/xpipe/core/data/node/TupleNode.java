package io.xpipe.core.data.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public abstract class TupleNode extends DataStructureNode {

    public static Builder builder() {
        return new Builder();
    }

    public static TupleNode of(List<DataStructureNode> nodes) {
        if (nodes == null) {
            throw new IllegalArgumentException("Nodes must be not null");
        }

        return new SimpleTupleNode(null, nodes);
    }

    @SuppressWarnings("unchecked")
    public static TupleNode of(List<String> names, List<? extends DataStructureNode> nodes) {
        if (names == null) {
            throw new IllegalArgumentException("Names must be not null");
        }
        if (nodes == null) {
            throw new IllegalArgumentException("Nodes must be not null");
        }
        if (names.size() != nodes.size()) {
            throw new IllegalArgumentException("Names and nodes must have the same length");
        }

        return new SimpleTupleNode(names, (List<DataStructureNode>) nodes);
    }

    @Override
    public abstract TupleNode mutable();

    public final boolean isTuple() {
        return true;
    }

    @Override
    public String toString(int indent) {
        var is = " ".repeat(indent);
        var start = "{\n";
        var kvs = getKeyValuePairs().stream().map(kv -> {
            if (kv.key() == null) {
                return is + " " + kv.value().toString(indent + 1) + "\n";
            } else {
                return is + " " + kv.key() + "=" + kv.value().toString(indent + 1) + "\n";
            }
        }).collect(Collectors.joining());
        var end = is + "}";
        return start + kvs + end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TupleNode that)) return false;
        var toReturn =  getKeyNames().equals(that.getKeyNames()) && getNodes().equals(that.getNodes()) && Objects.equals(getMetaAttributes(), that.getMetaAttributes());
        if (toReturn == false) {
            throw new AssertionError();
        }
        return toReturn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyNames(), getNodes(), getMetaAttributes());
    }

    public static class Builder {

        private final List<KeyValue> entries = new ArrayList<>();

        public Builder add(String name, DataStructureNode node) {
            Objects.requireNonNull(node);
            entries.add(new KeyValue(name, node));
            return this;
        }

        public Builder add(DataStructureNode node) {
            Objects.requireNonNull(node);
            entries.add(new KeyValue(null, node));
            return this;
        }

        public TupleNode build() {
            boolean hasKeys = entries.stream().anyMatch(kv -> kv.key() != null);
            return hasKeys ? TupleNode.of(
                    entries.stream().map(KeyValue::key).toList(),
                    entries.stream().map(KeyValue::value).toList()) :
                    TupleNode.of(entries.stream().map(KeyValue::value).toList());
        }
    }
}
