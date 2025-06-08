package io.xpipe.app.browser.menu;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.action.BrowserActionProviders;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.comp.base.TooltipHelper;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.LicenseProvider;

import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

import lombok.SneakyThrows;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public interface BrowserMenuLeafProvider extends BrowserMenuItemProvider {

    default void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception {
        createAction(model, entries).executeAsync();
    }

    default Class<? extends BrowserActionProvider> getDelegateActionClass() {
        return null;
    }

    @Override
    default boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        if (getDelegateActionClass() != null) {
            var provider = BrowserActionProviders.forClass(getDelegateActionClass());
            return provider.isApplicable(model, entries);
        } else {
            return true;
        }
    }

    @SneakyThrows
    default AbstractAction createAction(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var c = getDelegateActionClass() != null ? getDelegateActionClass() : getActionClass().orElseThrow();
        var bm = c.getDeclaredMethod("builder");
        bm.setAccessible(true);
        var b = bm.invoke(null);

        if (StoreAction.class.isAssignableFrom(c)) {
            var refMethod = b.getClass().getMethod("ref", DataStoreEntryRef.class);
            refMethod.setAccessible(true);
            refMethod.invoke(b, model.getEntry());
        }

        if (BrowserAction.class.isAssignableFrom(c)) {
            var modelMethod = b.getClass().getMethod("model", BrowserFileSystemTabModel.class);
            modelMethod.setAccessible(true);
            modelMethod.invoke(b, model);

            var entriesMethod = b.getClass().getMethod("files", List.class);
            entriesMethod.setAccessible(true);
            entriesMethod.invoke(b, entries.stream().map(browserEntry -> browserEntry.getRawFileEntry().getPath()).toList());
        }

        var m = b.getClass().getDeclaredMethod("build");
        m.setAccessible(true);
        var defValue = c.cast(m.invoke(b));
        return (AbstractAction) defValue;
    }

    default Button toButton(Region root, BrowserFileSystemTabModel model, List<BrowserEntry> selected) {
        var b = new Button();
        b.setOnAction(event -> {
            try {
                execute(model, selected);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            event.consume();
        });
        var name = getName(model, selected);
        Tooltip.install(b, TooltipHelper.create(name, getShortcut()));
        var graphic = getIcon(model, selected);
        if (graphic != null) {
            b.setGraphic(graphic);
        }
        b.setMnemonicParsing(false);
        b.accessibleTextProperty().bind(name);
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (getShortcut() != null && getShortcut().match(event)) {
                b.fire();
                event.consume();
            }
        });

        b.setDisable(!isActive(model, selected));
        model.getCurrentPath().addListener((observable, oldValue, newValue) -> {
            b.setDisable(!isActive(model, selected));
        });

        if (getLicensedFeatureId() != null
                && !LicenseProvider.get().getFeature(getLicensedFeatureId()).isSupported()) {
            b.setDisable(true);
            b.setGraphic(new FontIcon("mdi2p-professional-hexagon"));
        }

        return b;
    }

    default MenuItem toMenuItem(BrowserFileSystemTabModel model, List<BrowserEntry> selected) {
        var name = getName(model, selected);
        var mi = new MenuItem();
        mi.textProperty().bind(BindingsHelper.map(name, s -> {
            if (getLicensedFeatureId() != null) {
                return LicenseProvider.get().getFeature(getLicensedFeatureId()).suffix(s);
            }
            return s;
        }));
        mi.setOnAction(event -> {
            try {
                execute(model, selected);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

        if (getLicensedFeatureId() != null
                && !LicenseProvider.get().getFeature(getLicensedFeatureId()).isSupported()) {
            mi.setDisable(true);
        }

        return mi;
    }
}
