package io.xpipe.core.data.generic;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeIO;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GenericDataStreamParser {

    public static DataStructureNode read(InputStream in) throws IOException {
        var reader = new GenericDataStructureNodeReader();
        read(in, reader);
        return reader.create();
    }

    public static List<DataStructureNode> readN(InputStream in, int n) throws IOException {
        var list = new ArrayList<DataStructureNode>();
        var reader = new GenericDataStructureNodeReader();
        for (int i = 0; i < n; i++) {
            read(in, reader);
            list.add(reader.create());
        }
        return list;
    }

    public static void read(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var b = in.read();
        if (b == -1) {
            return;
        }

        switch (b) {
            case DataStructureNodeIO.GENERIC_TUPLE_ID -> {
                readTuple(in, cb);
            }
            case DataStructureNodeIO.GENERIC_ARRAY_ID -> {
                readArray(in, cb);
            }
            case DataStructureNodeIO.GENERIC_VALUE_ID -> {
                readValue(in, cb);
            }
            case DataStructureNodeIO.GENERIC_NAME_ID -> {
                readName(in, cb);
                read(in, cb);
            }
            default -> throw new IllegalStateException("Unexpected type id: " + b);
        }
    }

    private static void readName(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var nameLength = in.read();
        var name = new String(in.readNBytes(nameLength));
        cb.onName(name);
    }

    private static void readTuple(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var size = in.read();
        cb.onTupleStart(size);
        for (int i = 0; i < size; i++) {
            read(in, cb);
        }
        cb.onTupleEnd();
    }

    private static void readArray(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var size = in.read();
        cb.onArrayStart(size);
        for (int i = 0; i < size; i++) {
            read(in, cb);
        }
        cb.onArrayEnd();
    }

    private static void readValue(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var size = in.read();
        var data = in.readNBytes(size);
        cb.onValue(data);
    }
}
