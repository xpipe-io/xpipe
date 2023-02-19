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
public class SvgComp {

    private final ObservableValue<Number> width;
    private final ObservableValue<Number> height;
    private final ObservableValue<String> svgContent;

    public SvgComp(ObservableValue<Number> width, ObservableValue<Number> height, ObservableValue<String> svgContent) {
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
    public static SvgComp create(ObservableValue<String> content) {
        var widthProperty = new SimpleIntegerProperty();
        var heightProperty = new SimpleIntegerProperty();
        SimpleChangeListener.apply(content, val -> {
            if (val == null) {
                return;
            }

            var regularExpression = Pattern.compile("<svg.+?width=\"([^\s]+)\"", Pattern.DOTALL);
            var matcher = regularExpression.matcher(val);
            if (!matcher.find()) {
                return;
            }
            var width = matcher.group(1);
            regularExpression = Pattern.compile("<svg.+?height=\"([^\s]+)\"", Pattern.DOTALL);
            matcher = regularExpression.matcher(val);
            matcher.find();
            var height = matcher.group(1);
            var widthInteger = parseSize(width).pixels();
            var heightInteger = parseSize(height).pixels();
            widthProperty.set((int) Math.ceil(widthInteger));
            heightProperty.set((int) Math.ceil(heightInteger));
        });
        return new SvgComp(widthProperty, heightProperty, content);
    }

    private String getHtml(String content) {
        return "<html><body style='margin: 0; padding: 0; border: none;' >" + content + "</body></html>";
    }

    private WebView createWebView() {
        var wv = new WebView();
        wv.setPageFill(Color.TRANSPARENT);
        wv.setDisable(true);

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
                scroll.setVisible(false);
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
