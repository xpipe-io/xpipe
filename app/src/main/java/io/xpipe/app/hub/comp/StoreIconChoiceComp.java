package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.icon.SystemIcon;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

import static atlantafx.base.theme.Styles.TEXT_SMALL;

public class StoreIconChoiceComp extends ModalOverlayContentComp {

    private final Property<SystemIcon> selected;
    private final Set<SystemIcon> icons;
    private final int columns;
    private final SimpleStringProperty filter;
    private final Runnable doubleClick;
    private final String defaultIcon;

    @Getter
    private final BooleanProperty busy = new SimpleBooleanProperty();

    public StoreIconChoiceComp(
            Property<SystemIcon> selected,
            Set<SystemIcon> icons,
            int columns,
            SimpleStringProperty filter,
            Runnable doubleClick,
            String defaultIcon) {
        this.selected = selected;
        this.icons = icons;
        this.columns = columns;
        this.filter = filter;
        this.doubleClick = doubleClick;
        this.defaultIcon = defaultIcon;
    }

    @Override
    protected void setModalOverlay(ModalOverlay modalOverlay) {
        super.setModalOverlay(modalOverlay);
        if (modalOverlay != null) {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(busy, () -> {
                    SystemIconManager.loadAllAvailableIconImages();
                });
            });
        }
    }

    @Override
    protected Region createSimple() {
        var table = new TableView<List<SystemIcon>>();
        table.visibleProperty().bind(PlatformThread.sync(busy.not()));
        initTable(table);
        filter.addListener((observable, oldValue, newValue) -> updateData(table, newValue));
        busy.addListener((observable, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                PlatformThread.runLaterIfNeeded(() -> {
                    updateData(table, filter.getValue());
                });
            }
        });

        var loading = createLoadingPane();
        var stack = new StackPane();
        stack.getChildren().addAll(table, loading);

        return stack;
    }

    public void refresh() {
        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                SystemIconManager.rebuild();
            });
        });
    }

    private void initTable(TableView<List<SystemIcon>> table) {
        table.setPlaceholder(new Region());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getStyleClass().add("icon-browser");
        table.disableProperty().bind(PlatformThread.sync(busy));

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

    private Region createLoadingPane() {
        var refreshButton = new ButtonComp(
                AppI18n.observable("refreshIcons"),
                new SimpleObjectProperty<>(new LabelGraphic.IconGraphic("mdi2r-refresh")),
                () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        BooleanScope.executeExclusive(busy, () -> {
                            SystemIconManager.rebuild();
                        });
                    });
                });
        refreshButton.hide(Bindings.createBooleanBinding(
                () -> {
                    return SystemIconManager.hasLoadedAnyImages() && !SystemIconManager.isCacheOutdated();
                },
                busy));
        refreshButton.disable(busy);

        var text = new LabelComp(AppI18n.observable("refreshIconsDescription"));
        text.apply(struc -> {
            struc.setWrapText(true);
            struc.setTextAlignment(TextAlignment.CENTER);
            struc.setPrefWidth(300);
        });
        text.style(Styles.TEXT_SUBTLE);
        text.visible(busy);

        var loading = new LoadingIconComp(busy, AppFontSizes::title);
        loading.prefWidth(50);
        loading.prefHeight(50);

        var vbox = new VerticalComp(List.of(text, loading, refreshButton)).spacing(25);
        vbox.apply(struc -> {
            struc.setAlignment(Pos.CENTER);
            struc.setPickOnBounds(false);
        });
        return vbox.build();
    }

    private void updateData(TableView<List<SystemIcon>> table, String filterString) {
        var available = icons.stream()
                .filter(systemIcon -> AppImages.hasImage(
                        "icons/" + systemIcon.getSource().getId() + "/" + systemIcon.getId() + "-40.png"))
                .sorted(Comparator.comparing(systemIcon -> systemIcon.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
        available.addFirst(new SystemIcon(null, "default"));

        List<SystemIcon> shown;
        if (filterString != null
                && !filterString.isBlank()
                && filterString.strip().length() >= 2) {
            shown = available.stream()
                    .filter(icon -> containsString(icon.getId(), filterString.strip()))
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            shown = new ArrayList<>(available);
        }

        var data = partitionList(shown, columns);
        table.getItems().setAll(data);

        var selectMatch = shown.size() == 1
                || shown.stream().anyMatch(systemIcon -> systemIcon.getId().equals(filterString));
        // Table updates seem to not always be instant, sometimes the column is not there yet
        if (selectMatch && table.getColumns().size() > 0) {
            table.getSelectionModel().select(0, table.getColumns().getFirst());
            selected.setValue(shown.getFirst());
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
            Region imageView = PrettyImageHelper.ofFixedSize(image, 40, 40).build();
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

            if (icon.getSource() == null) {
                root.setText(AppI18n.get("default"));
                image.setValue(defaultIcon);
                setGraphic(root);
                return;
            }

            root.setText(icon.getId());
            image.set(SystemIconManager.getAndLoadIconFile(icon, false));
            setGraphic(root);
        }
    }
}
