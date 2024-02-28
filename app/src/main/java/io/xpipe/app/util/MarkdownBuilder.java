package io.xpipe.app.util;

import net.steppschuh.markdowngenerator.MarkdownElement;
import net.steppschuh.markdowngenerator.text.code.Code;
import net.steppschuh.markdowngenerator.text.code.CodeBlock;

public class MarkdownBuilder {

    private final StringBuilder builder = new StringBuilder();

    private MarkdownBuilder() {}

    public static MarkdownBuilder of() {
        return new MarkdownBuilder();
    }

    public MarkdownBuilder add(String t) {
        builder.append(t);
        return this;
    }

    public MarkdownBuilder line() {
        builder.append("\n");
        return this;
    }

    public MarkdownBuilder addCode(String s) {
        builder.append(new Code(s).getSerialized(""));
        return this;
    }

    public MarkdownBuilder addCodeBlock(String s) {
        builder.append("\n\n").append(new CodeBlock(s).getSerialized(""));
        return this;
    }

    public MarkdownBuilder addLine(MarkdownElement t) {
        builder.append(t.getSerialized("")).append("\n");
        return this;
    }

    public MarkdownBuilder addParagraph(String s) {
        builder.append("\n\n").append(s);
        return this;
    }

    public String build() {
        return builder.toString();
    }
}
