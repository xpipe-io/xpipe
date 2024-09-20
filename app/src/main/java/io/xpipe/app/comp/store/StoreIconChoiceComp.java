package io.xpipe.app.comp.store;

import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.resources.SystemIcon;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Tweaks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static atlantafx.base.theme.Styles.TEXT_SMALL;

public class StoreIconChoiceComp extends SimpleComp {

    private final Property<SystemIcon> selected;
    private final List<SystemIcon> icons;
    private final int columns;
    private final SimpleStringProperty filter;
    private final Runnable doubleClick;

    public StoreIconChoiceComp(
            Property<SystemIcon> selected,
            List<SystemIcon> icons,
            int columns,
            SimpleStringProperty filter,
            Runnable doubleClick) {
        this.selected = selected;
        this.icons = icons;
        this.columns = columns;
        this.filter = filter;
        this.doubleClick = doubleClick;
    }

    @Override
    protected Region createSimple() {
        var table = new TableView<List<SystemIcon>>();
        initTable(table);
        updateData(table, null);
        filter.addListener((observable, oldValue, newValue) -> updateData(table, newValue));
        return table;
    }

    private void initTable(TableView<List<SystemIcon>> table) {
        for (int i = 0; i < columns; i++) {
            var col = new TableColumn<List<SystemIcon>, SystemIcon>("col" + i);
            final int colIndex = i;
            col.setCellValueFactory(cb -> {
                var row = cb.getValue();
                var item = row.size() > colIndex ? row.get(colIndex) : null;
                return new SimpleObjectProperty<>(item);
            });
            col.setCellFactory(cb -> new IconCell());
            col.getStyleClass().add(Tweaks.ALIGN_CENTER);
            table.getColumns().add(col);
        }

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getStyleClass().add("icon-browser");
    }

    private void updateData(TableView<List<SystemIcon>> table, String filterString) {
        var displayedIcons = filterString == null || filterString.isBlank() || filterString.length() < 2
                ? icons
                : icons.stream()
                        .filter(icon -> containsString(icon.getDisplayName(), filterString))
                        .toList();

        var data = partitionList(displayedIcons, columns);
        table.getItems().setAll(data);
    }

    private <T> Collection<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        if (list.size() == 0) {
            return partitions;
        }

        int length = list.size();
        int numOfPartitions = length / size + ((length % size == 0) ? 0 : 1);

        for (int i = 0; i < numOfPartitions; i++) {
            int from = i * size;
            int to = Math.min((i * size + size), length);
            partitions.add(list.subList(from, to));
        }
        return partitions;
    }

    private boolean containsString(String s1, String s2) {
        return s1.toLowerCase(Locale.ROOT).contains(s2.toLowerCase(Locale.ROOT));
    }

    public class IconCell extends TableCell<List<SystemIcon>, SystemIcon> {

        private final Label root = new Label();
        private final StringProperty image = new SimpleStringProperty();

        public IconCell() {
            super();

            root.setContentDisplay(ContentDisplay.TOP);
            Region imageView = PrettyImageHelper.ofFixedSize(image, 40, 40).createRegion();
            root.setGraphic(imageView);
            root.setGraphicTextGap(10);
            root.getStyleClass().addAll("icon-label", TEXT_SMALL);

            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    selected.setValue(getItem());
                }

                if (event.getClickCount() > 1) {
                    doubleClick.run();
                }
            });
        }

        @Override
        protected void updateItem(SystemIcon icon, boolean empty) {
            super.updateItem(icon, empty);

            if (icon == null) {
                setGraphic(null);
                return;
            }

            root.setText(icon.getDisplayName());
            image.set(icon.getIconName() + ".svg");
            setGraphic(root);
        }
    }
}
