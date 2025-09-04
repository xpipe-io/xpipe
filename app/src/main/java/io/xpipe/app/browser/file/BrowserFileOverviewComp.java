package io.xpipe.app.browser.file;

import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.ext.FileEntry;

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

    BrowserFileSystemTabModel model;
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
                var l = new Button(entry.getPath().toString(), graphic.createRegion());
                l.setGraphicTextGap(1);
                l.setOnAction(event -> {
                    model.cdAsync(entry.getPath().toString());
                    event.consume();
                });
                l.setAlignment(Pos.CENTER_LEFT);
                l.setMaxWidth(10000);
                return l;
            });
        };

        var c = new ListBoxViewComp<>(list, list, factory, true).styleClass("overview-file-list");
        if (!grow) {
            c.apply(struc -> struc.get().setFitToHeight(true));
        }
        return c.createRegion();
    }
}
