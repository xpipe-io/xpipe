package io.xpipe.core.data.type.callback;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.generic.ArrayNode;
import io.xpipe.core.data.generic.TupleNode;
import io.xpipe.core.data.generic.ValueNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public class TypedDataStructureNodeCallback implements TypedDataStreamCallback {

    private final List<DataType> flattened;
    private int dataTypeIndex;
    private Stack<List<DataStructureNode>> children;
    private Stack<DataStructureNode> nodes;
    private DataStructureNode readNode;
    private final Consumer<DataStructureNode> consumer;

    public TypedDataStructureNodeCallback(DataType type, Consumer<DataStructureNode> consumer) {
        this.consumer = consumer;
        flattened = new ArrayList<>();
        children = new Stack<>();
        nodes = new Stack<>();
        type.traverseType(DataTypeCallback.flatten(d -> flattened.add(d)));
    }

    @Override
    public void onNodeBegin() {
        if (nodes.size() != 0 || children.size() != 0) {
            throw new IllegalStateException();
        }

        dataTypeIndex = 0;
        readNode = null;
    }

    @Override
    public void onNodeEnd() {
        if (nodes.size() != 0 || children.size() != 0 || readNode == null) {
            throw new IllegalStateException();
        }

        consumer.accept(readNode);
    }

    @Override
    public void onValue(byte[] data) {
        children.peek().add(ValueNode.wrap(data));
        if (!flattened.get(dataTypeIndex).isArray()) {
            dataTypeIndex++;
        }
    }

    protected void newTuple() {
        TupleType tupleType = (TupleType) flattened.get(dataTypeIndex);
        var l = new ArrayList<DataStructureNode>(tupleType.getSize());
        children.push(l);
        var newNode = TupleNode.wrap(tupleType.getNames(), l);
        nodes.push(newNode);
    }

    protected void newArray() {
        var l = new ArrayList<DataStructureNode>();
        children.push(new ArrayList<>());
        var newNode = ArrayNode.wrap(l);
        nodes.push(newNode);
    }

    private void finishTuple() {
        children.pop();
        dataTypeIndex++;
        var popped = nodes.pop();
        if (!popped.isTuple()) {
            throw new IllegalStateException();
        }

        TupleNode tuple = (TupleNode) popped;
        if (tuple.getNames().size() != tuple.getNodes().size()) {
            throw new IllegalStateException("");
        }

        if (nodes.empty()) {
            readNode = popped;
        } else {
            children.peek().add(popped);
        }
    }

    private void finishArray() {
        children.pop();
        dataTypeIndex++;
        var popped = nodes.pop();
        if (nodes.empty()) {
            readNode = popped;
        } else {
            children.peek().add(popped);
        }
    }

    @Override
    public void onTupleBegin(int size) {
        if (!flattened.get(dataTypeIndex).isTuple()) {
            throw new IllegalStateException();
        }

        newTuple();
    }

    @Override
    public void onTupleEnd() {
        finishTuple();
    }

    @Override
    public void onArrayBegin(int size) {
        if (!flattened.get(dataTypeIndex).isArray()) {
            throw new IllegalStateException();
        }

        newArray();
    }

    @Override
    public void onArrayEnd() {
        finishArray();
    }
}
