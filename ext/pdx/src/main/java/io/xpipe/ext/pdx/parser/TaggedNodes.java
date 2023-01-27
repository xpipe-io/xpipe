package io.xpipe.ext.pdx.parser;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class TaggedNodes {

    public static final TagType[] NO_TAGS = new TagType[0];
    public static final TagType[] COLORS = new TagType[] {TagType.RGB, TagType.HSV, TagType.HSV360, TagType.HEX};
    public static final TagType[] ALL = TagType.values();

    public static TagType getTagType(TagType[] possible, NodeContext ctx, int index) {
        if (possible.length == 0) {
            return null;
        }

        var begin = ctx.getLiteralsBegin()[index];
        var length = ctx.getLiteralsLength()[index];

        // Make lookup as fast as possible
        if (possible == COLORS) {
            if (length != 3 && length != 6) {
                return null;
            }

            if (ctx.getData()[begin] != 'r' && ctx.getData()[begin] != 'h') {
                return null;
            }

            var end = begin + length;
            for (var t : COLORS) {
                if (Arrays.equals(t.getBytes(), 0, t.id.length(), ctx.getData(), begin, end)) {
                    return t;
                }
            }
        } else {
            if (length != 3 && length != 4 && length != 6) {
                return null;
            }

            if (ctx.getData()[begin] != 'r' && ctx.getData()[begin] != 'h' && ctx.getData()[begin] != 'L') {
                return null;
            }

            var end = begin + length;
            for (var t : ALL) {
                if (Arrays.equals(t.getBytes(), 0, t.id.length(), ctx.getData(), begin, end)) {
                    return t;
                }
            }
        }
        return null;
    }

    public enum TagType {
        RGB("rgb"),
        HSV("hsv"),
        HSV360("hsv360"),
        HEX("hex"),
        LIST("LIST");

        private final String id;
        private final byte[] bytes;

        TagType(String id) {
            this.id = id;
            this.bytes = id.getBytes(StandardCharsets.UTF_8);
        }

        public String getId() {
            return id;
        }

        public byte[] getBytes() {
            return bytes;
        }
    }
}
