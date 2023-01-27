package io.xpipe.app.comp.storage.source;

import io.xpipe.app.comp.base.FileDropOverlayComp;
import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.comp.storage.collection.SourceCollectionEmptyIntroComp;
import io.xpipe.app.comp.storage.collection.SourceCollectionViewState;
import io.xpipe.app.comp.storage.collection.SourceCollectionWrapper;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Map;

public class SourceEntryListComp extends SimpleComp {

    private final SourceCollectionWrapper group;

    public SourceEntryListComp(SourceCollectionWrapper group) {
        this.group = group;
    }

    @SuppressWarnings("unchecked")
    private Region createList() {
        if (group == null) {
            return null;
        }

        var content =
                group.getDisplayMode().create(SourceCollectionViewState.get().getShownEntries());
        var cp = new ScrollPane(content);
        cp.setFitToWidth(true);
        content.getStyleClass().add("content-pane");
        cp.getStyleClass().add("storage-entry-list-comp");

        SourceCollectionViewState.get().getShownEntries().addListener((ListChangeListener<? super SourceEntryWrapper>)
                (c) -> {
                    Platform.runLater(() -> {
                        cp.setContent(group.getDisplayMode().create((List<SourceEntryWrapper>) c.getList()));
                    });
                });

        return cp;
    }

    @Override
    protected Region createSimple() {
        Map<Comp<?>, ObservableBooleanValue> map;
        if (group == null) {
            map = Map.of(
                    new SourceStorageEmptyIntroComp(),
                    SourceCollectionViewState.get().getStorageEmpty());
        } else {
            map = Map.of(
                    Comp.of(() -> createList()),
                    group.emptyProperty().not(),
                    new SourceCollectionEmptyIntroComp(),
                    group.emptyProperty());
        }

        var overlay = new FileDropOverlayComp<>(new MultiContentComp(map), files -> {
            files.forEach(group::dropFile);
        });
        return overlay.createRegion();
    }
}
