package io.xpipe.app.browser.action;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.util.Shortcuts;
import io.xpipe.app.util.BusyProperty;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;

import java.util.List;
import java.util.function.UnaryOperator;

public interface LeafAction extends BrowserAction {

    public abstract void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception;

    default Button toButton(OpenFileSystemModel model, List<BrowserEntry> selected) {
        var b = new Button();
        b.setOnAction(event -> {
            ThreadHelper.runFailableAsync(() -> {
                BusyProperty.execute(model.getBusy(), () -> {
                    execute(model, selected);
                });
            });
            event.consume();
        });
        if (getShortcut() != null) {
            Shortcuts.addShortcut(b, getShortcut());
        }
        new FancyTooltipAugment<>(new SimpleStringProperty(getName(model, selected))).augment(b);
        var graphic = getIcon(model, selected);
        if (graphic != null) {
            b.setGraphic(graphic);
        }
        b.setMnemonicParsing(false);
        b.setDisable(!isActive(model, selected));
        return b;
    }

    default MenuItem toItem(OpenFileSystemModel model, List<BrowserEntry> selected, UnaryOperator<String> nameFunc) {
        var mi = new MenuItem(nameFunc.apply(getName(model, selected)));
        mi.setOnAction(event -> {
            ThreadHelper.runFailableAsync(() -> {
                BusyProperty.execute(model.getBusy(), () -> {
                    execute(model, selected);
                });
            });
            event.consume();
        });
        if (getShortcut() != null) {
            mi.setAccelerator(getShortcut());
        }
        var graphic = getIcon(model, selected);
        if (graphic != null) {
            mi.setGraphic(graphic);
        }
        mi.setMnemonicParsing(false);
        mi.setDisable(!isActive(model, selected));
        return mi;
    }

    default String getId() {
        return null;
    }
}
