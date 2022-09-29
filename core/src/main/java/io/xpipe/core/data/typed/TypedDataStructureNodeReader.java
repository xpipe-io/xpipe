package io.xpipe.core.data.typed;

import io.xpipe.core.data.node.*;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.DataTypeVisitors;
import io.xpipe.core.data.type.TupleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class TypedDataStructureNodeReader implements TypedAbstractReader {

    public static TypedDataStructureNodeReader of(DataType type) {
        return new TypedDataStructureNodeReader(type);
    }

    private DataStructureNode readNode;

    private final Stack<List<DataStructureNode>> children;
    private final Stack<DataStructureNode> nodes;
    private int arrayDepth;

    private final List<DataType> flattened;
    private DataType expectedType;
    private int currentExpectedTypeIndex;

    private TypedDataStructureNodeReader(DataType type) {
        flattened = new ArrayList<>();
        type.visit(DataTypeVisitors.flatten(flattened::add));
        children = new Stack<>();
        nodes = new Stack<>();
        expectedType = flattened.get(0);
    }

    @Override
    public void onNodeBegin() {
        if (nodes.size() != 0 || children.size() != 0) {
            throw new IllegalStateException("Reader did not completely reset");
        }

        readNode = null;
    }

    @Override
    public boolean isDone() {
        return readNode != null;
    }

    public DataStructureNode create() {
        if (readNode == null) {
            throw new IllegalStateException("Reader is not finished yet");
        }

        return readNode;
    }

    @Override
    public void onNodeEnd() {
        if (nodes.size() != 0 || children.size() != 0 || readNode == null) {
            throw new IllegalStateException("Reader is not finished yet");
        }

        expectedType = flattened.get(0);
        currentExpectedTypeIndex = 0;
    }

    private void finishNode(DataStructureNode node) {
        if (nodes.empty()) {
            readNode = node;
        } else {
            children.peek().add(node);
        }
    }

    @Override
    public void onValue(byte[] data, Map<Integer, String> metaAttributes) {
        if (!expectedType.isValue()) {
            throw new IllegalStateException("Expected " + expectedType.getName() + " but got value");
        }

        var val = ValueNode.of(data).tag(metaAttributes);
        finishNode(val);
        moveExpectedType(false);
    }

    private boolean isInArray() {
        return arrayDepth >= 1;
    }

    @Override
    public void onGenericNode(DataStructureNode node) {
        if (!expectedType.isWildcard()) {
            throw new IllegalStateException("Expected " + expectedType.getName() + " but got generic node");
        }

        finishNode(node);
        moveExpectedType(false);
    }

    @Override
    public void onTupleBegin(TupleType type) {
        if (!expectedType.isTuple()) {
            throw new IllegalStateException("Expected " + expectedType.getName() + " but got tuple");
        }

        TupleType tupleType = expectedType.asTuple();
        moveExpectedType(false);

        var l = new ArrayList<DataStructureNode>(tupleType.getSize());
        children.push(l);

        var newNode = new SimpleTupleNode(tupleType.getNames(), l);
        nodes.push(newNode);
    }

    @Override
    public void onTupleEnd(Map<Integer, String> metaAttributes) {
        children.pop();
        var popped = nodes.pop();
        if (!popped.isTuple()) {
            throw new IllegalStateException("No tuple to end");
        }

        TupleNode tuple = popped.tag(metaAttributes).asTuple();
        if (tuple.getKeyNames().size() != tuple.getNodes().size()) {
            throw new IllegalStateException("Tuple node size mismatch");
        }

        finishNode(popped);
    }

    private void moveExpectedType(boolean force) {
        if (!isInArray() || force) {
            currentExpectedTypeIndex++;
            expectedType = currentExpectedTypeIndex == flattened.size() ? null : flattened.get(currentExpectedTypeIndex);
        }
    }

    @Override
    public void onArrayBegin(int size) {
        if (!expectedType.isArray()) {
            throw new IllegalStateException("Expected " + expectedType.getName() + " but got array");
        }

        arrayDepth++;
        moveExpectedType(true);

        var l = new ArrayList<DataStructureNode>();
        children.push(l);

        var newNode = ArrayNode.of(l);
        nodes.push(newNode);
    }

    @Override
    public void onArrayEnd(Map<Integer, String> metaAttributes) {
        if (!isInArray()) {
            throw new IllegalStateException("No array to end");
        }

        arrayDepth--;
        moveExpectedType(true);

        children.pop();
        var popped = nodes.pop().tag(metaAttributes);
        finishNode(popped);
    }
}
