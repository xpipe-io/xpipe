package io.xpipe.core.data.node;

import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleTupleNode extends TupleNode {

    private final boolean mutable;
    private final List<String> names;
    private final List<DataStructureNode> nodes;

    SimpleTupleNode(boolean mutable, List<String> names, List<DataStructureNode> nodes) {
        this.mutable = mutable;
        this.names = mutable ? names : Collections.unmodifiableList(names);
        this.nodes = mutable ? nodes : Collections.unmodifiableList(nodes);
    }

    @Override
    public TupleNode mutableCopy() {
        var nodesCopy = nodes.stream()
                .map(DataStructureNode::mutableCopy)
                .collect(Collectors.toCollection(ArrayList::new));
        return new SimpleTupleNode(true, new ArrayList<>(names), nodesCopy);
    }

    @Override
    public TupleNode immutableView() {
        var nodesCopy = nodes.stream()
                .map(DataStructureNode::immutableView)
                .collect(Collectors.toCollection(ArrayList::new));
        return new SimpleTupleNode(false, names, nodesCopy);
    }

    @Override
    public DataStructureNode set(int index, DataStructureNode node) {
        checkMutable();

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
        return mutable;
    }

    @Override
    public DataStructureNode at(int index) {
        return nodes.get(index);
    }

    @Override
    public DataStructureNode forKey(String name) {
        return nodes.get(names.indexOf(name));
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
        checkMutable();

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
    protected String getIdentifier() {
        return "S";
    }
}
