package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.ext.DownloadModuleInstall;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.app.util.Hyperlinks;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.nio.file.Files;

@Value
@EqualsAndHashCode(callSuper = true)
public class InstallExtensionComp extends SimpleComp {

    DownloadModuleInstall install;

    @Override
    protected Region createSimple() {
        var builder = new DynamicOptionsBuilder(false);
        builder.addTitle("installRequired");
        var header = new LabelComp(AppI18n.observable("extensionInstallDescription"))
                .apply(struc -> struc.get().setWrapText(true));
        builder.addComp(header);

        if (install.getVendorURL() != null) {
            var vendorLink = Comp.of(() -> {
                var hl = new Hyperlink(install.getVendorURL());
                hl.setOnAction(e -> Hyperlinks.open(install.getVendorURL()));
                return hl;
            });
            builder.addComp(vendorLink);
        }

        if (install.getLicenseFile() != null) {
            builder.addTitle("license");

            var changeNotice = new LabelComp(AppI18n.observable("extensionInstallLicenseNote"))
                    .apply(struc -> struc.get().setWrapText(true));
            builder.addComp(changeNotice);

            var license = Comp.of(() -> {
                var text = new TextArea();
                text.setEditable(false);
                AppResources.with(install.getModule(), install.getLicenseFile(), file -> {
                    var s = Files.readString(file);
                    text.setText(s);
                });
                text.setWrapText(true);
                VBox.setVgrow(text, Priority.ALWAYS);
                AppFont.verySmall(text);
                return text;
            });
            builder.addComp(license);
        }

        return builder.build();
    }
}
