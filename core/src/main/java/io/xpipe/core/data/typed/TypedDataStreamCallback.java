package io.xpipe.core.data.typed;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.type.TupleType;

import java.io.IOException;

public interface TypedDataStreamCallback {

    default void onValue(byte[] data) {
    }

    default void onGenericNode(DataStructureNode node) {
    }

    default void onTupleBegin(TupleType type) {
    }

    default void onTupleEnd() {
    }

    default void onArrayBegin(int size) throws IOException {
    }

    default void onArrayEnd() {
    }

    default void onNodeBegin() {
    }

    default void onNodeEnd() {
    }
}
