package io.xpipe.app.browser;

import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.DragOverPseudoClassAugment;
import io.xpipe.app.fxcomps.impl.*;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.store.FileNames;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.List;

public class BrowserTransferComp extends SimpleComp {

    private final BrowserTransferModel stage;

    public BrowserTransferComp(BrowserTransferModel stage) {
        this.stage = stage;
    }

    @Override
    protected Region createSimple() {
        var background = new LabelComp(AppI18n.observable("transferDescription")).apply(
                struc -> struc.get().setGraphic(new FontIcon("mdi2d-download-outline"))).visible(
                BindingsHelper.persist(Bindings.isEmpty(stage.getItems())));
        var backgroundStack = new StackComp(List.of(background)).grow(true, true).styleClass("download-background");

        var binding = BindingsHelper.mappedContentBinding(stage.getItems(), item -> item.getFileEntry());
        var list = new BrowserSelectionListComp(binding, entry -> Bindings.createStringBinding(() -> {
            var sourceItem = stage.getItems().stream().filter(item -> item.getFileEntry() == entry).findAny();
            if (sourceItem.isEmpty()) {
                return "?";
            }
            var name = sourceItem.get().getFinishedDownload().get() ? "Local" : DataStorage.get().getStoreDisplayName(
                    entry.getFileSystem().getStore()).orElse("?");
            return FileNames.getFileName(entry.getPath()) + " (" + name + ")";
        }, stage.getAllDownloaded())).apply(struc -> struc.get().setMinHeight(150)).grow(false, true);
        var dragNotice = new LabelComp(stage.getAllDownloaded()
                .flatMap(aBoolean -> aBoolean ? AppI18n.observable("dragLocalFiles") : AppI18n.observable("dragFiles"))).apply(
                struc -> struc.get().setGraphic(new FontIcon("mdi2e-export"))).hide(
                PlatformThread.sync(BindingsHelper.persist(Bindings.isEmpty(stage.getItems())))).grow(true, false).apply(
                struc -> struc.get().setPadding(new Insets(8)));

        var downloadButton = new IconButtonComp("mdi2d-download", () -> {
            stage.download();
        }).hide(BindingsHelper.persist(Bindings.isEmpty(stage.getItems()))).disable(PlatformThread.sync(stage.getAllDownloaded())).apply(
                new FancyTooltipAugment<>("downloadStageDescription"));
        var clearButton = new IconButtonComp("mdi2c-close", () -> {
            stage.clear();
        }).hide(BindingsHelper.persist(Bindings.isEmpty(stage.getItems())));
        var clearPane = Comp.derive(new HorizontalComp(List.of(downloadButton, clearButton)).apply(struc -> struc.get().setSpacing(10)), button -> {
            var p = new AnchorPane(button);
            AnchorPane.setRightAnchor(button, 20.0);
            AnchorPane.setTopAnchor(button, 20.0);
            p.setPickOnBounds(false);
            return p;
        });

        var listBox = new VerticalComp(List.of(list, dragNotice)).padding(new Insets(10, 10, 5, 10));
        var stack = new LoadingOverlayComp(new StackComp(List.of(backgroundStack, listBox, clearPane)).apply(DragOverPseudoClassAugment.create())
                .apply(struc -> {
                    struc.get().setOnDragOver(event -> {
                        // Accept drops from inside the app window
                        if (event.getGestureSource() != null && event.getGestureSource() != struc.get()) {
                            event.acceptTransferModes(TransferMode.ANY);
                            event.consume();
                        }
                    });
                    struc.get().setOnDragDropped(event -> {
                        if (event.getGestureSource() != null) {
                            var files = BrowserClipboard.retrieveDrag(event.getDragboard()).getEntries();
                            stage.drop(files);
                            event.setDropCompleted(true);
                            event.consume();
                        }
                    });
                    struc.get().setOnDragDetected(event -> {
                        if (stage.getDownloading().get()) {
                            return;
                        }

                        // Drag within browser
                        if (!stage.getAllDownloaded().get()) {
                            var selected = stage.getItems().stream().map(item -> item.getFileEntry()).toList();
                            Dragboard db = struc.get().startDragAndDrop(TransferMode.COPY);
                            db.setContent(BrowserClipboard.startDrag(null, selected));

                            Image image = BrowserSelectionListComp.snapshot(FXCollections.observableList(selected));
                            db.setDragView(image, -20, 15);

                            event.setDragDetect(true);
                            event.consume();
                            return;
                        }

                        // Drag outside browser
                        var files = stage.getItems().stream().map(item -> {
                            try {
                                return item.getLocalFile().toRealPath().toFile();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }).toList();
                        Dragboard db = struc.get().startDragAndDrop(TransferMode.MOVE);
                        var cc = new ClipboardContent();
                        cc.putFiles(files);
                        db.setContent(cc);

                        var image = BrowserSelectionListComp.snapshot(
                                FXCollections.observableList(stage.getItems().stream().map(item -> item.getFileEntry()).toList()));
                        db.setDragView(image, -20, 15);

                        event.setDragDetect(true);
                        event.consume();
                    });
                    struc.get().setOnDragDone(event -> {
                        if (!event.isAccepted()) {
                            return;
                        }

                        stage.getItems().clear();
                        event.consume();
                    });
                }), PlatformThread.sync(stage.getDownloading()));
        return stack.createRegion();
    }
}
