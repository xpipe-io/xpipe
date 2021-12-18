package io.xpipe.core.data.type.callback;

import io.xpipe.core.data.type.TupleType;

public class FlatArrayTypeCallback implements DataTypeCallback {

    private final FlatCallback cb;
    private int arrayDepth = 0;

    public FlatArrayTypeCallback(FlatCallback cb) {
        this.cb = cb;
    }

    private boolean isInArray() {
        return arrayDepth > 0;
    }

    @Override
    public void onValue() {
        if (isInArray()) {
            return;
        }

        cb.onValue();
    }

    @Override
    public void onTupleBegin(TupleType tuple) {
        if (isInArray()) {
            throw new IllegalStateException();
        }

        cb.onTupleBegin(tuple);
    }

    @Override
    public void onTupleEnd() {
        cb.onTupleEnd();
    }

    public void onArray() {
        if (isInArray()) {
            throw new IllegalStateException();
        }

        arrayDepth++;
    }

    public interface FlatCallback {

        default void onValue() {
        }

        default void onTupleBegin(TupleType tuple) {
        }

        default void onTupleEnd() {
        }

        default void onFlatArray() {
        }
    }
}
