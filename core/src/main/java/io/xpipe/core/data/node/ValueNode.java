package io.xpipe.core.data.node;

import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.ValueType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public abstract class ValueNode extends DataStructureNode {

    protected ValueNode() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValueNode that)) return false;
        return Arrays.equals(getRawData(), that.getRawData());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getRawData());
    }

    @Override
    public abstract ValueNode immutableView();

    @Override
    public abstract ValueNode mutableCopy();

    public static ValueNode immutable(byte[] data, boolean textual) {
        return new SimpleImmutableValueNode(data, textual);
    }

    public static ValueNode immutable(Object o, boolean textual) {
        if (o == null) {
            return immutableNull();
        }

        return immutable(o.toString().getBytes(StandardCharsets.UTF_8), textual);
    }

    public static ValueNode immutableNull() {
        return MutableValueNode.NULL.immutableView();
    }

    public static ValueNode mutableNull() {
        return MutableValueNode.NULL.mutableCopy();
    }

    public static ValueNode mutable(byte[] data, boolean textual) {
        return new MutableValueNode(data, textual);
    }

    public static ValueNode mutable(Object o, boolean textual) {
        return mutable(o.toString().getBytes(StandardCharsets.UTF_8), textual);
    }

    public static ValueNode of(byte[] data) {
        return mutable(data, false);
    }

    public static ValueNode of(Object o) {
        return mutable(o, false);
    }

    public static ValueNode ofText(byte[] data) {
        return mutable(data, true);
    }

    public static ValueNode ofText(Object o) {
        return mutable(o, true);
    }

    @Override
    public abstract boolean isTextual();

    @Override
    public abstract DataStructureNode setRaw(byte[] data);

    @Override
    public abstract DataStructureNode set(Object newValue);

    @Override
    public abstract DataStructureNode set(Object newValue, boolean textual);

    @Override
    public final int asInt() {
        return Integer.parseInt(asString());
    }

    @Override
    public final boolean isValue() {
        return true;
    }

    @Override
    protected final String getName() {
        return "value node";
    }

    @Override
    public final DataType determineDataType() {
        return ValueType.of();
    }

    public abstract byte[] getRawData();
}
