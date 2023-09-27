package io.xpipe.app.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.function.UnaryOperator;

public class MarkdownHelper {

    public static String toHtml(String value, UnaryOperator<String> htmlTransformation) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Document document = parser.parse(value);
        var html = renderer.render(document);
        var result = htmlTransformation.apply(html);
        return "<meta charset=\"utf-8\"/><article class=\"markdown-body\">" + result + "</article>";
    }
}
