package io.xpipe.core.data.node;

import java.nio.charset.StandardCharsets;

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
        if (getRawData() == null) {
            return "null";
        }

        return new String(getRawData(), StandardCharsets.UTF_8);
    }
}
