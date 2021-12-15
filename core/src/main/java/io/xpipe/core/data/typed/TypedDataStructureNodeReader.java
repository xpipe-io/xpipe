package io.xpipe.core.data.typed;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.SimpleTupleNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.callback.DataTypeCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TypedDataStructureNodeReader implements TypedDataStreamCallback {

    private int currentDataTypeIndex;
    private final List<DataType> flattened;
    private Stack<List<DataStructureNode>> children;
    private Stack<DataStructureNode> nodes;
    private DataStructureNode readNode;
    private boolean initialized;
    private int arrayDepth;

    public TypedDataStructureNodeReader(DataType type) {
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

        readNode = null;
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
        if (!initialized) {
            readNode = ValueNode.wrap(data);
            return;
        }

        children.peek().add(ValueNode.wrap(data));
        if (!flattened.get(currentDataTypeIndex).isArray()) {
            currentDataTypeIndex++;
        }
    }

    private void finishTuple() {
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

    private boolean isInArray() {
        return arrayDepth >= 1;
    }

    private void finishArray() {
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

    @Override
    public void onTupleBegin(int size) {
        if (flattened.size() == currentDataTypeIndex) {
            int a = 0;
        }

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
        var newNode = TupleNode.wrapRaw(tupleType.getNames(), l);
        nodes.push(newNode);
    }

    @Override
    public void onTupleEnd() {
        finishTuple();
    }

    @Override
    public void onArrayBegin(int size) {
        if (!flattened.get(currentDataTypeIndex).isArray()) {
            throw new IllegalStateException();
        }

        var l = new ArrayList<DataStructureNode>();
        children.push(l);
        var newNode = ArrayNode.wrap(l);
        nodes.push(newNode);
        arrayDepth++;
    }

    @Override
    public void onArrayEnd() {
        finishArray();
    }
}
