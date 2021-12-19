package io.xpipe.core.data.node;

public class MutableValueNode extends ValueNode {

    private byte[] data;

    MutableValueNode(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString(int indent) {
        return new String(data) + "(M)";
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public ValueNode immutableView() {
        return new ImmutableValueNode(data);
    }

    @Override
    public ValueNode mutableCopy() {
        return new MutableValueNode(data);
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
