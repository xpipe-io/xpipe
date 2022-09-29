package io.xpipe.core.data.typed;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.type.TupleType;

import java.util.Map;

public interface TypedDataStreamCallback {

    default void onValue(byte[] data, Map<Integer, String> metaAttributes) {
    }

    default void onGenericNode(DataStructureNode node) {
    }

    default void onTupleBegin(TupleType type) {
    }

    default void onTupleEnd(Map<Integer, String> metaAttributes) {
    }

    default void onArrayBegin(int size) {
    }

    default void onArrayEnd(Map<Integer, String> metaAttributes) {
    }

    default void onNodeBegin() {
    }

    default void onNodeEnd() {
    }
}
