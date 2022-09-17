package io.xpipe.core.data.node;

import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

@AllArgsConstructor


public class SimpleArrayNode extends ArrayNode {

    List<DataStructureNode> nodes;

    @Override
    public DataStructureNode put(DataStructureNode node) {
        nodes.add(node);
        return this;
    }

    @Override
    public DataStructureNode set(int index, DataStructureNode node) {
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
    public boolean isMutable() {
        return true;
    }

    @Override
    public DataStructureNode clear() {
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
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public DataStructureNode remove(int index) {
        nodes.remove(index);
        return this;
    }


}
