package io.xpipe.core.data.typed;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.SimpleTupleNode;
import io.xpipe.core.data.node.ValueNode;

import java.io.IOException;
import java.io.OutputStream;

public class TypedDataStreamWriter {

    private static final int STRUCTURE_ID = 0;
    private static final int TUPLE_ID = 1;
    private static final int ARRAY_ID = 2;
    private static final int VALUE_ID = 3;

    public static void writeStructure(OutputStream out, DataStructureNode node) throws IOException {
        out.write(STRUCTURE_ID);
        write(out, node);
    }

    private static void write(OutputStream out, DataStructureNode node) throws IOException {
        if (node.isTuple()) {
            writeTuple(out, (SimpleTupleNode) node);
        }
        else if (node.isArray()) {
            writeArray(out, (ArrayNode) node);
        }
        else if (node.isValue()) {
            writeValue(out, (ValueNode) node);
        } else {
            throw new AssertionError();
        }
    }

    private static void writeValue(OutputStream out, ValueNode n) throws IOException {
        out.write(VALUE_ID);
        out.write(n.getRawData().length);
        out.write(n.getRawData());
    }

    private static void writeTuple(OutputStream out, SimpleTupleNode tuple) throws IOException {
        out.write(TUPLE_ID);
        out.write(tuple.size());
        for (int i = 0; i < tuple.size(); i++) {
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
}
