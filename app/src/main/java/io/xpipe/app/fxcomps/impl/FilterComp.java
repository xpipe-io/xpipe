package io.xpipe.app.fxcomps.impl;

import atlantafx.base.controls.CustomTextField;
import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

public class FilterComp extends Comp<CompStructure<CustomTextField>> {

    private final Property<String> filterText;

    public FilterComp(Property<String> filterText) {
        this.filterText = filterText;
    }

    @Override
    public CompStructure<CustomTextField> createBase() {
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
        filter.alignmentProperty().bind(Bindings.createObjectBinding(() -> {
            return filter.isFocused() || (filter.getText() != null && !filter.getText().isEmpty()) ? Pos.CENTER_LEFT : Pos.CENTER;
        }, filter.textProperty(), filter.focusedProperty()));
        filter.setMaxHeight(2000);
        filter.getStyleClass().add("filter-comp");
        filter.promptTextProperty().bind(AppI18n.observable("searchFilter"));
        filter.setLeft(fi);
        filter.setRight(clear);
        filter.setAccessibleText("Filter");

        filterText.subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                clear.setVisible(val != null);
                if (!Objects.equals(filter.getText(), val)) {
                    filter.setText(val);
                }
            });
        });

        filter.textProperty().addListener((observable, oldValue, n) -> {
            // Handle pasted xpipe URLs
            if (n != null && n.startsWith("xpipe://")) {
                AppActionLinkDetector.handle(n, false);
                filter.setText(null);
                return;
            }

            filterText.setValue(n != null && n.length() > 0 ? n : null);
        });

        return new SimpleCompStructure<>(filter);
    }
}
