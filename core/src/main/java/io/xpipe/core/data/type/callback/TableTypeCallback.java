package io.xpipe.core.data.type.callback;

import io.xpipe.core.data.generic.DataStructureNodePointer;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.TupleType;

import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TableTypeCallback implements DataTypeCallback {

    private final Stack<TupleType> tuples = new Stack<>();
    private final Stack<Integer> keyIndices = new Stack<>();
    private final Consumer<String> newTuple;
    private final Runnable endTuple;
    private final BiConsumer<String, DataStructureNodePointer> newValue;

    private TableTypeCallback(Consumer<String> newTuple, Runnable endTuple, BiConsumer<String, DataStructureNodePointer> newValue) {
        this.newTuple = newTuple;
        this.endTuple = endTuple;
        this.newValue = newValue;
    }

    public static DataTypeCallback create(Consumer<String> newTuple, Runnable endTuple, BiConsumer<String, DataStructureNodePointer> newValue) {
        return new TableTypeCallback(newTuple, endTuple, newValue);
    }

    private boolean isOnTopLevel() {
        return tuples.size() <= 1;
    }

    private void onAnyValue() {
        var pointer = DataStructureNodePointer.builder();
        for (int index : keyIndices) {
            pointer.index(index);
        }
        var p = pointer.build();
        newValue.accept(tuples.peek().getNames().get(keyIndices.peek()), p);

        moveIndex();
    }

    private void moveIndex() {
        var index = keyIndices.pop();
        index++;
        keyIndices.push(index);
    }

    @Override
    public void onValue() {
        onAnyValue();
    }

    @Override
    public void onTupleBegin(TupleType tuple) {
        if (!isOnTopLevel()) {
            moveIndex();
        }

        tuples.push(tuple);
        keyIndices.push(0);

        if (!isOnTopLevel()) {
            newTuple.accept(tuples.peek().getNames().get(keyIndices.peek()));
        }
    }

    @Override
    public void onTupleEnd() {
        endTuple.run();
        tuples.pop();
        keyIndices.pop();
    }

    @Override
    public void onArray(ArrayType type) {
        onAnyValue();
    }
}
