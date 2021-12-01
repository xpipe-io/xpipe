package io.xpipe.core.data.generic;

import java.io.IOException;
import java.io.InputStream;

public class DataStreamReader {

    private static final int TUPLE_ID = 1;
    private static final int ARRAY_ID = 2;
    private static final int VALUE_ID = 3;

    public static void read(InputStream in, DataStreamCallback cb) throws IOException {
        var b = in.read();
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
            default -> throw new IllegalStateException("Unexpected value: " + b);
        }
    }

    private static String readName(InputStream in) throws IOException {
        var nameLength = in.read();
        return new String(in.readNBytes(nameLength));
    }

    private static void readTuple(InputStream in, DataStreamCallback cb) throws IOException {
        var name = readName(in);
        var size = in.read();
        cb.onTupleStart(name, size);
        for (int i = 0; i < size; i++) {
            read(in, cb);
        }
        cb.onTupleEnd();
    }

    private static void readArray(InputStream in, DataStreamCallback cb) throws IOException {
        var name = readName(in);
        var size = in.read();
        cb.onArrayStart(name, size);
        for (int i = 0; i < size; i++) {
            read(in, cb);
        }
        cb.onArrayEnd();
    }

    private static void readValue(InputStream in, DataStreamCallback cb) throws IOException {
        var name = readName(in);
        var size = in.read();
        var data = in.readNBytes(size);
        cb.onValue(name, data);
    }
}
