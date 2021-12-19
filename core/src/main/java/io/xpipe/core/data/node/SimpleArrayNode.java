package io.xpipe.core.data.node;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleArrayNode extends ArrayNode {

    private final boolean mutable;
    private final List<DataStructureNode> nodes;

    SimpleArrayNode(boolean mutable, List<DataStructureNode> nodes) {
        this.nodes = nodes;
        this.mutable = mutable;
    }

    private void checkMutable() {
        if (!mutable) {
            throw new UnsupportedOperationException("Array node is immutable");
        }
    }

    @Override
    public DataStructureNode put(DataStructureNode node) {
        checkMutable();

        nodes.add(node);
        return this;
    }

    @Override
    public DataStructureNode set(int index, DataStructureNode node) {
        checkMutable();

        nodes.add(index, node);
        return this;
    }

    @Override
    public Stream<DataStructureNode> stream() {
        return nodes.stream();
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public ArrayNode mutableCopy() {
        return new SimpleArrayNode(true, nodes.stream()
                .map(DataStructureNode::mutableCopy)
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    protected String getIdentifier() {
        return "S";
    }

    @Override
    public boolean isMutable() {
        return mutable;
    }

    @Override
    public ArrayNode immutableView() {
        return new SimpleArrayNode(false, nodes.stream()
                .map(DataStructureNode::immutableView)
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public DataStructureNode clear() {
        checkMutable();

        nodes.clear();
        return this;
    }

    @Override
    public DataStructureNode at(int index) {
        return nodes.get(index);
    }

    @Override
    public void forEach(Consumer<? super DataStructureNode> action) {
        nodes.forEach(action);
    }

    @Override
    public Spliterator<DataStructureNode> spliterator() {
        return nodes.spliterator();
    }

    @Override
    public Iterator<DataStructureNode> iterator() {
        return nodes.iterator();
    }

    @Override
    public List<DataStructureNode> getNodes() {
        return nodes;
    }

    @Override
    public DataStructureNode remove(int index) {
        checkMutable();

        nodes.remove(index);
        return this;
    }
}
