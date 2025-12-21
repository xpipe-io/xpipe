package io.xpipe.app.ext;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;

import javafx.beans.property.Property;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import lombok.AllArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@AllArgsConstructor
public class ShellDialectChoiceComp extends SimpleComp {

    public enum NullHandling {
        NULL_IS_DEFAULT,
        NULL_IS_ALL,
        NULL_DISABLED
    }

    public static final Map<ShellDialect, String> ICONS = new LinkedHashMap<>();

    static {
        ICONS.put(ShellDialects.CMD, "cmd_icon.svg");
        ICONS.put(ShellDialects.POWERSHELL, "powershell_logo.svg");
        ICONS.put(ShellDialects.POWERSHELL_CORE, "pwsh_logo.png");
        ICONS.put(ShellDialects.SH, "sh_icon.svg");
        ICONS.put(ShellDialects.ASH, "sh_icon.svg");
        ICONS.put(ShellDialects.DASH, "sh_icon.svg");
        ICONS.put(ShellDialects.BASH, "bash_icon.svg");
        ICONS.put(ShellDialects.FISH, "fish_icon.svg");
        ICONS.put(ShellDialects.ZSH, "zsh_icon.svg");
        ICONS.put(ShellDialects.NUSHELL, "nushell_icon.svg");
        ICONS.put(ShellDialects.XONSH, "xonsh_icon.png");
    }

    private final List<ShellDialect> available;
    private final Property<ShellDialect> selected;
    private final NullHandling nullHandling;

    public static String getImageName(ShellDialect t) {
        if (t == null) {
            return "proc:defaultShell_icon.svg";
        }

        return "proc:" + ICONS.get(t);
    }

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
                setGraphic(
                        item != null
                                ? PrettyImageHelper.ofFixedSizeSquare("proc:" + ICONS.get(item), 16)
                                        .createRegion()
                                : PrettyImageHelper.ofFixedSizeSquare("proc:defaultShell_icon.svg", 16)
                                        .createRegion());
            }
        };
        var cb = MenuHelper.<ShellDialect>createComboBox();
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
        return cb;
    }
}
