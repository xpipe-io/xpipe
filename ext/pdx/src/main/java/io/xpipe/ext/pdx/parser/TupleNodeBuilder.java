package io.xpipe.ext.pdx.parser;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TupleNodeBuilder {

    private final int maxSize;
    private final NodeContext context;
    private final int[] valueScalars;
    private final List<DataStructureNode> values;
    private int index;
    private int[] keyScalars;

    public TupleNodeBuilder(NodeContext context, int maxSize) {
        this.maxSize = maxSize;
        this.context = context;
        this.valueScalars = new int[maxSize];
        this.values = new ArrayList<>(maxSize);
    }

    private void initKeys() {
        if (keyScalars == null) {
            this.keyScalars = new int[maxSize];
            Arrays.fill(this.keyScalars, -1);
        }
    }

    public TupleNode build() {
        return new ContextTupleNode(context, keyScalars, valueScalars, values);
    }

    public void putScalarValue(int scalarIndex) {
        checkFull();

        valueScalars[index] = scalarIndex;
        values.add(null);
        index++;
    }

    public void putKeyAndScalarValue(int keyIndex, int scalarIndex) {
        checkFull();

        initKeys();
        keyScalars[index] = keyIndex;
        valueScalars[index] = scalarIndex;
        values.add(null);
        index++;
    }

    public void putNodeValue(DataStructureNode node) {
        checkFull();

        valueScalars[index] = -1;
        values.add(node);
        index++;
    }

    public void putKeyAndNodeValue(int keyIndex, DataStructureNode node) {
        checkFull();

        initKeys();
        keyScalars[index] = keyIndex;
        valueScalars[index] = -1;
        values.add(node);
        index++;
    }

    private void checkFull() {
        if (isFull()) {
            var string = build().toString();
            throw new IndexOutOfBoundsException("Node " + string + " is already full");
        }
    }

    public boolean isFull() {
        return getUsedSize() == getMaxSize();
    }

    public int getUsedSize() {
        return index;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
