package io.xpipe.app.core.window;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.LicenseProvider;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import lombok.Getter;

public class AppWindowTitle {

    @Getter
    private static final StringProperty title = new SimpleStringProperty(createTitle());

    public static void init() {
        if (LicenseProvider.get() != null) {
            var t = LicenseProvider.get().licenseTitle();
            t.subscribe(ignored -> {
                title.setValue(createTitle());
            });
        }

        var l = AppI18n.activeLanguage();
        l.subscribe(ignored -> {
            title.setValue(createTitle());
        });

        if (AppDistributionType.get() != AppDistributionType.UNKNOWN) {
            var u = AppDistributionType.get().getUpdateHandler().getPreparedUpdate();
            u.subscribe(ignored -> {
                title.setValue(createTitle());
            });
        }
    }

    private static String createTitle() {
        var t = LicenseProvider.get() != null
                ? " " + LicenseProvider.get().licenseTitle().getValue()
                : "";
        var base = String.format(AppNames.ofMain().getName() + "%s (%s)", t, AppProperties.get().getVersion());
        var prefix = AppProperties.get().isStaging() ? "[Public Test Build, Not a proper release] " : "";
        var dist = AppDistributionType.get();
        if (dist != AppDistributionType.UNKNOWN) {
            var u = dist.getUpdateHandler().getPreparedUpdate();
            var suffix = u.getValue() != null
                    ? " " + AppI18n.get("updateReadyTitle", u.getValue().getVersion())
                    : "";
            return prefix + base + suffix;
        } else {
            return prefix + base;
        }
    }
}
