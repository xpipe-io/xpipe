package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;

public class DataStructureReader implements DataStreamCallback {

    private boolean isWrapped;
    private DataStructureNodeReader reader;

    public DataStructureNode create() {
        return null;
    }


    @Override
    public void onArrayStart(String name, int length) {
        if (reader != null) {
            reader.onArrayStart(name, length);
            return;
        }

        if (name != null) {
            reader = new TupleReader(1);
            reader.onArrayStart(name, length);
        } else {
            reader = new ArrayReader(length);
            reader.onArrayStart(null, length);
        }
    }

    @Override
    public void onArrayEnd() {
        if (reader != null) {
            reader.onArrayEnd();
        }
    }

    @Override
    public void onTupleStart(String name, int length) {
        if (reader != null) {
            reader.onTupleStart(name, length);
            return;
        }

        if (name != null) {
            reader = new TupleReader(1);
            reader.onTupleStart(name, length);
        } else {
            reader = new TupleReader(length);
        }
    }

    @Override
    public void onTupleEnd() {
        if (reader != null) {
            reader.onTupleEnd();
            if (reader.isDone()) {

            }
        }

        DataStreamCallback.super.onTupleEnd();
    }

    @Override
    public void onValue(String name, byte[] value) {
        DataStreamCallback.super.onValue(name, value);
    }
}
