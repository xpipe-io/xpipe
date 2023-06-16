package io.xpipe.app.comp.storage.collection;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.storage.source.SourceEntryListComp;
import io.xpipe.app.comp.storage.source.SourceEntryListHeaderComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class SourceCollectionLayoutComp extends SimpleComp {

    private Comp<?> createEntries(SourceCollectionWrapper group, Region groupHeader) {
        var entryList = new SourceEntryListComp(group);
        var entriesHeader = new SourceEntryListHeaderComp(group);
        entriesHeader.apply(r -> r.get().minHeightProperty().bind(groupHeader.heightProperty()));
        var entriesHeaderWrapped = Comp.derive(entriesHeader, r -> {
            var sp = new StackPane(r);
            sp.setPadding(new Insets(0, 5, 5, 5));
            return sp;
        });

        var list = new ArrayList<Comp<?>>(List.of(entriesHeaderWrapped));
        list.add(entryList);
        entryList.apply(s -> VBox.setVgrow(s.get(), Priority.ALWAYS));

        return new VerticalComp(list);
    }

    private Comp<?> createCollectionList() {
        var listComp = new SourceCollectionListComp();
        listComp.apply(s -> s.get().setPrefHeight(Region.USE_COMPUTED_SIZE));
        return listComp;
    }

    private Comp<?> createFiller() {
        var filler = Comp.of(() -> new Region());
        filler.styleClass("bar");
        filler.styleClass("filler-bar");
        var button = new ButtonComp(
                        AppI18n.observable("addCollection"), new FontIcon("mdi2f-folder-plus-outline"), () -> {
                            SourceCollectionViewState.get().addNewCollection();
                        })
                .apply(new FancyTooltipAugment<>("addCollectionFolder"));
        button.styleClass("intro-add-collection-button");

        var pane = Comp.derive(button, r -> {
            var sp = new StackPane(r);
            sp.setAlignment(Pos.CENTER);
            sp.setPickOnBounds(false);
            return sp;
        });
        pane.apply(r -> {
            r.get().visibleProperty().bind(SourceCollectionViewState.get().getStorageEmpty());
            r.get()
                    .mouseTransparentProperty()
                    .bind(BindingsHelper.persist(
                            Bindings.not(SourceCollectionViewState.get().getStorageEmpty())));
        });

        var stack = new StackComp(List.of(filler, pane));
        stack.apply(s -> {
            s.get().setMinHeight(0);
            s.get().setPrefHeight(0);
        });
        return stack;
    }

    @Override
    protected Region createSimple() {
        var listComp = createCollectionList();
        var r = new BorderPane();

        var listR = listComp.createRegion();
        var groupHeader = new SourceCollectionFilterBarComp().createRegion();
        var filler = createFiller().createRegion();
        var groups = new VBox(groupHeader, listR);
        groups.getStyleClass().add("sidebar");
        VBox.setVgrow(filler, Priority.SOMETIMES);
        VBox.setVgrow(listR, Priority.SOMETIMES);
        r.setLeft(groups);

        Runnable update = () -> {
            r.setCenter(createEntries(SourceCollectionViewState.get().getSelectedGroup(), groupHeader)
                    .createRegion());
        };
        update.run();
        SourceCollectionViewState.get().selectedGroupProperty().addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(update);
        });

        return r;
    }
}
