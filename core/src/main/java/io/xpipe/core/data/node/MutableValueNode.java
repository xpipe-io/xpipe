package io.xpipe.core.data.node;

import io.xpipe.core.data.DataStructureNode;

public class MutableValueNode extends ValueNode {

    private byte[] data;

    MutableValueNode(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString(int indent) {
        return getClass().getSimpleName() + "(" + new String(data) + ")";
    }

    @Override
    public DataStructureNode setRawData(byte[] data) {
        this.data = data;
        return this;
    }

    public byte[] getRawData() {
        return data;
    }
}
