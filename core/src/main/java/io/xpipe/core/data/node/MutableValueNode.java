package io.xpipe.core.data.node;

import java.nio.charset.StandardCharsets;

public class MutableValueNode extends ValueNode {

    static final MutableValueNode NULL = new MutableValueNode(null, false);

    private byte[] data;
    private boolean textual;

    MutableValueNode(byte[] data, boolean textual) {
        this.data = data;
        this.textual = textual;
    }

    @Override
    public String asString() {
        return new String(data);
    }

    @Override
    public String toString(int indent) {
        if (isNull()) {
            return "null (M)";
        }

        return (textual ? "\"" : "") + new String(data) + (textual ? "\"" : "") +  " (M)";
    }

    @Override
    public boolean isNull() {
        return data == null;
    }

    @Override
    public boolean isTextual() {
        return textual;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public ValueNode immutableView() {
        return new SimpleImmutableValueNode(data, textual);
    }

    @Override
    public ValueNode mutableCopy() {
        return new MutableValueNode(data, textual);
    }

    @Override
    public DataStructureNode setRaw(byte[] data) {
        this.data = data;
        return this;
    }

    @Override
    public DataStructureNode set(Object newValue) {
        if (newValue == null) {
            this.data = null;
            this.textual = false;
        } else {
            setRaw(newValue.toString().getBytes(StandardCharsets.UTF_8));
        }

        return this;
    }

    @Override
    public DataStructureNode set(Object newValue, boolean textual) {
        if (newValue == null && textual) {
            throw new IllegalArgumentException("Can't set a textual null");
        }

        if (newValue == null) {
            this.data = null;
            this.textual = false;
        } else {
            setRaw(newValue.toString().getBytes(StandardCharsets.UTF_8));
            this.textual = textual;
        }

        return this;
    }

    public byte[] getRawData() {
        return data;
    }
}
