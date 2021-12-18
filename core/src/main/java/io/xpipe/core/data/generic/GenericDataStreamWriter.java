package io.xpipe.core.data.generic;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class GenericDataStreamWriter {

    private static final int TUPLE_ID = 1;
    private static final int ARRAY_ID = 2;
    private static final int VALUE_ID = 3;
    private static final int NAME_ID = 4;

    public static void write(OutputStream out, DataStructureNode node) throws IOException {
        if (node.isTuple()) {
            writeTuple(out, (TupleNode) node);
        } else if (node.isArray()) {
            writeArray(out, (ArrayNode) node);
        } else if (node.isValue()) {
            writeValue(out, (ValueNode) node);
        } else {
            throw new IllegalStateException();
        }
    }

    private static void writeName(OutputStream out, String s) throws IOException {
        var b = s.getBytes(StandardCharsets.UTF_8);
        out.write(NAME_ID);
        out.write(b.length);
        out.write(b);
    }

    private static void writeTuple(OutputStream out, TupleNode tuple) throws IOException {
        out.write(TUPLE_ID);
        out.write(tuple.size());
        for (int i = 0; i < tuple.size(); i++) {
            writeName(out, tuple.nameAt(i));
            write(out, tuple.at(i));
        }
    }

    private static void writeArray(OutputStream out, ArrayNode array) throws IOException {
        out.write(ARRAY_ID);
        out.write(array.size());
        for (int i = 0; i < array.size(); i++) {
            write(out, array.at(i));
        }
    }

    private static void writeValue(OutputStream out, ValueNode value) throws IOException {
        out.write(VALUE_ID);
        out.write(value.getRawData().length);
        out.write(value.getRawData());
    }
}
