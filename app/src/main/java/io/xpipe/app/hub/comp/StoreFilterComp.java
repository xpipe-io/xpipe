package io.xpipe.app.hub.comp;

import atlantafx.base.controls.CustomTextField;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.RegionDescriptor;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppOpenArguments;
import io.xpipe.app.platform.PlatformThread;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Objects;

public class StoreFilterComp extends RegionBuilder<CustomTextField> {

    private final Property<String> rawText = new SimpleStringProperty();

    private boolean isQuickConnect() {
        var v = rawText.getValue();
        return v != null && (v.startsWith("ssh") || "ssh".startsWith(v));
    }

    private boolean isSearch() {
        return rawText.getValue() != null && rawText.getValue().length() > 1 && !isQuickConnect();
    }

    @Override
    public CustomTextField createSimple() {
        var searchIcon = new FontIcon("mdi2m-magnify");
        var launchIcon = new FontIcon("mdi2p-play");
        var filter = new CustomTextField();
        filter.setMinHeight(0);
        filter.setMaxHeight(20000);
        filter.getStyleClass().add("store-filter-comp");
        filter.promptTextProperty().bind(AppI18n.observable("storeFilterPrompt"));
        filter.rightProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            return filter.isFocused() ? (isQuickConnect() ? launchIcon : isSearch() ? searchIcon : null) : null;
                        },
                        filter.focusedProperty(), rawText));
        RegionDescriptor.builder().nameKey("search").build().apply(filter);

        filter.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (new KeyCodeCombination(KeyCode.ESCAPE).match(event)) {
                filter.clear();
                event.consume();
            } else if (isQuickConnect() && new KeyCodeCombination(KeyCode.ENTER).match(event)) {
                if (StoreQuickConnect.launchQuickConnect(filter.getText())) {
                    filter.clear();
                    event.consume();
                }
            }
        });

        rawText.subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
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

            rawText.setValue(n != null && n.length() > 0 ? n : null);
        });

        return filter;
    }
}
