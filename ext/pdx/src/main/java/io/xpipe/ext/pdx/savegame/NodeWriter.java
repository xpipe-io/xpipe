package io.xpipe.ext.pdx.savegame;

import io.xpipe.core.data.node.TupleNode;
import io.xpipe.ext.pdx.parser.NodeContext;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface NodeWriter {

    static byte[] writeToBytes(TupleNode node, int maxLines, String indent) {
        var out = new ByteArrayOutputStream();
        var writer = new NodeWriterImpl(out, StandardCharsets.UTF_8, maxLines, indent);
        // node.writeTopLevel(writer);
        return out.toByteArray();
    }

    static String writeToString(TupleNode node, int maxLines, String indent) {
        return new String(writeToBytes(node, maxLines, indent), StandardCharsets.UTF_8);
    }

    static void write(OutputStream out, Charset charset, TupleNode node, String indent, int indentLevel)
            throws IOException {
        var bout = new BufferedOutputStream(out, 1000000);
        try {
            var writer = new NodeWriterImpl(bout, charset, Integer.MAX_VALUE, indent);
            for (int i = 0; i < indentLevel; i++) {
                writer.incrementIndent();
            }
            // node.writeTopLevel(writer);
            bout.flush();
        } catch (IOException e) {
            bout.close();
            throw e;
        }
    }

    void incrementIndent();

    void decrementIndent();

    void indent() throws IOException;

    void write(NodeContext ctx, int index) throws IOException;

    void write(String s) throws IOException;

    void space() throws IOException;

    void newLine() throws IOException;
}
