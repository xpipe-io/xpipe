package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;

import java.util.Set;

@Getter
public class SvgView {

    private final ObservableValue<Number> width;
    private final ObservableValue<Number> height;
    private final ObservableValue<String> svgContent;

    private SvgView(ObservableValue<Number> width, ObservableValue<Number> height, ObservableValue<String> svgContent) {
        this.width = PlatformThread.sync(width);
        this.height = PlatformThread.sync(height);
        this.svgContent = PlatformThread.sync(svgContent);
    }

    @SneakyThrows
    public static SvgView create(ObservableValue<String> content) {
        var widthProperty = new SimpleIntegerProperty();
        var heightProperty = new SimpleIntegerProperty();
        content.subscribe(val -> {
            if (val == null || val.isBlank()) {
                return;
            }

            var dim = SvgHelper.getDimensions(val);
            widthProperty.set((int) Math.ceil(dim.getX()));
            heightProperty.set((int) Math.ceil(dim.getY()));
        });
        return new SvgView(widthProperty, heightProperty, content);
    }

    private String getHtml(String content) {
        return "<html><body style='margin: 0; padding: 0; border: none;' >" + content + "</body></html>";
    }

    private WebView createWebView() {
        var wv = new WebView();
        wv.getEngine()
                .setUserDataDirectory(
                        AppProperties.get().getDataDir().resolve("webview").toFile());
        // Sometimes a web view might not render when the background is set to transparent, at least according to stack
        // overflow
        wv.setPageFill(Color.valueOf("#00000001"));
        // wv.setPageFill(Color.BLACK);
        wv.getEngine().setJavaScriptEnabled(false);
        wv.setContextMenuEnabled(false);
        wv.setFocusTraversable(false);
        wv.setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        wv.setDisable(true);

        wv.getEngine().loadContent(svgContent.getValue() != null ? getHtml(svgContent.getValue()) : null);
        svgContent.subscribe(n -> {
            if (n == null) {
                wv.setOpacity(0.0);
                return;
            }

            wv.setOpacity(1.0);
            var html = getHtml(n);
            wv.getEngine().loadContent(html);
        });

        // Hide scrollbars that popup on every content change. Bug in WebView?
        wv.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
            Set<Node> scrolls = wv.lookupAll(".scroll-bar");
            for (Node scroll : scrolls) {
                scroll.setFocusTraversable(false);
                scroll.setVisible(false);
                scroll.setManaged(false);
            }
        });

        // As the aspect ratio of the WebView is kept constant, we can compute the zoom only using the width
        wv.zoomProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            return wv.getWidth() / width.getValue().doubleValue();
                        },
                        wv.widthProperty(),
                        width));

        wv.maxWidthProperty().bind(wv.prefWidthProperty());
        wv.maxHeightProperty().bind(wv.prefHeightProperty());

        wv.minWidthProperty().bind(wv.prefWidthProperty());
        wv.minHeightProperty().bind(wv.prefHeightProperty());

        return wv;
    }

    public WebView createWebview() {
        var wv = createWebView();
        wv.getStyleClass().add("svg-comp");
        return wv;
    }

    @Value
    @Builder
    public static class Structure implements CompStructure<StackPane> {
        StackPane pane;
        WebView webView;

        @Override
        public StackPane get() {
            return pane;
        }
    }
}
