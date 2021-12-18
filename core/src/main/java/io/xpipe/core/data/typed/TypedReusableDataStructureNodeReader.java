package io.xpipe.core.data.typed;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.callback.DataTypeCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TypedReusableDataStructureNodeReader implements TypedAbstractReader {

    private final List<DataType> flattened;
    private final TypedDataStructureNodeReader initialReader;
    private DataStructureNode node;
    private final Stack<Integer> indices;
    private int arrayDepth;

    public TypedReusableDataStructureNodeReader(DataType type) {
        flattened = new ArrayList<>();
        indices = new Stack<>();
        initialReader = TypedDataStructureNodeReader.mutable(type);
        type.traverseType(DataTypeCallback.flatten(d -> flattened.add(d)));
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

        getCurrentParent().set(indices.peek(), node);
        if (!indices.isEmpty()) {
            indices.push(indices.pop() + 1);
        }
    }

    private DataStructureNode getCurrentParent() {
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

    private void setValue(byte[] data) {
        var current = node;
        for (var index : indices) {
            current = current.at(index);
        }
        var value = (ValueNode) current;
        value.setRawData(data);
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
    public void onArrayBegin(int size) throws IOException {
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
            return;
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
