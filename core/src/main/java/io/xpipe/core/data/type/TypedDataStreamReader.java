package io.xpipe.core.data.type;

import io.xpipe.core.data.type.callback.TypedDataStreamCallback;

import java.io.IOException;
import java.io.InputStream;

public class TypedDataStreamReader {

    private static final int STRUCTURE_ID = 0;
    private static final int TUPLE_ID = 1;
    private static final int ARRAY_ID = 2;
    private static final int VALUE_ID = 3;

    public static boolean hasNext(InputStream in) throws IOException {
        var b = in.read();
        if (b == -1) {
            return false;
        }

        if (b != STRUCTURE_ID) {
            throw new IOException("Unexpected value: " + b);
        }

        return true;
    }

    public static void readStructures(InputStream in, TypedDataStreamCallback cb) throws IOException {
        while (true) {
            if (!hasNext(in)) {
                break;
            }

            cb.onNodeBegin();
            read(in, cb);
            cb.onNodeEnd();
        }
    }

    public static void readStructure(InputStream in, TypedDataStreamCallback cb) throws IOException {
        if (!hasNext(in)) {
            throw new IllegalStateException("No structure to read");
        }

        cb.onNodeBegin();
        read(in, cb);
        cb.onNodeEnd();
    }

    private static void read(InputStream in, TypedDataStreamCallback cb) throws IOException {
        var b = in.read();

        // Skip
        if (b == STRUCTURE_ID) {
            b = in.read();
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
            default -> throw new IllegalStateException("Unexpected value: " + b);
        }
    }

    private static void readTuple(InputStream in, TypedDataStreamCallback cb) throws IOException {
        var size = in.read();
        cb.onTupleBegin(size);
        for (int i = 0; i < size; i++) {
            read(in, cb);
        }
        cb.onTupleEnd();
    }

    private static void readArray(InputStream in, TypedDataStreamCallback cb) throws IOException {
        var size = in.read();
        cb.onArrayBegin(size);
        for (int i = 0; i < size; i++) {
            read(in, cb);
        }
        cb.onArrayEnd();
    }

    private static void readValue(InputStream in, TypedDataStreamCallback cb) throws IOException {
        var size = in.read();
        var data = in.readNBytes(size);
        cb.onValue(data);
    }
}
