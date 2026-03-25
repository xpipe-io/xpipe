package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.RegionDescriptor;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppOpenArguments;
import io.xpipe.app.hub.comp.StoreFilter;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import atlantafx.base.controls.CustomTextField;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Objects;

public class FilterComp extends RegionBuilder<CustomTextField> {

    public static FilterComp ofStoreFilter(Property<StoreFilter> filter) {
        var prop = new SimpleStringProperty();
        prop.subscribe(s -> {
            filter.setValue(StoreFilter.of(s));
        });
        return new FilterComp(prop);
    }

    private final Property<String> filterText;

    public FilterComp(Property<String> filterText) {
        this.filterText = filterText;
    }

    @Override
    public CustomTextField createSimple() {
        var fi = new FontIcon("mdi2m-magnify");
        var clear = new FontIcon("mdi2c-close");
        clear.setCursor(Cursor.DEFAULT);
        clear.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                filterText.setValue(null);
                event.consume();
            }
        });
        var filter = new CustomTextField();
        filter.setMinHeight(0);
        filter.setMaxHeight(20000);
        filter.getStyleClass().add("filter-comp");
        filter.promptTextProperty().bind(AppI18n.observable("searchFilter"));
        filter.rightProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            return filter.isFocused()
                                            || (filter.getText() != null
                                                    && !filter.getText().isEmpty())
                                    ? clear
                                    : fi;
                        },
                        filter.focusedProperty()));
        RegionDescriptor.builder().nameKey("search").build().apply(filter);

        filter.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (new KeyCodeCombination(KeyCode.ESCAPE).match(event)) {
                filter.clear();
                event.consume();
            }
        });

        filterText.subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                clear.setVisible(val != null);
                if (!Objects.equals(filter.getText(), val) && !(val == null && "".equals(filter.getText()))) {
                    filter.setText(val);
                }
            });
        });

        filter.textProperty().addListener((observable, oldValue, n) -> {
            // Handle pasted xpipe URLs
            if (n != null && n.startsWith("xpipe://")) {
                AppOpenArguments.handle(List.of(n));
                filter.setText(null);
                return;
            }

            filterText.setValue(n != null && n.length() > 0 ? n : null);
        });

        // Fix caret not being visible on right side when overflowing
        filter.setSkin(filter.createDefaultSkin());
        Pane pane = (Pane) filter.getChildrenUnmodifiable().getFirst();
        var rec = new Rectangle();
        rec.widthProperty().bind(pane.widthProperty().add(2));
        rec.heightProperty().bind(pane.heightProperty());
        rec.setSmooth(false);
        filter.getChildrenUnmodifiable().getFirst().setClip(rec);

        return filter;
    }
}
