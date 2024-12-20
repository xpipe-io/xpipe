package io.xpipe.app.update;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.Hyperlinks;

import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class UpdateAvailableAlert {

    public static void showIfNeeded() {
        UpdateHandler uh = XPipeDistributionType.get().getUpdateHandler();
        if (uh.getPreparedUpdate().getValue() == null) {
            return;
        }

        // Check whether we still have the latest version prepared
        uh.refreshUpdateCheckSilent(false, uh.getPreparedUpdate().getValue().isSecurityOnly());
        if (uh.getPreparedUpdate().getValue() == null) {
            return;
        }

        TrackEvent.withInfo("Showing update alert ...")
                .tag("version", uh.getPreparedUpdate().getValue().getVersion())
                .handle();
        var u = uh.getPreparedUpdate().getValue();

        var markdown = new MarkdownComp(u.getBody() != null ? u.getBody() : "", s -> s).createRegion();
        var updaterContent = uh.createInterface();

        Region region;
        if (updaterContent != null) {
            var stack = new StackPane(updaterContent);
            stack.setPadding(new Insets(18));
            var box = new VBox(markdown, stack);
            box.setFillWidth(true);
            box.setPadding(Insets.EMPTY);
            region = box;
        } else {
            region = markdown;
        }

        var modal = ModalOverlay.of("updateReadyAlertTitle", Comp.of(() -> region).prefWidth(600), null);
        modal.addButton(new ModalOverlay.ModalButton("ignore",null,true,false));
        modal.addButton(new ModalOverlay.ModalButton("checkOutUpdate",() -> {
            Hyperlinks.open(uh.getPreparedUpdate().getValue().getReleaseUrl());
        },false,false));
        modal.addButton(new ModalOverlay.ModalButton("install",() -> {
            uh.executeUpdateAndClose();
        },false,true));
        AppDialog.show(modal);
    }
}
