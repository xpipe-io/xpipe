package io.xpipe.core.data.generic;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.SimpleTupleNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;

import java.util.ArrayList;
import java.util.List;

public class GenericTupleReader implements GenericAbstractReader {

    private boolean initialized;
    private int length;
    private List<String> names;
    private List<DataStructureNode> nodes;
    private int currentIndex = 0;
    private GenericAbstractReader currentReader;
    private DataStructureNode created;
    public GenericTupleReader() {
    }

    public static GenericTupleReader newReader(int length) {
        var tr = new GenericTupleReader();
        tr.onTupleStart(length);
        return tr;
    }

    private boolean hasReader() {
        return currentReader != null;
    }

    private void init(int length) {
        this.length = length;
        this.names = new ArrayList<>(length);
        this.nodes = new ArrayList<>(length);
        initialized = true;
    }

    private boolean isInitialized() {
        return initialized;
    }

    private void putNode(DataStructureNode node) {
        // If no key was read, assume null key
        if (this.names.size() == this.nodes.size()) {
            this.names.add(null);
        }

        this.nodes.add(node);
        currentIndex++;
    }

    private boolean filled() {
        return currentIndex == length;
    }

    @Override
    public void onName(String name) {
        if (hasReader()) {
            currentReader.onName(name);
            return;
        }

        if (filled()) {
            throw new IllegalStateException("Tuple is full but got another name");
        }

        if (names.size() - nodes.size() == 1) {
            throw new IllegalStateException("Tuple is waiting for a node but got another name");
        }

        names.add(name);
    }

    @Override
    public void onArrayStart(int length) {
        if (hasReader()) {
            currentReader.onArrayStart(length);
            return;
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Expected tuple start but got array start");
        }

        currentReader = GenericArrayReader.newReader(length);
    }

    @Override
    public void onArrayEnd() {
        if (hasReader()) {
            currentReader.onArrayEnd();
            if (currentReader.isDone()) {
                putNode(currentReader.create());
                currentReader = null;
            }
            return;
        }

        throw new IllegalStateException("Expected tuple end but got array end");
    }

    @Override
    public void onTupleStart(int length) {
        if (hasReader()) {
            currentReader.onTupleStart(length);
            return;
        }

        if (!isInitialized()) {
            init(length);
            return;
        }

        currentReader = GenericTupleReader.newReader(length);
    }

    @Override
    public void onTupleEnd() {
        if (hasReader()) {
            currentReader.onTupleEnd();
            if (currentReader.isDone()) {
                putNode(currentReader.create());
                currentReader = null;
            }
            return;
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Expected tuple start but got tuple end");
        }

        if (!filled()) {
            throw new IllegalStateException("Tuple ended but is not full yet");
        }

        created = TupleNode.of(names, nodes);
    }

    @Override
    public void onValue(byte[] value, boolean textual) {
        if (currentReader != null) {
            currentReader.onValue(value, textual);
            return;
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Expected tuple start but got value");
        }

        if (filled()) {
            throw new IllegalStateException("Tuple is full but got another value");
        }

        putNode(ValueNode.mutable(value, textual));
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

        return SimpleTupleNode.of(names, nodes);
    }
}
