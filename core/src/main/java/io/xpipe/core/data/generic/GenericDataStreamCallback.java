package io.xpipe.core.data.generic;

import java.util.Map;

public interface GenericDataStreamCallback {

    default void onName(String name) {
    }

    default void onArrayStart(int length) {
    }

    default void onArrayEnd() {
    }

    default void onTupleStart(int length) {
    }

    default void onTupleEnd() {
    }


    default void onValue(byte[] value, Map<Integer, String> metaAttributes) {
    }
}
