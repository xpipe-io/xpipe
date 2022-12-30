package io.xpipe.core.data.generic;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.ValueNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenericArrayReader implements GenericAbstractReader {

    private boolean initialized;
    private List<DataStructureNode> nodes;
    private int length;
    private int currentIndex = 0;
    private GenericAbstractReader currentReader;
    private DataStructureNode created;

    public GenericArrayReader() {
    }

    public static GenericArrayReader newReader(int length) {
        var ar = new GenericArrayReader();
        ar.onArrayStart(length);
        return ar;
    }

    private void init(int length) {
        this.length = length;
        this.nodes = new ArrayList<>(length);
        initialized = true;
    }

    private void put(DataStructureNode node) {
        this.nodes.add(node);
        currentIndex++;
    }

    @Override
    public void onName(String name) {
        if (hasReader()) {
            currentReader.onName(name);
            return;
        }

        throw new IllegalStateException("Expected array content but got a key name");
    }

    private boolean filled() {
        return currentIndex == length;
    }

    private boolean isInitialized() {
        return initialized;
    }

    private boolean hasReader() {
        return currentReader != null;
    }

    @Override
    public void onArrayStart(int length) {
        if (hasReader()) {
            currentReader.onArrayStart(length);
            return;
        }

        if (!isInitialized()) {
            init(length);
            return;
        }

        currentReader = newReader(length);
    }

    @Override
    public void onArrayEnd(Map<Integer, String> metaAttributes) {
        if (hasReader()) {
            currentReader.onArrayEnd(metaAttributes);
            if (currentReader.isDone()) {
                put(currentReader.create());
                currentReader = null;
            }
            return;
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Expected array start but got array end");
        }

        if (!filled()) {
            throw new IllegalStateException("Array ended but is not full yet");
        }

        created = ArrayNode.of(nodes);
    }

    @Override
    public void onTupleStart(int length) {
        if (hasReader()) {
            currentReader.onTupleStart(length);
            return;
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Expected array start but got tuple start");
        }

        if (filled()) {
            throw new IllegalStateException("Tuple is full but got another tuple");
        }

        currentReader = GenericTupleReader.newReader(length);
    }

    @Override
    public void onTupleEnd(Map<Integer, String> metaAttributes) {
        if (hasReader()) {
            currentReader.onTupleEnd(metaAttributes);
            if (currentReader.isDone()) {
                put(currentReader.create());
                currentReader = null;
            }
            return;
        }

        throw new IllegalStateException("Expected array end but got tuple end");
    }

    @Override
    public void onValue(byte[] value, Map<Integer, String> metaAttributes) {
        if (currentReader != null) {
            currentReader.onValue(value, metaAttributes);
            return;
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Expected array start but got value");
        }

        if (filled()) {
            throw new IllegalStateException("Array is full but got another value");
        }

        put(ValueNode.of(value).tag(metaAttributes));
    }

    @Override
    public boolean isDone() {
        return filled() && created != null;
    }

    @Override
    public DataStructureNode create() {
        if (!isDone()) {
            throw new IllegalStateException();
        }

        return ArrayNode.of(nodes);
    }
}
