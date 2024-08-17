package io.xpipe.app.browser.file;

import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.VBoxViewComp;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.core.store.FileEntry;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.function.Function;

@Value
@EqualsAndHashCode(callSuper = true)
public class BrowserFileOverviewComp extends SimpleComp {

    OpenFileSystemModel model;
    ObservableList<FileEntry> list;
    boolean grow;

    @Override
    protected Region createSimple() {
        Function<FileEntry, Comp<?>> factory = entry -> {
            return Comp.of(() -> {
                var icon = BrowserIcons.createIcon(entry);
                var graphic = new HorizontalComp(List.of(
                        icon,
                        new BrowserQuickAccessButtonComp(() -> new BrowserEntry(entry, model.getFileList()), model)));
                var l = new Button(entry.getPath(), graphic.createRegion());
                l.setGraphicTextGap(1);
                l.setOnAction(event -> {
                    model.cdAsync(entry.getPath());
                    event.consume();
                });
                l.setAlignment(Pos.CENTER_LEFT);
                GrowAugment.create(true, false).augment(l);
                return l;
            });
        };

        if (grow) {
            var c = new ListBoxViewComp<>(list, list, factory, true).styleClass("overview-file-list");
            return c.createRegion();
        } else {
            var c = new VBoxViewComp<>(list, list, factory).styleClass("overview-file-list");
            return c.createRegion();
        }
    }
}
