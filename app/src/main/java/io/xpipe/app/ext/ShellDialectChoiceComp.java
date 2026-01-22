package io.xpipe.app.ext;


import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;

import javafx.beans.property.Property;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import lombok.AllArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@AllArgsConstructor
public class ShellDialectChoiceComp extends SimpleRegionBuilder {

    public enum NullHandling {
        NULL_IS_DEFAULT,
        NULL_IS_ALL,
        NULL_DISABLED
    }

    private final List<ShellDialect> available;
    private final Property<ShellDialect> selected;
    private final NullHandling nullHandling;

    @Override
    protected Region createSimple() {
        Supplier<ListCell<ShellDialect>> supplier = () -> new ListCell<>() {
            @Override
            protected void updateItem(ShellDialect item, boolean empty) {
                super.updateItem(item, empty);
                setText(
                        item != null
                                ? item.getDisplayName()
                                : nullHandling == NullHandling.NULL_IS_ALL
                                        ? AppI18n.get("all")
                                        : AppI18n.get("default"));
                setGraphic(PrettyImageHelper.ofFixedSizeSquare(ShellDialectIcons.getImageName(item), 16).build());
            }
        };
        var cb = new ComboBox<ShellDialect>();
        cb.setCellFactory(param -> supplier.get());
        cb.setButtonCell(supplier.get());
        cb.setValue(selected.getValue());
        selected.bind(cb.valueProperty());

        cb.setOnKeyPressed(event -> {
            if (!event.getCode().equals(KeyCode.ENTER)) {
                return;
            }

            cb.show();
            event.consume();
        });

        if (nullHandling != NullHandling.NULL_DISABLED) {
            cb.getItems().add(null);
        }
        cb.getItems().addAll(available);
        cb.setVisibleRowCount(available.size() + 1);
        cb.getStyleClass().add("choice-comp");
        cb.setMaxWidth(20000);
        var skin = new ComboBoxListViewSkin<>(cb);
        cb.setSkin(skin);
        MenuHelper.fixComboBoxSkin(skin);
        return cb;
    }
}
