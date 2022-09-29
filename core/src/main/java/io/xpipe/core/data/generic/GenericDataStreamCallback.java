package io.xpipe.core.data.generic;

import java.util.Map;

public interface GenericDataStreamCallback {

    default void onName(String name) {
    }

    default void onArrayStart(int length) {
    }

    default void onArrayEnd(Map<Integer, String> metaAttributes) {
    }

    default void onTupleStart(int length) {
    }

    default void onTupleEnd(Map<Integer, String> metaAttributes) {
    }


    default void onValue(byte[] value, Map<Integer, String> metaAttributes) {
    }
}
