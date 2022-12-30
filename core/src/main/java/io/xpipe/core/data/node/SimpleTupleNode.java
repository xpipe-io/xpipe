package io.xpipe.core.data.node;

import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SimpleTupleNode extends TupleNode {

    private final List<String> names;
    private final List<DataStructureNode> nodes;

    public SimpleTupleNode(List<String> names, List<DataStructureNode> nodes) {
        this.names = names;
        this.nodes = nodes;
    }

    @Override
    public DataStructureNode set(int index, DataStructureNode node) {
        nodes.set(index, node);
        return this;
    }

    @Override
    public DataType determineDataType() {
        var subtypes = nodes.stream().map(DataStructureNode::determineDataType).toList();
        return names != null ? TupleType.of(names, subtypes) : TupleType.of(subtypes);
    }

    @Override
    protected String getName() {
        return "tuple node";
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public DataStructureNode at(int index) {
        return nodes.get(index);
    }

    @Override
    public DataStructureNode forKey(String name) {
        var index = names != null ? names.indexOf(name) : -1;
        if (index == -1) {
            throw new IllegalArgumentException("Key " + name + " not found");
        }

        return nodes.get(index);
    }

    @Override
    public Optional<DataStructureNode> forKeyIfPresent(String name) {
        if (names == null || !names.contains(name)) {
            return Optional.empty();
        }

        return Optional.of(nodes.get(names.indexOf(name)));
    }

    @Override
    public DataStructureNode clear() {
        nodes.clear();
        if (names != null) {
            names.clear();
        }
        return this;
    }

    @Override
    public int size() {
        return nodes.size();
    }

    public String keyNameAt(int index) {
        if (names == null) {
            return null;
        }

        return names.get(index);
    }

    @Override
    public List<KeyValue> getKeyValuePairs() {
        var l = new ArrayList<KeyValue>(size());
        for (int i = 0; i < size(); i++) {
            l.add(new KeyValue(
                    names != null ? getKeyNames().get(i) : null, getNodes().get(i)));
        }
        return l;
    }

    public List<String> getKeyNames() {
        return names != null ? Collections.unmodifiableList(names) : Collections.nCopies(size(), null);
    }

    public List<DataStructureNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public TupleNode mutable() {
        return this;
    }
}
