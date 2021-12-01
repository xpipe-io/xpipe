package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;

import java.util.ArrayList;
import java.util.List;

public class TupleReader implements DataStructureNodeReader {

    private final int length;
    private final List<String> names;
    private final List<DataStructureNode> nodes;
    private boolean hasSeenEnd;
    private int currentIndex = 0;
    private DataStructureNodeReader currentReader;

    public TupleReader(int length) {
        this.length = length;
        this.names = new ArrayList<>(length);
        this.nodes = new ArrayList<>(length);
    }

    private void put(String name, DataStructureNode node) {
        this.names.add(name);
        this.nodes.add(node);
        currentIndex++;
    }

    private void putNode(DataStructureNode node) {
        this.nodes.add(node);
        currentIndex++;
    }

    private boolean filled() {
        return currentIndex == length;
    }

    @Override
    public void onArrayStart(String name, int length) {
        if (currentReader != null) {
            currentReader.onArrayStart(name, length);
            return;
        }

        names.add(name);
        currentReader = new ArrayReader(length);
    }

    @Override
    public void onArrayEnd() {
        if (currentReader != null) {
            currentReader.onArrayEnd();
            if (currentReader.isDone()) {
                putNode(currentReader.create());
                currentReader = null;
            }
            return;
        }

        throw new IllegalStateException();
    }

    @Override
    public void onTupleStart(String name, int length) {
        if (currentReader != null) {
            currentReader.onTupleStart(name, length);
            return;
        }

        names.add(name);
        currentReader = new TupleReader(length);
    }

    @Override
    public void onTupleEnd() {
        if (currentReader != null) {
            currentReader.onTupleEnd();
            if (currentReader.isDone()) {
                putNode(currentReader.create());
                currentReader = null;
            }
            return;
        }

        if (!filled()) {
            throw new IllegalStateException();
        }

        hasSeenEnd = true;
    }

    @Override
    public void onValue(String name, byte[] value) {
        if (currentReader != null) {
            currentReader.onValue(name, value);
            return;
        }

        if (filled()) {
            throw new IllegalStateException();
        }

        put(name, ValueNode.wrap(value));
    }

    @Override
    public boolean isDone() {
        return filled() && hasSeenEnd;
    }

    @Override
    public DataStructureNode create() {
        if (!isDone()) {
            throw new IllegalStateException();
        }

        return TupleNode.wrap(names, nodes);
    }
}
