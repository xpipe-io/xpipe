package io.xpipe.app.core;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;
import javafx.scene.Node;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.function.Function;
import java.util.regex.Pattern;

@Value
@AllArgsConstructor
public class AppFontSizes {

    private static final Pattern FONT_SIZE_PATTERN = Pattern.compile("-fx-font-size: \\d+(\\.\\d+)?pt;");

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

    private static void apply(Node node, Function<AppFontSizes, String> function) {
        if (AppPrefs.get() == null) {
            setFont(node, function.apply(getDefault()));
            return;
        }

        AppPrefs.get().theme().subscribe((newValue) -> {
            var effective = newValue != null ? newValue.getFontSizes() : getDefault();
            setFont(node, function.apply(effective));
        });
    }

    private static void setFont(Node node, String fontSize) {
        var s = node.getStyle();
        var matcher = FONT_SIZE_PATTERN.matcher(s);
        s = matcher.replaceAll("");
        node.setStyle("-fx-font-size: " + fontSize + "pt;" + s);
    }

    public static final AppFontSizes DEFAULT = getDefault();
    public static final AppFontSizes BASE_11 = ofBase("11");
    public static final AppFontSizes BASE_11_5 = ofBase("11.5");
    public static final AppFontSizes BASE_12 = ofBase("12");

    public static AppFontSizes ofBase(String base) {
        if (base.contains(".")) {
            var l = Integer.parseInt(base.split("\\.")[0]);
            var r = "." + base.split("\\.")[1];
            return new AppFontSizes((l - 2) + r, (l - 1) + r, base, (l + 1) + r, (l + 2) + r, (l + 7) + r);
        } else {
            var l = Integer.parseInt(base);
            return new AppFontSizes(l - 2 + "", l - 1 + "", l + "", l + 1 + "", l + 2 + "", l + 7 + "");
        }
    }
    
    public static AppFontSizes forOs(AppFontSizes windows, AppFontSizes linux, AppFontSizes mac) {
        return switch (OsType.getLocal()) {
            case OsType.Linux linux1 -> linux;
            case OsType.MacOs macOs -> mac;
            case OsType.Windows windows1 -> windows;
        };
    }

    public static AppFontSizes getDefault() {
        return forOs(AppFontSizes.BASE_11_5, AppFontSizes.BASE_11, AppFontSizes.BASE_12);
    }

    String xs;

    String sm;

    String base;

    String lg;

    String xl;

    String xxl;
}
