package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.ValueType;

public class ValueNode extends DataStructureNode {

    private final byte[] data;

    private ValueNode(byte[] data) {
        this.data = data;
    }

    public static ValueNode wrap(byte[] data) {
        return new ValueNode(data);
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
    public DataType getDataType() {
        return new ValueType();
    }

    public byte[] getRawData() {
        return data;
    }
}
