package io.xpipe.core.data.node;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.ValueType;
import lombok.EqualsAndHashCode;

import java.nio.charset.StandardCharsets;

@EqualsAndHashCode(callSuper = false)
public class ValueNode extends DataStructureNode {

    private byte[] data;

    private ValueNode(byte[] data) {
        this.data = data;
    }

    public static ValueNode wrap(byte[] data) {
        return new ValueNode(data);
    }

    public static ValueNode of(Object o) {
        return new ValueNode(o.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public DataStructureNode setRawData(byte[] data) {
        this.data = data;
        return this;
    }

    @Override
    public boolean isValue() {
        return true;
    }

    @Override
    public int asInt() {
        return Integer.parseInt(asString());
    }

    @Override
    public String asString() {
        return new String(data);
    }

    @Override
    protected String getName() {
        return "value node";
    }

    @Override
    public String toString(int indent) {
        return new String(data);
    }

    @Override
    public DataType getDataType() {
        return new ValueType();
    }

    public byte[] getRawData() {
        return data;
    }
}
