package io.xpipe.app.comp.base;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.SneakyThrows;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.function.UnaryOperator;

public class MarkdownComp extends Comp<CompStructure<StackPane>> {

    private final ObservableValue<String> markdown;
    private final UnaryOperator<String> htmlTransformation;

    public MarkdownComp(String markdown, UnaryOperator<String> htmlTransformation) {
        this.markdown = new SimpleStringProperty(markdown);
        this.htmlTransformation = htmlTransformation;
    }

    public MarkdownComp(ObservableValue<String> markdown, UnaryOperator<String> htmlTransformation) {
        this.markdown = markdown;
        this.htmlTransformation = htmlTransformation;
    }

    private String getHtml() {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Document document = parser.parse(markdown.getValue());
        var html = renderer.render(document);
        var result = htmlTransformation.apply(html);
        return "<article class=\"markdown-body\">" + result + "</article>";
    }

    @SneakyThrows
    private WebView createWebView() {
        var wv = new WebView();
        wv.setPageFill(Color.TRANSPARENT);
        var theme = AppPrefs.get() != null && AppPrefs.get().theme.getValue().getTheme().isDarkMode() ? "web/github-markdown-dark.css" : "web/github-markdown-light.css";
        var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, theme)
                .orElseThrow();
        wv.getEngine().setUserStyleSheetLocation(url.toString());

        SimpleChangeListener.apply(PlatformThread.sync(markdown), val -> {
            // Work around for https://bugs.openjdk.org/browse/JDK-8199014
            try {
                var file = Files.createTempFile(null, ".html");
                Files.writeString(file, getHtml());
                var contentUrl = file.toUri();
                wv.getEngine().load(contentUrl.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        wv.getStyleClass().add("markdown-comp");
        addLinkHandler(wv.getEngine());
        return wv;
    }

    private void addLinkHandler(WebEngine engine) {
        engine.getLoadWorker()
                .stateProperty()
                .addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
                    String toBeopen = engine.getLoadWorker().getMessage().trim().replace("Loading ", "");
                    if (toBeopen.contains("http://") || toBeopen.contains("https://")) {
                        engine.getLoadWorker().cancel();
                        try {
                            Desktop.getDesktop().browse(URI.create(toBeopen));
                        } catch (Exception e) {
                            ErrorEvent.fromThrowable(e).omit().handle();
                        }
                    }
                }));
    }

    @Override
    public CompStructure<StackPane> createBase() {
        var sp = new StackPane(createWebView());
        sp.setPadding(Insets.EMPTY);
        return new SimpleCompStructure<>(sp);
    }
}
