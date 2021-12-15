package io.xpipe.core.data.typed;

public interface TypedDataStreamCallback {

    default void onValue(byte[] data) {
    }

    default void onTupleBegin(int size) {
    }

    default void onTupleEnd() {
    }

    default void onArrayBegin(int size) {
    }

    default void onArrayEnd() {
    }

    default void onNodeBegin() {
    }

    default void onNodeEnd() {
    }
}
