package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.icon.SystemIcon;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;

import java.util.*;

import static atlantafx.base.theme.Styles.TEXT_SMALL;

public class StoreIconChoiceComp extends SimpleComp {

    private final Runnable reshow;
    private final Property<SystemIcon> selected;
    private final Set<SystemIcon> icons;
    private final int columns;
    private final SimpleStringProperty filter;
    private final Runnable doubleClick;

    public StoreIconChoiceComp(
            Runnable reshow,
            Property<SystemIcon> selected,
            Set<SystemIcon> icons,
            int columns,
            SimpleStringProperty filter,
            Runnable doubleClick) {
        this.reshow = reshow;
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
        if (!SystemIconManager.isCacheOutdated()) {
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
        }

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getStyleClass().add("icon-browser");

        var busy = new SimpleBooleanProperty(false);
        var refreshButton = new ButtonComp(
                AppI18n.observable("refreshIcons"),
                new SimpleObjectProperty<>(new LabelGraphic.IconGraphic("mdi2r-refresh")),
                () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        try (var ignored = new BooleanScope(busy).start()) {
                            SystemIconManager.reload();
                        }
                        reshow.run();
                    });
                });
        refreshButton.disable(busy);
        var text = new LabelComp(AppI18n.observable("refreshIconsDescription"));
        text.apply(struc -> {
            struc.get().setWrapText(true);
            struc.get().setTextAlignment(TextAlignment.CENTER);
            struc.get().setPrefWidth(300);
        });
        text.styleClass(Styles.TEXT_SUBTLE);
        text.visible(busy);
        var vbox = new VerticalComp(List.of(refreshButton, text)).spacing(25);
        vbox.apply(struc -> struc.get().setAlignment(Pos.CENTER));
        var placeholder = new StackPane(vbox.createRegion());
        table.setPlaceholder(placeholder);
    }

    private void updateData(TableView<List<SystemIcon>> table, String filterString) {
        if (SystemIconManager.isCacheOutdated()) {
            table.getItems().clear();
            return;
        }

        var available = icons.stream()
                .filter(systemIcon -> AppImages.hasNormalImage(
                        "icons/" + systemIcon.getSource().getId() + "/" + systemIcon.getId() + "-40.png"))
                .sorted(Comparator.comparing(systemIcon -> systemIcon.getId()))
                .toList();
        table.getPlaceholder().setVisible(available.size() == 0);
        var filtered = available;
        if (filterString != null && !filterString.isBlank() && filterString.length() >= 2) {
            filtered = available.stream()
                    .filter(icon -> containsString(icon.getId(), filterString))
                    .toList();
        }
        var data = partitionList(filtered, columns);
        table.getItems().setAll(data);

        var selectMatch = filtered.size() == 1
                || filtered.stream().anyMatch(systemIcon -> systemIcon.getId().equals(filterString));
        // Table updates seem to not always be instant, sometimes the column is not there yet
        if (selectMatch && table.getColumns().size() > 0) {
            table.getSelectionModel().select(0, table.getColumns().getFirst());
            selected.setValue(filtered.getFirst());
        } else {
            selected.setValue(null);
        }
    }

    private <T> List<List<T>> partitionList(List<T> list, int size) {
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

            root.setText(icon.getId());
            image.set(SystemIconManager.getIconFile(icon));
            setGraphic(root);
        }
    }
}
