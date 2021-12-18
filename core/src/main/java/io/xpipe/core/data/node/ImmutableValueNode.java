package io.xpipe.core.data.node;

import io.xpipe.core.data.DataStructureNode;

public class ImmutableValueNode extends ValueNode {

    private final byte[] data;

    ImmutableValueNode(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString(int indent) {
        return getClass().getSimpleName() + "(" + new String(data) + ")";
    }

    @Override
    public DataStructureNode setRawData(byte[] data) {
        throw new UnsupportedOperationException("Value node is immutable");
    }

    public byte[] getRawData() {
        return data;
    }
}
