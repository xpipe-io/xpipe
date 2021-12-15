package io.xpipe.core.data.generic;

import java.io.IOException;
import java.io.InputStream;

public class GenericDataStreamReader {

    private static final int TUPLE_ID = 1;
    private static final int ARRAY_ID = 2;
    private static final int VALUE_ID = 3;
    private static final int NAME_ID = 4;

    public static void read(InputStream in, GenericDataStreamCallback cb) throws IOException {
        var b = in.read();
        if (b == -1) {
            return;
        }

        switch (b) {
            case TUPLE_ID -> {
                readTuple(in, cb);
            }
            case ARRAY_ID -> {
                readArray(in, cb);
            }
            case VALUE_ID -> {
                readValue(in, cb);
            }
            case NAME_ID -> {
                readName(in, cb);
                read(in, cb);
            }
            default -> throw new IllegalStateException("Unexpected value: " + b);
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
