package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TupleNode extends DataStructureNode {

    private final List<String> names;
    private final List<DataStructureNode> nodes;

    private TupleNode(List<String> names, List<DataStructureNode> nodes) {
        this.names = names;
        this.nodes = nodes;
    }

    public static TupleNode wrap(List<String> names, List<DataStructureNode> nodes) {
        return new TupleNode(names, nodes);
    }

    public static TupleNode copy(List<String> names, List<DataStructureNode> nodes) {
        return new TupleNode(new ArrayList<>(names), new ArrayList<>(nodes));
    }

    public boolean isTuple() {
        return true;
    }

    @Override
    public DataType getDataType() {
        return TupleType.wrap(names, nodes.stream().map(DataStructureNode::getDataType).toList());
    }

    @Override
    protected String getName() {
        return "tuple node";
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
    public int size() {
        return nodes.size();
    }

    public String nameAt(int index) {
        return names.get(index);
    }

    public List<String> getNames() {
        return Collections.unmodifiableList(names);
    }

    public List<DataStructureNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
}
