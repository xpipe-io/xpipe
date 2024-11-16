package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.resources.AppResources;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.MarkdownHelper;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ShellTemp;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static Path TEMP;

    private Path getHtmlFile(String markdown) {
        if (TEMP == null) {
            TEMP = ShellTemp.getLocalTempDataDirectory("wv");
        }

        if (markdown == null) {
            return null;
        }

        var hash = markdown.hashCode();
        var file = TEMP.resolve("md-" + hash + ".html");
        if (Files.exists(file)) {
            return file;
        }

        var html = MarkdownHelper.toHtml(markdown, s -> s, htmlTransformation, null);
        try {
            // Workaround for https://bugs.openjdk.org/browse/JDK-8199014
            FileUtils.forceMkdir(file.getParent().toFile());
            Files.writeString(file, html);
            return file;
        } catch (IOException e) {
            // Any possible IO errors can occur here
            ErrorEvent.fromThrowable(e).expected().handle();
            return null;
        }
    }

    @SneakyThrows
    private WebView createWebView() {
        var wv = new WebView();
        wv.getEngine().setJavaScriptEnabled(false);
        wv.setContextMenuEnabled(false);
        wv.getEngine()
                .setUserDataDirectory(
                        AppProperties.get().getDataDir().resolve("webview").toFile());
        wv.setPageFill(Color.TRANSPARENT);
        var theme = AppPrefs.get() != null
                        && AppPrefs.get().theme.getValue() != null
                        && AppPrefs.get().theme.getValue().isDark()
                ? "misc/github-markdown-dark.css"
                : "misc/github-markdown-light.css";
        var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, theme).orElseThrow();
        wv.getEngine().setUserStyleSheetLocation(url.toString());

        PlatformThread.sync(markdown).subscribe(val -> {
            var file = getHtmlFile(val);
            if (file != null) {
                var contentUrl = file.toUri();
                wv.getEngine().load(contentUrl.toString());
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
                        Hyperlinks.open(toBeopen);
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
