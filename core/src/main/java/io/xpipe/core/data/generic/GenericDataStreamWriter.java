package io.xpipe.core.data.generic;

import io.xpipe.core.data.node.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class GenericDataStreamWriter {

    public static void writeStructure(OutputStream out, DataStructureNode node) throws IOException {
        out.write(DataStructureNodeIO.GENERIC_STRUCTURE_ID);
        write(out, node);
    }

    private static void write(OutputStream out, DataStructureNode node) throws IOException {
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
        if (s != null) {
            var b = s.getBytes(StandardCharsets.UTF_8);
            out.write(DataStructureNodeIO.GENERIC_NAME_ID);
            out.write(b.length);
            out.write(b);
        }
    }

    private static void writeTuple(OutputStream out, TupleNode tuple) throws IOException {
        out.write(DataStructureNodeIO.GENERIC_TUPLE_ID);
        out.write(tuple.size());
        for (int i = 0; i < tuple.size(); i++) {
            writeName(out, tuple.keyNameAt(i));
            write(out, tuple.at(i));
        }
    }

    private static void writeArray(OutputStream out, ArrayNode array) throws IOException {
        out.write(DataStructureNodeIO.GENERIC_ARRAY_ID);
        out.write(array.size());
        for (int i = 0; i < array.size(); i++) {
            write(out, array.at(i));
        }
    }

    private static void writeValue(OutputStream out, ValueNode n) throws IOException {
        out.write(DataStructureNodeIO.GENERIC_VALUE_ID);
        if (n.isNull()) {
            out.write(DataStructureNodeIO.VALUE_TYPE_NULL);
            return;
        }

        out.write(n.isTextual() ? DataStructureNodeIO.VALUE_TYPE_TEXT : DataStructureNodeIO.VALUE_TYPE_BARE);
        out.write(n.getRawData().length);
        out.write(n.getRawData());
    }
}
