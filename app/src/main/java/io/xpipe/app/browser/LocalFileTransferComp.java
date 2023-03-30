package io.xpipe.app.browser;

import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.List;

public class LocalFileTransferComp extends SimpleComp {

    private final LocalFileTransferStage stage;

    public LocalFileTransferComp(LocalFileTransferStage stage) {
        this.stage = stage;
    }

    @Override
    protected Region createSimple() {
        var background = new LabelComp(AppI18n.observable("download"))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2d-download-outline")))
                .visible(BindingsHelper.persist(Bindings.isEmpty(stage.getItems())));
        var backgroundStack =
                new StackComp(List.of(background)).grow(true, true).styleClass("download-background");
        var binding = BindingsHelper.mappedContentBinding(stage.getItems(), item -> item.getFileEntry());
        var list = new SelectedFileListComp(binding).apply(struc -> struc.get().setMinHeight(200));
        var dragNotice = new LabelComp(AppI18n.observable("dragFiles"))
                .apply(struc -> struc.get().setGraphic(new FontIcon("mdi2e-export")))
                .hide(BindingsHelper.persist(Bindings.isEmpty(stage.getItems())))
                .grow(true, false)
                .apply(struc -> struc.get().setPadding(new Insets(8)));
        var loading = new LoadingOverlayComp(
                new VerticalComp(List.of(list, dragNotice)), PlatformThread.sync(stage.getDownloading()));
        var stack = new StackComp(List.of(backgroundStack, loading)).apply(struc -> {
            struc.get().setOnDragOver(event -> {
                // Accept drops from inside the app window
                if (event.getGestureSource() != null) {
                    event.acceptTransferModes(TransferMode.ANY);
                    event.consume();
                }
            });
            struc.get().setOnDragDropped(event -> {
                if (event.getGestureSource() != null) {
                    var files = FileBrowserClipboard.retrieveDrag(event.getDragboard())
                            .getEntries();
                    stage.drop(files);
                    event.setDropCompleted(true);
                    event.consume();
                }
            });
            struc.get().setOnDragDetected(event -> {
                if (stage.getDownloading().get()) {
                    return;
                }

                var files = stage.getItems().stream()
                        .map(item -> {
                            try {
                                return item.getLocalFile().toRealPath().toFile();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .toList();
                Dragboard db = struc.get().startDragAndDrop(TransferMode.MOVE);
                var cc = new ClipboardContent();
                cc.putFiles(files);
                db.setContent(cc);

                var r = new SelectedFileListComp(FXCollections.observableList(stage.getItems().stream()
                                .map(item -> item.getFileEntry())
                                .toList()))
                        .createRegion();
                new Scene(r);
                WritableImage image = r.snapshot(new SnapshotParameters(), null);
                db.setDragView(image, -20, 15);

                event.setDragDetect(true);
                event.consume();
            });
            struc.get().setOnDragDone(event -> {
                stage.getItems().clear();
                event.consume();
            });
        });
        return stack.createRegion();
    }
}
