package io.xpipe.core.data.node;

public class ImmutableValueNode extends ValueNode {

    private final byte[] data;
    private final boolean textual;

    ImmutableValueNode(byte[] data, boolean textual) {
        this.data = data;
        this.textual = textual;
    }

    @Override
    public String toString(int indent) {
        return (textual ? "\"" : "") + new String(data) + (textual ? "\"" : "") + " (I)";
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
        return ValueNode.mutable(data, textual);
    }

    @Override
    public boolean isTextual() {
        return textual;
    }

    @Override
    public DataStructureNode setRaw(byte[] data) {
        throw new UnsupportedOperationException("Value node is immutable");
    }

    @Override
    public DataStructureNode set(Object newValue) {
        throw new UnsupportedOperationException("Value node is immutable");
    }

    @Override
    public DataStructureNode set(Object newValue, boolean textual) {
        throw new UnsupportedOperationException("Value node is immutable");
    }

    public byte[] getRawData() {
        return data;
    }
}
