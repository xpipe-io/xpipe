package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;

import java.util.ArrayList;
import java.util.List;

public class ArrayReader implements DataStructureNodeReader {

    private final List<DataStructureNode> nodes;
    private int length;
    private boolean hasSeenEnd;
    private int currentIndex = 0;
    private DataStructureNodeReader currentReader;

    public ArrayReader(int length) {
        this.length = length;
        this.nodes = new ArrayList<>(length);
    }

    @Override
    public void onArrayStart(String name, int length) {
        DataStructureNodeReader.super.onArrayStart(name, length);
    }

    @Override
    public void onArrayEnd() {
        DataStructureNodeReader.super.onArrayEnd();
    }

    @Override
    public void onTupleStart(String name, int length) {
        DataStructureNodeReader.super.onTupleStart(name, length);
    }

    @Override
    public void onTupleEnd() {
        DataStructureNodeReader.super.onTupleEnd();
    }

    @Override
    public void onValue(String name, byte[] value) {
        DataStructureNodeReader.super.onValue(name, value);
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public DataStructureNode create() {
        return null;
    }
}
