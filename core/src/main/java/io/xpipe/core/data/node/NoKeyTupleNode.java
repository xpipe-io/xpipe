package io.xpipe.core.data.node;

import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NoKeyTupleNode extends TupleNode {

    private final boolean mutable;
    private final List<DataStructureNode> nodes;

    NoKeyTupleNode(boolean mutable, List<DataStructureNode> nodes) {
        this.mutable = mutable;
        this.nodes = mutable ? nodes : Collections.unmodifiableList(nodes);
    }

    @Override
    public TupleNode mutableCopy() {
        return new NoKeyTupleNode(true, nodes.stream()
                .map(DataStructureNode::mutableCopy)
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public TupleNode immutableView() {
        return new NoKeyTupleNode(false, nodes.stream()
                .map(DataStructureNode::immutableView)
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public DataStructureNode set(int index, DataStructureNode node) {
        checkMutable();

        nodes.set(index, node);
        return this;
    }

    @Override
    public DataType determineDataType() {
        return TupleType.of(nodes.stream().map(DataStructureNode::determineDataType).toList());
    }

    @Override
    protected String getName() {
        return "no key tuple node";
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
    public int size() {
        return nodes.size();
    }

    @Override
    public List<KeyValue> getKeyValuePairs() {
        return nodes.stream().map(n -> new KeyValue(null, n)).toList();
    }

    @Override
    public List<String> getKeyNames() {
        return Collections.nCopies(size(), null);
    }

    public List<DataStructureNode> getNodes() {
        return nodes;
    }

    @Override
    protected String getIdentifier() {
        return "NK";
    }
}
