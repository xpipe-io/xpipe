package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class FileBrowserStatusBarComp extends SimpleComp {

    OpenFileSystemModel model;

    @Override
    protected Region createSimple() {
        var cc = PlatformThread.sync(FileBrowserClipboard.currentCopyClipboard);
        var ccCount = Bindings.createStringBinding(() -> {
            if (cc.getValue() != null && cc.getValue().getEntries().size() > 0) {
                return cc.getValue().getEntries().size() + " file" + (cc.getValue().getEntries().size() > 1 ? "s" : "") + " in clipboard";
            } else {
                return null;
            }
        }, cc);

        var selectedCount = PlatformThread.sync(Bindings.createIntegerBinding(() -> {
            return model.getFileList().getSelected().size();
        }, model.getFileList().getSelected()));

        var allCount = PlatformThread.sync(Bindings.createIntegerBinding(() -> {
            return (int) model.getFileList().getAll().getValue().stream().filter(entry -> !entry.isSynthetic()).count();
        }, model.getFileList().getAll()));

        var selectedComp = new LabelComp(Bindings.createStringBinding(() -> {
            if (selectedCount.getValue().intValue() == 0) {
                return null;
            } else {
                return selectedCount.getValue() + " / " + allCount.getValue() + " selected";
            }
        }, selectedCount, allCount));

        var bar = new ToolBar();
        bar.getItems().setAll(
                new LabelComp(ccCount).createRegion(),
                new Spacer(),
                selectedComp.createRegion()
        );
        bar.getStyleClass().add("status-bar");
        bar.setOnDragDetected(event -> {
            event.consume();
            bar.startFullDrag();
        });
        AppFont.small(bar);

        // Use status bar as an extension of file list
        new ContextMenuAugment<>(false, () -> new FileContextMenu(model,true)).augment(new SimpleCompStructure<>(bar));

        return bar;
    }
}
