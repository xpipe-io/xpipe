package io.xpipe.core.data.node;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DataStructureNodeIO {

    public static final int GENERIC_STRUCTURE_ID = 0;
    public static final int GENERIC_TUPLE_ID = 1;
    public static final int GENERIC_ARRAY_ID = 2;
    public static final int GENERIC_VALUE_ID = 3;
    public static final int GENERIC_NAME_ID = 4;

    public static final int TYPED_STRUCTURE_ID = 5;
    public static final int TYPED_TUPLE_ID = 6;
    public static final int TYPED_ARRAY_ID = 7;
    public static final int TYPED_VALUE_ID = 8;

    public static void writeShort(OutputStream out, int value) throws IOException {
        var buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) value);
        out.write(buffer.array());
    }

    public static void writeString(OutputStream out, String s) throws IOException {
        if (s != null) {
            var b = s.getBytes(StandardCharsets.UTF_8);
            DataStructureNodeIO.writeShort(out, b.length);
            out.write(b);
        }
    }

    public static short parseShort(InputStream in) throws IOException {
        var read = in.readNBytes(2);
        if (read.length < 2) {
            throw new IllegalStateException("Unable to read short");
        }

        var buffer = ByteBuffer.wrap(read);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }

    public static String parseString(InputStream in) throws IOException {
        var nameLength = parseShort(in);
        var name = new String(in.readNBytes(nameLength), StandardCharsets.UTF_8);
        return name;
    }

    public static Map<Integer, String> parseAttributes(InputStream in) throws IOException {
        var attributesLength = parseShort(in);
        if (attributesLength == 0) {
            return null;
        }

        var map = new HashMap<Integer, String>();
        for (int i = 0; i < attributesLength; i++) {
            var key = parseShort(in);
            var value = parseString(in);
            map.put((int) key, value);
        }
        return map;
    }

    public static void writeAttributes(OutputStream out, DataStructureNode s) throws IOException {
        if (s.getMetaAttributes() != null) {
            writeShort(out, s.getMetaAttributes().size());
            for (Map.Entry<Integer, String> entry : s.getMetaAttributes().entrySet()) {
                Integer integer = entry.getKey();
                var value = entry.getValue().getBytes(StandardCharsets.UTF_8);
                writeShort(out, integer);
                writeShort(out, value.length);
                out.write(value);
            }
        } else {
            out.write(0);
        }
    }
}
