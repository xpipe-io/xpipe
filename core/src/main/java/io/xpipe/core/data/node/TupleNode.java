package io.xpipe.core.data.node;

import io.xpipe.core.data.DataStructureNode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class TupleNode extends DataStructureNode {

    public static Builder builder() {
        return new Builder();
    }

    public static TupleNode wrap(List<DataStructureNode> nodes) {
        if (nodes == null) {
            throw new IllegalArgumentException("Nodes must be not null");
        }

        return new NoKeyTupleNode(nodes);
    }

    public static TupleNode copy(List<DataStructureNode> nodes) {
        return TupleNode.wrap(List.copyOf(nodes));
    }

    public static TupleNode wrap(List<String> names, List<DataStructureNode> nodes) {
        if (names == null) {
            throw new IllegalArgumentException("Names must be not null");
        }
        if (nodes == null) {
            throw new IllegalArgumentException("Nodes must be not null");
        }
        if (names.size() != nodes.size()) {
            throw new IllegalArgumentException("Names and nodes must have the same length");
        }

        return new SimpleTupleNode(names, nodes);
    }

    public static TupleNode wrapRaw(List<String> names, List<DataStructureNode> nodes) {
        if (names == null) {
            throw new IllegalArgumentException("Names must be not null");
        }
        if (nodes == null) {
            throw new IllegalArgumentException("Nodes must be not null");
        }
        return new SimpleTupleNode(names, nodes);
    }

    public static TupleNode copy(List<String> names, List<DataStructureNode> nodes) {
        return TupleNode.wrap(List.copyOf(names), List.copyOf(nodes));
    }

    public final boolean isTuple() {
        return true;
    }

    @Override
    public String toString(int indent) {
        var is = " ".repeat(indent);
        var start = getClass().getSimpleName() + " {\n";
        var kvs = getKeyValuePairs().stream().map(kv -> {
            if (kv.key == null) {
                return is + " " + kv.value.toString(indent + 1) + "\n";
            } else {
                return is + " " + kv.key + "=" + kv.value.toString(indent + 1) + "\n";
            }
        }).collect(Collectors.joining());
        var end = is + "}";
        return start + kvs + end;
    }

    public abstract String nameAt(int index);

    public abstract List<KeyValue> getKeyValuePairs();

    public abstract List<String> getNames();

    public abstract List<DataStructureNode> getNodes();

    @Value
    public static class KeyValue {

        String key;
        DataStructureNode value;
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
            boolean hasKeys = entries.stream().anyMatch(kv -> kv.key != null);
            return hasKeys ? TupleNode.wrap(
                    entries.stream().map(kv -> kv.key).toList(),
                    entries.stream().map(kv -> kv.value).toList()) :
                    TupleNode.wrap(entries.stream().map(kv -> kv.value).toList());
        }
    }
}
