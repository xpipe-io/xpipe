package io.xpipe.core.data.typed;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.DataTypeVisitors;
import io.xpipe.core.data.type.TupleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TypedReusableDataStructureNodeReader implements TypedAbstractReader {

    public static TypedReusableDataStructureNodeReader create(DataType type) {
        return new TypedReusableDataStructureNodeReader(type);
    }

    private final List<DataType> flattened;
    private final TypedDataStructureNodeReader initialReader;
    private DataStructureNode node;
    private final Stack<Integer> indices;
    private int arrayDepth;

    private TypedReusableDataStructureNodeReader(DataType type) {
        flattened = new ArrayList<>();
        indices = new Stack<>();
        initialReader = TypedDataStructureNodeReader.mutable(type);
        type.visit(DataTypeVisitors.flatten(d -> flattened.add(d)));
    }

    @Override
    public boolean isDone() {
        return true;
    }

    public DataStructureNode create() {
        return node;
    }

    private boolean isInArray() {
        return arrayDepth >= 1;
    }

    private boolean initialized() {
        return node != null;
    }

    @Override
    public void onValue(byte[] data) {
        if (!initialized()) {
            initialReader.onValue(data);
            return;
        }

        if (isInArray()) {
            getCurrentParent().set(indices.peek(), ValueNode.mutable(data));
        } else {
            getCurrent().setRawData(data);
        }

        if (!indices.isEmpty()) {
            indices.push(indices.pop() + 1);
        }
    }

    @Override
    public void onGenericNode(DataStructureNode node) {
        if (!initialized()) {
            initialReader.onGenericNode(node);
            return;
        }

        if (hasParent()) {
            getCurrentParent().set(indices.peek(), node);
        } else {
            this.node = node;
        }
        if (!indices.isEmpty()) {
            indices.push(indices.pop() + 1);
        }
    }

    private boolean hasParent() {
        return indices.size() > 0;
    }

    private DataStructureNode getCurrentParent() {
        if (!hasParent()) {
            throw new IllegalStateException("No parent available");
        }

        var current = node;
        for (var index : indices.subList(0, indices.size() - 1)) {
            current = current.at(index);
        }
        return current;
    }

    private DataStructureNode getCurrent() {
        var current = node;
        for (var index : indices) {
            current = current.at(index);
        }
        return current;
    }

    @Override
    public void onTupleBegin(TupleType type) {
        if (!initialized()) {
            initialReader.onTupleBegin(type);
            return;
        }

        indices.push(0);
    }

    @Override
    public void onTupleEnd() {
        if (!initialized()) {
            initialReader.onTupleEnd();
            return;
        }

        indices.pop();
        if (!indices.isEmpty()) {
            indices.push(indices.pop() + 1);
        }
    }

    @Override
    public void onArrayBegin(int size) {
        if (!initialized()) {
            initialReader.onArrayBegin(size);
            return;
        }

        getCurrent().clear();
        indices.push(0);
        arrayDepth++;
    }

    @Override
    public void onArrayEnd() {
        if (!initialized()) {
            initialReader.onArrayEnd();
            return;
        }

        indices.pop();
        arrayDepth--;
        if (!indices.isEmpty()) {
            indices.push(indices.pop() + 1);
        }
    }

    @Override
    public void onNodeBegin() {
        if (!initialized()) {
            initialReader.onNodeBegin();
        }
    }

    @Override
    public void onNodeEnd() {
        if (!initialized()) {
            initialReader.onNodeEnd();
            node = initialReader.create();
        }
    }
}
