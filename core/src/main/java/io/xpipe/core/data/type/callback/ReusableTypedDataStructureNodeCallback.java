package io.xpipe.core.data.type.callback;

public class ReusableTypedDataStructureNodeCallback implements TypedDataStreamCallback {

    @Override
    public void onValue(byte[] data) {
        TypedDataStreamCallback.super.onValue(data);
    }

    @Override
    public void onTupleBegin(int size) {
        TypedDataStreamCallback.super.onTupleBegin(size);
    }

    @Override
    public void onTupleEnd() {
        TypedDataStreamCallback.super.onTupleEnd();
    }

    @Override
    public void onArrayBegin(int size) {
        TypedDataStreamCallback.super.onArrayBegin(size);
    }

    @Override
    public void onArrayEnd() {
        TypedDataStreamCallback.super.onArrayEnd();
    }
}
