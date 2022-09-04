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
        return TupleType.of(names, nodes.stream().map(DataStructureNode::determineDataType).toList());
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
        var index = names.indexOf(name);
        if (index == -1) {
            throw new IllegalArgumentException("Key " + name + " not found");
        }

        return nodes.get(index);
    }

    @Override
    public Optional<DataStructureNode> forKeyIfPresent(String name) {
        if (!names.contains(name)) {
            return Optional.empty();
        }

        return Optional.of(nodes.get(names.indexOf(name)));
    }

    @Override
    public DataStructureNode clear() {
        nodes.clear();
        names.clear();
        return this;
    }


    @Override
    public int size() {
        return nodes.size();
    }

    public String keyNameAt(int index) {
        return names.get(index);
    }

    @Override
    public List<KeyValue> getKeyValuePairs() {
        var l = new ArrayList<KeyValue>(size());
        for (int i = 0; i < size(); i++) {
            l.add(new KeyValue(getKeyNames().get(i), getNodes().get(i)));
        }
        return l;
    }

    public List<String> getKeyNames() {
        return Collections.unmodifiableList(names);
    }

    public List<DataStructureNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public TupleNode mutable() {
        return this;
    }
}
