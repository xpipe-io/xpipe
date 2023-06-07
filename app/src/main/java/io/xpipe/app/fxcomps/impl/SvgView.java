package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.geometry.Point2D;
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
import java.util.regex.Pattern;

@Getter
public class SvgView {

    private final ObservableValue<Number> width;
    private final ObservableValue<Number> height;
    private final ObservableValue<String> svgContent;

    private SvgView(
            ObservableValue<Number> width,
            ObservableValue<Number> height,
            ObservableValue<String> svgContent) {
        this.width = PlatformThread.sync(width);
        this.height = PlatformThread.sync(height);
        this.svgContent = PlatformThread.sync(svgContent);
    }

    private static Size parseSize(String string) {
        for (SizeUnits unit : SizeUnits.values()) {
            if (string.endsWith(unit.toString())) {
                return new Size(
                        Double.parseDouble(string.substring(
                                0, string.length() - unit.toString().length())),
                        unit);
            }
        }
        return new Size(Double.parseDouble(string), SizeUnits.PX);
    }

    @SneakyThrows
    public static SvgView create(ObservableValue<String> content) {
        var widthProperty = new SimpleIntegerProperty();
        var heightProperty = new SimpleIntegerProperty();
        SimpleChangeListener.apply(content, val -> {
            if (val == null || val.isBlank()) {
                return;
            }

            var dim = getDimensions(val);
            widthProperty.set((int) Math.ceil(dim.getX()));
            heightProperty.set((int) Math.ceil(dim.getY()));
        });
        return new SvgView(widthProperty, heightProperty, content);
    }

    private static Point2D getDimensions(String val) {
        var regularExpression = Pattern.compile("<svg[^>]+?width=\"([^\s]+)\"", Pattern.DOTALL);
        var matcher = regularExpression.matcher(val);

        if (!matcher.find()) {
            var viewBox = Pattern.compile(
                    "<svg.+?viewBox=\"([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\"", Pattern.DOTALL);
            matcher = viewBox.matcher(val);
            if (matcher.find()) {
                return new Point2D(
                        parseSize(matcher.group(3)).pixels(),
                        parseSize(matcher.group(4)).pixels());
            }
        }

        var width = matcher.group(1);
        regularExpression = Pattern.compile("<svg.+?height=\"([^\s]+)\"", Pattern.DOTALL);
        matcher = regularExpression.matcher(val);
        matcher.find();
        var height = matcher.group(1);
        return new Point2D(parseSize(width).pixels(), parseSize(height).pixels());
    }

    private String getHtml(String content) {
        return "<html><body style='margin: 0; padding: 0; border: none;' >" + content + "</body></html>";
    }

    private WebView createWebView() {
        var wv = new WebView();
        // Sometimes a web view might not render when the background is said to transparent, at least according to stack overflow
        wv.setPageFill(Color.valueOf("#00000001"));
        // wv.setPageFill(Color.BLACK);
        wv.getEngine().setJavaScriptEnabled(false);
        wv.setContextMenuEnabled(false);
        wv.setFocusTraversable(false);
        wv.setAccessibleRole(AccessibleRole.IMAGE_VIEW);

        wv.getEngine().loadContent(getHtml(svgContent.getValue()));
        svgContent.addListener((c, o, n) -> {
            if (n == null) {
                wv.getEngine().loadContent("");
                return;
            }

            wv.getEngine().loadContent(getHtml(n));
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
