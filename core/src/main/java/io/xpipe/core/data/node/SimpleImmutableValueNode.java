package io.xpipe.core.data.node;

public class SimpleImmutableValueNode extends ImmutableValueNode {

    private final byte[] data;
    private final boolean textual;

    SimpleImmutableValueNode(byte[] data, boolean textual) {
        this.data = data;
        this.textual = textual;
    }

    @Override
    public ValueNode mutableCopy() {
        return ValueNode.mutable(data, textual);
    }

    @Override
    public boolean isTextual() {
        return textual;
    }

    public byte[] getRawData() {
        return data;
    }

    @Override
    public final String asString() {
        return new String(getRawData());
    }
}
