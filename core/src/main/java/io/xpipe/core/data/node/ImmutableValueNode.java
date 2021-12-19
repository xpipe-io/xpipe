package io.xpipe.core.data.node;

public class ImmutableValueNode extends ValueNode {

    private final byte[] data;

    ImmutableValueNode(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString(int indent) {
        return new String(data) + "(I)";
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public ValueNode immutableView() {
        return this;
    }

    @Override
    public ValueNode mutableCopy() {
        return ValueNode.mutable(data);
    }

    @Override
    public DataStructureNode setRawData(byte[] data) {
        throw new UnsupportedOperationException("Value node is immutable");
    }

    public byte[] getRawData() {
        return data;
    }
}
