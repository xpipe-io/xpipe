package io.xpipe.app.browser;

import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.FileSystem;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class SelectedFileListComp extends SimpleComp {

    ObservableList<FileSystem.FileEntry> list;

    @Override
    protected Region createSimple() {
        var c = new ListBoxViewComp<>(list, list, entry -> {
            var l = new LabelComp(FileNames.getFileName(entry.getPath())).apply(struc -> struc.get()
                    .setGraphic(FileIcons.createIcon(entry).createRegion()));
            return l;
        }).styleClass("selected-file-list");
        return c.createRegion();
    }
}
