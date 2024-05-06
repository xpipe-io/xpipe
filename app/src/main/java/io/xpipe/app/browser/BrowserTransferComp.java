package io.xpipe.app.browser;

import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.DragOverPseudoClassAugment;
import io.xpipe.app.fxcomps.impl.*;
import io.xpipe.app.fxcomps.util.ListBindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.core.process.OsType;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class BrowserTransferComp extends SimpleComp {

    private final BrowserTransferModel model;

    public BrowserTransferComp(BrowserTransferModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var syncItems = PlatformThread.sync(model.getItems());
        var syncDownloaded = PlatformThread.sync(model.getDownloading());
        var syncAllDownloaded = PlatformThread.sync(model.getAllDownloaded());

        var background = new LabelComp(AppI18n.observable("transferDescription"))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2d-download-outline")))
                .visible(Bindings.isEmpty(syncItems));
        var backgroundStack =
                new StackComp(List.of(background)).grow(true, true).styleClass("download-background");

        var binding = ListBindingsHelper.mappedContentBinding(syncItems, item -> item.getBrowserEntry());
        var list = new BrowserSelectionListComp(
                        binding,
                        entry -> Bindings.createStringBinding(
                                () -> {
                                    var sourceItem = syncItems.stream()
                                            .filter(item -> item.getBrowserEntry() == entry)
                                            .findAny();
                                    if (sourceItem.isEmpty()) {
                                        return "?";
                                    }
                                    var name = entry.getModel() == null || sourceItem.get().downloadFinished().get()
                                                    ? "Local"
                                                    : entry.getModel().getFileSystemModel().getName();
                                    return entry.getFileName() + " (" + name + ")";
                                },
                                syncAllDownloaded))
                .apply(struc -> struc.get().setMinHeight(150))
                .grow(false, true);
        var dragNotice = new LabelComp(syncAllDownloaded.flatMap(
                        aBoolean -> aBoolean ? AppI18n.observable("dragLocalFiles") : AppI18n.observable("dragFiles")))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2h-hand-left")))
                .hide(Bindings.isEmpty(syncItems))
                .grow(true, false)
                .apply(struc -> struc.get().setPadding(new Insets(8)));

        var downloadButton = new IconButtonComp("mdi2d-download", () -> {
                    model.download();
                })
                .hide(Bindings.isEmpty(syncItems))
                .disable(syncAllDownloaded)
                .apply(new TooltipAugment<>("downloadStageDescription"));
        var clearButton = new IconButtonComp("mdi2c-close", () -> {
                    model.clear();
                })
                .hide(Bindings.isEmpty(syncItems));
        var clearPane = Comp.derive(
                new HorizontalComp(List.of(downloadButton, clearButton))
                        .apply(struc -> struc.get().setSpacing(10)),
                button -> {
                    var p = new AnchorPane(button);
                    AnchorPane.setRightAnchor(button, 10.0);
                    AnchorPane.setTopAnchor(button, 10.0);
                    p.setPickOnBounds(false);
                    return p;
                });

        var listBox = new VerticalComp(List.of(list, dragNotice)).padding(new Insets(10, 10, 5, 10));
        var stack = LoadingOverlayComp.noProgress(
                new StackComp(List.of(backgroundStack, listBox, clearPane))
                        .apply(DragOverPseudoClassAugment.create())
                        .apply(struc -> {
                            struc.get().setOnDragOver(event -> {
                                // Accept drops from inside the app window
                                if (event.getGestureSource() != null && event.getGestureSource() != struc.get()) {
                                    event.acceptTransferModes(TransferMode.ANY);
                                    event.consume();
                                }

                                // Accept drops from outside the app window
                                if (event.getGestureSource() == null
                                        && !event.getDragboard().getFiles().isEmpty()) {
                                    event.acceptTransferModes(TransferMode.ANY);
                                    event.consume();
                                }
                            });
                            struc.get().setOnDragDropped(event -> {
                                // Accept drops from inside the app window
                                if (event.getGestureSource() != null) {
                                    var drag = BrowserClipboard.retrieveDrag(event.getDragboard());
                                    if (drag == null) {
                                        return;
                                    }

                                    if (!(model.getBrowserSessionModel()
                                                    .getSelectedEntry()
                                                    .getValue()
                                            instanceof OpenFileSystemModel fileSystemModel)) {
                                        return;
                                    }

                                    var files = drag.getEntries();
                                    model.drop(fileSystemModel, files);
                                    event.setDropCompleted(true);
                                    event.consume();
                                }

                                // Accept drops from outside the app window
                                if (event.getGestureSource() == null) {
                                    model.dropLocal(event.getDragboard().getFiles());
                                    event.setDropCompleted(true);
                                    event.consume();
                                }
                            });
                            struc.get().setOnDragDetected(event -> {
                                if (syncDownloaded.getValue()) {
                                    return;
                                }

                                var selected = syncItems.stream()
                                        .map(item -> item.getBrowserEntry())
                                        .toList();
                                Dragboard db = struc.get().startDragAndDrop(TransferMode.COPY);

                                var cc = BrowserClipboard.startDrag(null, selected);
                                if (cc == null) {
                                    return;
                                }

                                var files = syncItems.stream()
                                        .filter(item -> item.downloadFinished().get())
                                        .map(item -> {
                                            try {
                                                var file = item.getLocalFile();
                                                if (!Files.exists(file)) {
                                                    return Optional.<File>empty();
                                                }

                                                return Optional.of(
                                                        file.toRealPath().toFile());
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        })
                                        .flatMap(Optional::stream)
                                        .toList();
                                cc.putFiles(files);
                                db.setContent(cc);

                                Image image = BrowserSelectionListComp.snapshot(FXCollections.observableList(selected));
                                db.setDragView(image, -20, 15);

                                event.setDragDetect(true);
                                event.consume();
                            });
                            struc.get().setOnDragDone(event -> {
                                // macOS does always report false here, which is unfortunate
                                if (!event.isAccepted() && !OsType.getLocal().equals(OsType.MACOS)) {
                                    return;
                                }

                                // Don't clear, it might be more convenient to keep the contents
                                // model.clear();
                                event.consume();
                            });
                        }),
                syncDownloaded);
        return stack.styleClass("transfer").createRegion();
    }
}
