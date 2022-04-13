package io.xpipe.core.data.typed;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.type.TupleType;

public interface TypedDataStreamCallback {

    default void onValue(byte[] data, boolean textual) {
    }

    default void onGenericNode(DataStructureNode node) {
    }

    default void onTupleBegin(TupleType type) {
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
