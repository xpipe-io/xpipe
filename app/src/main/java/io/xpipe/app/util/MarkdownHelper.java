package io.xpipe.app.util;

import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.Arrays;
import java.util.function.UnaryOperator;

public class MarkdownHelper {

    public static String toHtml(
            String value,
            UnaryOperator<String> headTransformation,
            UnaryOperator<String> bodyTransformation,
            String bodyStyleClass) {
        MutableDataSet options = new MutableDataSet()
                .set(
                        Parser.EXTENSIONS,
                        Arrays.asList(
                                StrikethroughExtension.create(),
                                TaskListExtension.create(),
                                TablesExtension.create(),
                                FootnoteExtension.create(),
                                DefinitionExtension.create(),
                                AnchorLinkExtension.create(),
                                YamlFrontMatterExtension.create(),
                                TocExtension.create()))
                .set(FootnoteExtension.FOOTNOTE_BACK_LINK_REF_CLASS, "footnotes")
                .set(TablesExtension.WITH_CAPTION, false)
                .set(TablesExtension.COLUMN_SPANS, false)
                .set(TablesExtension.MIN_HEADER_ROWS, 1)
                .set(TablesExtension.MAX_HEADER_ROWS, 1)
                .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
                .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)
                .set(HtmlRenderer.GENERATE_HEADER_ID, true);
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Document document = parser.parse(value);
        var html = renderer.render(document);
        var result = bodyTransformation.apply(html);
        var headContent = headTransformation.apply("<meta charset=\"utf-8\"/>");
        return "<html><head>" + headContent
                + "</head><body"
                + (bodyStyleClass != null ? " class=\"" + bodyStyleClass + "\"" : "")
                + "><article class=\"markdown-body\">"
                + result
                + "</article></body></html>";
    }
}
