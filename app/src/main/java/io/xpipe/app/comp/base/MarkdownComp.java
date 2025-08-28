package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.platform.MarkdownHelper;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
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

    private static Boolean WEB_VIEW_SUPPORTED;
    private static Path TEMP;
    private final ObservableValue<String> markdown;
    private final UnaryOperator<String> htmlTransformation;
    private final boolean bodyPadding;

    public MarkdownComp(String markdown, UnaryOperator<String> htmlTransformation, boolean bodyPadding) {
        this.markdown = new SimpleStringProperty(markdown);
        this.htmlTransformation = htmlTransformation;
        this.bodyPadding = bodyPadding;
    }

    public MarkdownComp(
            ObservableValue<String> markdown, UnaryOperator<String> htmlTransformation, boolean bodyPadding) {
        this.markdown = markdown;
        this.htmlTransformation = htmlTransformation;
        this.bodyPadding = bodyPadding;
    }

    private Path getHtmlFile(String markdown) {
        if (TEMP == null) {
            TEMP = ShellTemp.getLocalTempDataDirectory("webview");
        }

        if (markdown == null) {
            return null;
        }

        int hash;
        // Rebuild files for updates in case the css have been changed
        if (AppProperties.get().isImage()) {
            hash = markdown.hashCode() + AppProperties.get().getVersion().hashCode();
        } else {
            hash = markdown.hashCode();
        }
        var file = TEMP.resolve("md-" + hash + ".html");
        if (Files.exists(file)) {
            return file;
        }

        var html = MarkdownHelper.toHtml(markdown, s -> s, htmlTransformation, bodyPadding ? "padded" : null);
        try {
            // Workaround for https://bugs.openjdk.org/browse/JDK-8199014
            FileUtils.forceMkdir(file.getParent().toFile());
            Files.writeString(file, html);
            return file;
        } catch (IOException e) {
            // Any possible IO errors can occur here
            ErrorEventFactory.fromThrowable(e).expected().handle();
            return null;
        }
    }

    @SneakyThrows
    private WebView createWebView() {
        var wv = new WebView();
        wv.getEngine().setJavaScriptEnabled(false);
        wv.setContextMenuEnabled(false);
        wv.setPageFill(Color.TRANSPARENT);
        wv.getEngine()
                .setUserDataDirectory(
                        AppProperties.get().getDataDir().resolve("webview").toFile());
        var theme = AppPrefs.get() != null
                        && AppPrefs.get().theme().getValue() != null
                        && AppPrefs.get().theme().getValue().isDark()
                ? "misc/github-markdown-dark.css"
                : "misc/github-markdown-light.css";
        var url = AppResources.getResourceURL(AppResources.MAIN_MODULE, theme).orElseThrow();
        wv.getEngine().setUserStyleSheetLocation(url.toString());

        PlatformThread.sync(markdown).subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var file = getHtmlFile(val);
                if (file != null) {
                    var contentUrl = file.toUri();
                    wv.getEngine().load(contentUrl.toString());
                }
            });
        });

        // Fix initial scrollbar size
        wv.lookupAll(".scroll-bar").stream().findFirst().ifPresent(node -> {
            Region region = (Region) node;
            region.setMinWidth(0);
            region.setPrefWidth(7);
            region.setMaxWidth(7);
        });

        wv.getStyleClass().add("markdown-comp");
        addLinkHandler(wv.getEngine());
        return wv;
    }

    private void addLinkHandler(WebEngine engine) {
        engine.getLoadWorker()
                .stateProperty()
                .addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
                    String toBeopen =
                            engine.getLoadWorker().getMessage().strip().replace("Loading ", "");
                    if (toBeopen.contains("http://") || toBeopen.contains("https://") || toBeopen.contains("mailto:")) {
                        engine.getLoadWorker().cancel();
                        Hyperlinks.open(toBeopen);
                    }
                }));
    }

    @Override
    public CompStructure<StackPane> createBase() {
        var sp = new StackPane();

        if (OsType.getLocal() == OsType.WINDOWS && AppProperties.get().getArch().equals("arm64")) {
            WEB_VIEW_SUPPORTED = false;
        }

        if (WEB_VIEW_SUPPORTED == null || WEB_VIEW_SUPPORTED) {
            try {
                var wv = createWebView();
                WEB_VIEW_SUPPORTED = true;
                sp.getChildren().addAll(wv);
            } catch (Throwable t) {
                ErrorEventFactory.fromThrowable(t).handle();
                WEB_VIEW_SUPPORTED = false;
            }
        }

        if (!WEB_VIEW_SUPPORTED) {
            var text = new TextArea();
            text.setEditable(false);
            text.setWrapText(true);
            markdown.subscribe(s -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    text.setText(s);
                });
            });
            sp.getChildren().add(text);
        }

        return new SimpleCompStructure<>(sp);
    }
}
