package io.xpipe.ext.pdx.parser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class NodeContext {

    private final byte[] data;
    private final Charset charset;
    private final int[] literalsBegin;
    private final short[] literalsLength;
    private final int literalsCount;

    public NodeContext() {
        this.data = null;
        this.charset = StandardCharsets.UTF_8;
        this.literalsBegin = null;
        this.literalsLength = null;
        this.literalsCount = 0;
    }

    public NodeContext(String data, boolean quoted) {
        this.data = quoted
                ? ("\"" + StringValues.escapeStringContent(data) + "\"").getBytes()
                : StringValues.escapeStringContent(data).getBytes();
        this.charset = StandardCharsets.UTF_8;
        this.literalsBegin = new int[] {0};
        this.literalsLength = new short[] {(short) this.data.length};
        this.literalsCount = 1;
    }

    public NodeContext(byte[] data, Charset charset, int[] literalsBegin, short[] literalsLength, int literalsCount) {
        this.data = data;
        this.charset = charset;
        this.literalsBegin = literalsBegin;
        this.literalsLength = literalsLength;
        this.literalsCount = literalsCount;
    }

    public boolean isQuoted(int literalIndex) {
        var begin = data[literalsBegin[literalIndex]] == '"';
        var end = data[literalsBegin[literalIndex] + literalsLength[literalIndex] - 1] == '"';
        return begin && end;
    }

    public String evaluate(int literalIndex) {
        return StringValues.unescapeScalarValue(this, literalIndex);
    }

    public String evaluateRaw(int literalIndex) {
        return new String(getData(), literalsBegin[literalIndex], literalsLength[literalIndex], getCharset());
    }

    public byte[] getData() {
        return data;
    }

    public Charset getCharset() {
        return charset;
    }

    public int[] getLiteralsBegin() {
        return literalsBegin;
    }

    public short[] getLiteralsLength() {
        return literalsLength;
    }

    public int getLiteralsCount() {
        return literalsCount;
    }
}
