package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.DataType;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ArrayNode extends DataStructureNode {

    private final List<DataStructureNode> valueNodes;

    private ArrayNode(List<DataStructureNode> valueNodes) {
        this.valueNodes = valueNodes;
    }

    public static ArrayNode wrap(List<DataStructureNode> valueNodes) {
        return new ArrayNode(valueNodes);
    }

    public static ArrayNode copy(List<DataStructureNode> valueNodes) {
        return new ArrayNode(new ArrayList<>(valueNodes));
    }

    @Override
    public Stream<DataStructureNode> stream() {
        return Collections.unmodifiableList(valueNodes).stream();
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public int size() {
        return valueNodes.size();
    }

    @Override
    protected String getName() {
        return "array node";
    }

    @Override
    public DataType getDataType() {
        return ArrayType.of(valueNodes.stream().map(DataStructureNode::getDataType).toList());
    }

    @Override
    public DataStructureNode at(int index) {
        return valueNodes.get(index);
    }

    @Override
    public void forEach(Consumer<? super DataStructureNode> action) {
        valueNodes.forEach(action);
    }

    @Override
    public Spliterator<DataStructureNode> spliterator() {
        return valueNodes.spliterator();
    }

    @Override
    public Iterator<DataStructureNode> iterator() {
        return valueNodes.iterator();
    }
}
