package io.xpipe.core.data.generic;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeIO;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GenericDataStreamParser {

    public static DataStructureNode parse(InputStream in) throws IOException {
        var reader = new GenericDataStructureNodeReader();
        parse(in, reader);
        return reader.create();
    }

    public static List<DataStructureNode> parseN(InputStream in, int n) throws IOException {
        var list = new ArrayList<DataStructureNode>();
        var reader = new GenericDataStructureNodeReader();
        for (int i = 0; i < n; i++) {
            parse(in, reader);
            list.add(reader.create());
        }
        return list;
    }

    public static void parse(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var b = in.read();
        if (b == -1) {
            return;
        }

        if (b == DataStructureNodeIO.GENERIC_STRUCTURE_ID) {
            b = in.read();
        }

        switch (b) {
            case DataStructureNodeIO.GENERIC_TUPLE_ID -> {
                parseTuple(in, cb);
            }
            case DataStructureNodeIO.GENERIC_ARRAY_ID -> {
                parseArray(in, cb);
            }
            case DataStructureNodeIO.GENERIC_VALUE_ID -> {
                parseValue(in, cb);
            }
            case DataStructureNodeIO.GENERIC_NAME_ID -> {
                parseName(in, cb);
                parse(in, cb);
            }
            default -> throw new IllegalStateException("Unexpected type id: " + b);
        }
    }

    private static void parseName(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var nameLength = in.read();
        var name = new String(in.readNBytes(nameLength));
        cb.onName(name);
    }

    private static void parseTuple(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var size = in.read();
        cb.onTupleStart(size);
        for (int i = 0; i < size; i++) {
            parse(in, cb);
        }
        var attributes = DataStructureNodeIO.parseAttributes(in);
        cb.onTupleEnd();
    }

    private static void parseArray(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var size = in.read();
        cb.onArrayStart(size);
        for (int i = 0; i < size; i++) {
            parse(in, cb);
        }
        var attributes = DataStructureNodeIO.parseAttributes(in);
        cb.onArrayEnd();
    }

    private static void parseValue(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var size = in.read();
        var data = in.readNBytes(size);
        var attributes = DataStructureNodeIO.parseAttributes(in);
        cb.onValue(data, attributes);
    }
}
