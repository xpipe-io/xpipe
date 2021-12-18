package io.xpipe.core.data.node;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NoKeyTupleNode extends TupleNode {

    private final List<DataStructureNode> nodes;

    NoKeyTupleNode(List<DataStructureNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public DataStructureNode set(int index, DataStructureNode node) {
        nodes.set(index, node);
        return this;
    }

    @Override
    public DataType determineDataType() {
        return TupleType.of(null, nodes.stream().map(DataStructureNode::determineDataType).toList());
    }

    @Override
    protected String getName() {
        return "no key tuple node";
    }

    @Override
    public DataStructureNode at(int index) {
        return nodes.get(index);
    }

    @Override
    public DataStructureNode forKey(String name) {
        throw unsupported("key indexing");
    }

    @Override
    public Optional<DataStructureNode> forKeyIfPresent(String name) {
        return Optional.empty();
    }

    @Override
    public int size() {
        return nodes.size();
    }

    public String nameAt(int index) {
        throw unsupported("name getter");
    }

    @Override
    public List<KeyValue> getKeyValuePairs() {
        return nodes.stream().map(n -> new KeyValue(null, n)).toList();
    }

    public List<String> getNames() {
        return Collections.nCopies(size(), null);
    }

    public List<DataStructureNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
}
