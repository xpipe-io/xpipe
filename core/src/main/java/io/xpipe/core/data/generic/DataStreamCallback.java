package io.xpipe.core.data.generic;

import java.util.function.Consumer;

public interface DataStreamCallback {

    static DataStreamCallback flat(Consumer<byte[]> con) {
        return new DataStreamCallback() {
            @Override
            public void onValue(String name, byte[] value) {
                con.accept(value);
            }
        };
    }

    default void onArrayStart(String name, int length) {
    }

    default void onArrayEnd() {
    }

    default void onTupleStart(String name, int length) {
    }

    default void onTupleEnd() {
    }

    default void onValue(String name, byte[] value) {
    }
}
