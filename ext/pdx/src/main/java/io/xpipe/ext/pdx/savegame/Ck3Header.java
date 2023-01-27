package io.xpipe.ext.pdx.savegame;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/**
 * CK3 header format:
 * <p>
 * SAV0 <unknown> 0 <type> <8 hex digits of randomness> <8 hex digits of meta data size>
 * <p>
 * unknown:
 * Can either be 0 or 1, don't know the meaning yet
 * type:
 * 3: Compressed + Binary
 * 2: Compressed + Plaintext
 * 1: Uncompressed + Binary
 * 0: Uncompressed + Plaintext
 * <p>
 * meta data size:
 * Length of meta data block at the beginning of the file in bytes.
 */
public record Ck3Header(boolean unknown, boolean compressed, boolean binary, long randomness, long metaLength) {

    public static final int LENGTH = 23;

    public Ck3Header(boolean unknown, boolean compressed, boolean binary, int metaLength) {
        this(unknown, compressed, binary, (new Random().nextLong() >>> 1) % 0xFFFFFFFFL + 1, metaLength);
    }

    public static boolean skipsHeader(byte[] input) {
        if (input.length < LENGTH) {
            return true;
        }

        return Arrays.equals(input, 0, 9, "meta_data".getBytes(StandardCharsets.UTF_8), 0, 9);
    }

    public static Ck3Header determineHeaderForFile(byte[] data) {
        if (data.length < LENGTH) {
            throw new IllegalArgumentException();
        }

        return fromString(new String(data, 0, LENGTH));
    }

    public static Ck3Header fromString(String header) {
        if (!header.startsWith("SAV000") && !header.startsWith("SAV010")) {
            throw new IllegalArgumentException(
                    "Invalid CK3 header start: " + header.substring(0, Math.min(6, header.length())));
        }

        boolean unknown = Integer.parseInt(header.substring(4, 5)) == 1;
        int type = Integer.parseInt(header.substring(6, 7));
        boolean compressed = (type & 2) != 0;
        boolean binary = (type & 1) != 0;
        long randomness = Long.parseLong(header.substring(7, 15).toUpperCase(), 16);
        long metaLength = Long.parseLong(header.substring(15, 23).toUpperCase(), 16);
        return new Ck3Header(unknown, compressed, binary, randomness, metaLength);
    }

    @Override
    public String toString() {
        int type = (compressed ? 2 : 0) + (binary ? 1 : 0);
        return "SAV0" + (unknown ? 1 : 0) + "0" + type + String.format("%08x", randomness)
                + String.format("%08x", metaLength);
    }
}
