package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.FileKind;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.core.OsType;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

import java.util.List;

public class GradleRunMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        if (model.getFileSystem().getShell().isEmpty()) {
            return false;
        }

        if (entries.size() != 1) {
            return false;
        }

        if (entries.getFirst().getRawFileEntry().getKind() != FileKind.FILE) {
            return false;
        }

        OsType.Any osType = model.getFileSystem().getShell().orElseThrow().getOsType();
        var ext = switch (osType) {
            case OsType.Windows ignored -> "gradlew.bat";
            default -> "gradlew";
        };

        if (!entries.getFirst().getFileName().equalsIgnoreCase(ext)) {
            return false;
        }

        return true;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("runTask");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.CUSTOM;
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2e-elephant");
    }

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var tasks = new SimpleStringProperty();
        var modal = ModalOverlay.of(
                "gradleTasks",
                RegionBuilder.of(() -> {
                            var creationName = new TextField();
                            creationName.textProperty().bindBidirectional(tasks);
                            return creationName;
                        })
                        .prefWidth(350));
        modal.withDefaultButtons(() -> {
            var fixedTasks = tasks.getValue();
            if (fixedTasks == null) {
                return;
            }

            var parent = entries.getFirst().getRawFileEntry().getPath().getParent();
            var command = model.getFileSystem().getShell().orElseThrow().command(CommandBuilder.of()
                    .add("sh")
                    .addFile(entries.getFirst().getRawFileEntry().getPath())
                    .add(fixedTasks)
            );

            model.openTerminalAsync(fixedTasks, parent, command, true);
        });
        modal.show();
    }

}
