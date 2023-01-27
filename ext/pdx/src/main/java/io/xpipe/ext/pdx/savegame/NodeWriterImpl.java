package io.xpipe.ext.pdx.savegame;

import io.xpipe.ext.pdx.parser.NodeContext;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public final class NodeWriterImpl implements NodeWriter {

    private static final byte[] NEW_LINE = "\n".getBytes();
    private final OutputStream out;
    private final Charset charset;
    private final int maxLines;
    private final byte[] indentValue;
    private int currentLines;
    private boolean hitMaxLines;
    private int indent;

    public NodeWriterImpl(OutputStream out, Charset charset, int maxLines, String indentValue) {
        this.out = out;
        this.charset = charset;
        this.maxLines = maxLines;
        this.indentValue = indentValue.getBytes();
    }

    @Override
    public void incrementIndent() {
        indent++;
    }

    @Override
    public void decrementIndent() {
        indent--;
    }

    @Override
    public void indent() throws IOException {
        if (hitMaxLines) {
            return;
        }

        for (int i = 0; i < indent; i++) {
            out.write(indentValue);
        }
    }

    @Override
    public void write(NodeContext ctx, int index) throws IOException {
        if (hitMaxLines) {
            return;
        }

        if (ctx.getCharset().equals(charset)) {
            out.write(ctx.getData(), ctx.getLiteralsBegin()[index], ctx.getLiteralsLength()[index]);
        } else {
            out.write(ctx.evaluateRaw(index).getBytes(charset));
        }
    }

    @Override
    public void write(String s) throws IOException {
        if (hitMaxLines) {
            return;
        }

        out.write(s.getBytes(charset));
    }

    @Override
    public void space() throws IOException {
        if (hitMaxLines) {
            return;
        }

        out.write(" ".getBytes(charset));
    }

    @Override
    public void newLine() throws IOException {
        if (hitMaxLines) {
            return;
        }

        out.write(NEW_LINE);

        currentLines++;
        if (currentLines >= maxLines) {
            hitMaxLines = true;
        }
    }
}
