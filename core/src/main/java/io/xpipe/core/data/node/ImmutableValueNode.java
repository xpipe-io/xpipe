package io.xpipe.core.data.node;

public abstract class ImmutableValueNode extends ValueNode {

    @Override
    public String toString(int indent) {
        return (isTextual() ? "\"" : "") + asString() + (isTextual() ? "\"" : "") + " (I)";
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
}
