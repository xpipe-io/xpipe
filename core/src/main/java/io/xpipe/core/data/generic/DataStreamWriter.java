package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DataStreamWriter {

    private static final int TUPLE_ID = 1;
    private static final int ARRAY_ID = 2;
    private static final int VALUE_ID = 3;

    public static void write(OutputStream out, DataStructureNode node) throws IOException {
        if (node.isTuple()) {
            writeTuple(out, (TupleNode) node);
        }
    }

    private static void writeName(OutputStream out, String s) throws IOException {
        out.write(s.length());
        out.write(s.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeTuple(OutputStream out, TupleNode tuple) throws IOException {
        out.write(TUPLE_ID);
        for (int i = 0; i < tuple.size(); i++) {
            writeName(out, tuple.nameAt(i));
            write(out, tuple.at(i));
        }
    }
}
