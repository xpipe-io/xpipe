package io.xpipe.core.data.node;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.DataType;
import lombok.EqualsAndHashCode;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = false)
public class ArrayNode extends DataStructureNode {

    private final List<DataStructureNode> valueNodes;

    private ArrayNode(List<DataStructureNode> valueNodes) {
        this.valueNodes = valueNodes;
    }

    public static ArrayNode of(DataStructureNode... dsn) {
        return wrap(List.of(dsn));
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
    public String toString(int indent) {
        var content = valueNodes.stream().map(n -> n.toString(indent)).collect(Collectors.joining(", "));
        return "[" + content + "]";
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
