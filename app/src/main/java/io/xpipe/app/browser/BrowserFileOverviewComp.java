package io.xpipe.app.browser;

import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.core.store.FileSystem;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class BrowserFileOverviewComp extends SimpleComp {

    OpenFileSystemModel model;
    ObservableList<FileSystem.FileEntry> list;

    @Override
    protected Region createSimple() {
        var c = new ListBoxViewComp<>(list, list, entry -> {
            return Comp.of(() -> {
                var icon = BrowserIcons.createIcon(entry);
                var l = new Button(entry.getPath(), icon.createRegion());
                l.setOnAction(event -> {
                    model.cd(entry.getPath());
                    event.consume();
                });
                l.setAlignment(Pos.CENTER_LEFT);
                GrowAugment.create(true,false).augment(l);
                return l;
            });
        })
                .styleClass("overview-file-list");
        return c.createRegion();
    }
}
