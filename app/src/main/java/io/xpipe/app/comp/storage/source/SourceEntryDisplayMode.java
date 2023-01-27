package io.xpipe.app.comp.storage.source;

import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

public interface SourceEntryDisplayMode {

    SourceEntryDisplayMode LIST = new ListMode();
    SourceEntryDisplayMode TILES = new ListMode();

    public Region create(List<SourceEntryWrapper> entries);

    static class ListMode implements SourceEntryDisplayMode {

        private static final double SOURCE_TYPE_WIDTH = 0.15;
        private static final double NAME_WIDTH = 0.4;
        private static final double STORE_TYPE_WIDTH = 0.1;
        private static final double DETAILS_WIDTH = 0.35;

        @Override
        public Region create(List<SourceEntryWrapper> entries) {
            VBox content = new VBox();

            Runnable updateList = () -> {
                var nw = entries.stream()
                        .map(v -> {
                            return new SourceEntryComp(v).createRegion();
                        })
                        .collect(Collectors.toList());
                content.getChildren().setAll(nw);
            };

            updateList.run();
            content.setFillWidth(true);
            content.setSpacing(5);
            content.getStyleClass().add("content");
            content.getStyleClass().add("list-mode");
            return content;
        }
    }
}
