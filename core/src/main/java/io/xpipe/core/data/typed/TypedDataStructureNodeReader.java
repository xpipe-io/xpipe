package io.xpipe.core.data.typed;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.SimpleTupleNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.callback.DataTypeCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class TypedDataStructureNodeReader implements TypedAbstractReader {

    private final List<DataType> flattened;
    private final Stack<List<DataStructureNode>> children;
    private final Stack<DataStructureNode> nodes;
    private final boolean makeImmutable;
    private int currentDataTypeIndex;
    private DataStructureNode readNode;
    private boolean initialized;
    private int arrayDepth;
    private TypedDataStructureNodeReader(DataType type, boolean makeImmutable) {
        flattened = new ArrayList<>();
        children = new Stack<>();
        nodes = new Stack<>();
        type.traverseType(DataTypeCallback.flatten(d -> flattened.add(d)));
        this.makeImmutable = makeImmutable;
    }

    public static TypedDataStructureNodeReader mutable(DataType type) {
        return new TypedDataStructureNodeReader(type, false);
    }

    public static TypedDataStructureNodeReader immutable(DataType type) {
        return new TypedDataStructureNodeReader(type, true);
    }

    @Override
    public void onNodeBegin() {
        if (nodes.size() != 0 || children.size() != 0) {
            throw new IllegalStateException();
        }

        readNode = null;
    }

    @Override
    public boolean isDone() {
        return readNode != null;
    }

    public DataStructureNode create() {
        return readNode;
    }

    @Override
    public void onNodeEnd() {
        if (nodes.size() != 0 || children.size() != 0 || readNode == null) {
            throw new IllegalStateException();
        }

        initialized = false;
    }

    @Override
    public void onValue(byte[] data) {
        var val = makeImmutable ? ValueNode.immutable(data) : ValueNode.mutable(data);
        if (!initialized) {
            readNode = val;
            return;
        }

        children.peek().add(val);
        if (!flattened.get(currentDataTypeIndex).isArray()) {
            currentDataTypeIndex++;
        }
    }

    private boolean isInArray() {
        return arrayDepth >= 1;
    }

    @Override
    public void onGenericNode(DataStructureNode node) {
        children.peek().add(node);
        if (!isInArray()) {
            currentDataTypeIndex++;
        }
    }

    @Override
    public void onTupleBegin(TupleType type) {
        if (!isInArray() && !flattened.get(currentDataTypeIndex).isTuple()) {
            throw new IllegalStateException();
        }

        TupleType tupleType = (TupleType) flattened.get(currentDataTypeIndex);
        if (!initialized || !flattened.get(currentDataTypeIndex).isArray()) {
            currentDataTypeIndex++;
        }
        if (!initialized) {
            initialized = true;
        }

        var l = new ArrayList<DataStructureNode>(tupleType.getSize());
        children.push(l);

        var tupleNames = makeImmutable ?
                Collections.unmodifiableList(tupleType.getNames()) : new ArrayList<>(tupleType.getNames());
        var tupleNodes = makeImmutable ? Collections.unmodifiableList(l) : l;
        var newNode = TupleNode.wrapRaw(tupleNames, tupleNodes);
        nodes.push(newNode);
    }

    @Override
    public void onTupleEnd() {
        children.pop();
        var popped = nodes.pop();
        if (!popped.isTuple()) {
            throw new IllegalStateException();
        }

        SimpleTupleNode tuple = (SimpleTupleNode) popped;
        if (tuple.getNames().size() != tuple.getNodes().size()) {
            throw new IllegalStateException("");
        }

        if (nodes.empty()) {
            readNode = popped;
        } else {
            children.peek().add(popped);
        }
    }

    @Override
    public void onArrayBegin(int size) throws IOException {
        if (!flattened.get(currentDataTypeIndex).isArray()) {
            throw new IllegalStateException();
        }

        var l = new ArrayList<DataStructureNode>();
        children.push(l);

        var arrayNodes = makeImmutable ? Collections.unmodifiableList(l) : l;
        var newNode = ArrayNode.of(arrayNodes);
        nodes.push(newNode);
        arrayDepth++;
    }

    @Override
    public void onArrayEnd() {
        arrayDepth--;
        if (!isInArray()) {
            currentDataTypeIndex++;
        }

        children.pop();
        var popped = nodes.pop();
        if (nodes.empty()) {
            readNode = popped;
        } else {
            children.peek().add(popped);
        }
    }
}
