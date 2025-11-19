package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppOpenArguments;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import atlantafx.base.controls.CustomTextField;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
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
        filter.setAccessibleText("Filter");

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

        return new SimpleCompStructure<>(filter);
    }
}
