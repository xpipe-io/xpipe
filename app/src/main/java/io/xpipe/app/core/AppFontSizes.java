package io.xpipe.app.core;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.OsType;

import javafx.scene.Node;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.function.Function;
import java.util.regex.Pattern;

@Value
@AllArgsConstructor
public class AppFontSizes {

    public static final AppFontSizes BASE_10 = ofBase("10");
    public static final AppFontSizes BASE_10_5 = ofBase("10.5");
    public static final AppFontSizes BASE_11 = ofBase("11");

    private static final Pattern FONT_SIZE_PATTERN = Pattern.compile("-fx-font-size: \\d+(\\.\\d+)?pt;");
    // -1.0pt
    String xs;
    // -0.5pt
    String sm;
    // 0pt
    String base;
    // +0.5pt
    String lg;
    // +1.0pt
    String xl;
    // +2.0pt
    String xxl;
    // +3.0pt
    String xxxl;
    // +7.0pt
    String title;

    public static void xs(Node node) {
        apply(node, AppFontSizes::getXs);
    }

    public static void sm(Node node) {
        apply(node, AppFontSizes::getSm);
    }

    public static void base(Node node) {
        apply(node, AppFontSizes::getBase);
    }

    public static void lg(Node node) {
        apply(node, AppFontSizes::getLg);
    }

    public static void xl(Node node) {
        apply(node, AppFontSizes::getXl);
    }

    public static void xxl(Node node) {
        apply(node, AppFontSizes::getXxl);
    }

    public static void xxxl(Node node) {
        apply(node, AppFontSizes::getXxxl);
    }

    public static void title(Node node) {
        apply(node, AppFontSizes::getTitle);
    }

    public static void apply(Node node, Function<AppFontSizes, String> function) {
        if (AppPrefs.get() == null) {
            setFont(node, function.apply(getDefault()));
            return;
        }

        AppPrefs.get().theme().subscribe((newValue) -> {
            var effective = newValue != null ? newValue.getFontSizes().get() : getDefault();
            setFont(node, function.apply(effective));
        });

        AppPrefs.get().useSystemFont().addListener((ignored, ignored2, newValue) -> {
            var effective = AppPrefs.get().theme().getValue() != null
                    ? AppPrefs.get().theme().getValue().getFontSizes().get()
                    : getDefault();
            setFont(node, function.apply(effective));
        });
    }

    private static void setFont(Node node, String fontSize) {
        var s = node.getStyle();
        var matcher = FONT_SIZE_PATTERN.matcher(s);
        s = matcher.replaceAll("");
        node.setStyle("-fx-font-size: " + fontSize + "pt;" + s);
    }

    public static AppFontSizes ofBase(String base) {
        if (base.contains(".")) {
            var l = Integer.parseInt(base.split("\\.")[0]);
            var r = "." + base.split("\\.")[1];
            return new AppFontSizes(
                    (l - 1) + r, (l - 1) + "", base, (l + 1) + "", (l + 1) + r, (l + 2) + r, (l + 3) + r, (l + 7) + r);
        } else {
            var l = Integer.parseInt(base);
            return new AppFontSizes(
                    (l - 1) + "", (l - 1) + ".5", l + "", l + ".5", l + 1 + "", l + 2 + "", l + 3 + "", l + 7 + "");
        }
    }

    private static AppFontSizes fallbackFontSize(AppFontSizes s) {
        if (s == BASE_10) {
            return BASE_10;
        } else if (s == BASE_10_5) {
            return BASE_10_5;
        } else if (s == BASE_11) {
            return BASE_10_5;
        } else {
            return s;
        }
    }

    public static AppFontSizes forOs(AppFontSizes windows, AppFontSizes linux, AppFontSizes mac) {
        var inter = AppPrefs.get() != null && !AppPrefs.get().useSystemFont().getValue();
        var r =
                switch (OsType.ofLocal()) {
                    case OsType.Linux ignored -> linux;
                    case OsType.MacOs ignored -> mac;
                    case OsType.Windows ignored -> windows;
                };
        return inter ? fallbackFontSize(r) : r;
    }

    public static AppFontSizes getDefault() {
        return forOs(AppFontSizes.BASE_10_5, AppFontSizes.BASE_10, AppFontSizes.BASE_11);
    }
}
