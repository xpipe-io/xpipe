package io.xpipe.app.action;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.ClipboardHelper;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.*;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Region;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ActionShortcutComp extends SimpleComp {

    private final Property<AbstractAction> action;
    private final Runnable onCreateMacro;

    public ActionShortcutComp(Property<AbstractAction> action, Runnable onCreateMacro) {
        this.action = action;
        this.onCreateMacro = onCreateMacro;
    }

    @Override
    protected Region createSimple() {
        var options = new OptionsBuilder();
        options.nameAndDescription("actionDesktopShortcut").addComp(createDesktopComp());
        options.name(AppDistributionType.get().isSupportsUrls() ? "actionUrlShortcut" : "actionUrlShortcutDisabled");
        options.description(
                AppDistributionType.get().isSupportsUrls()
                        ? AppI18n.observable("actionUrlShortcutDescription")
                        : AppI18n.observable(
                                "actionUrlShortcutDisabledDescription",
                                AppDistributionType.get().toTranslatedString().getValue()));
        options.addComp(createUrlComp()).disable(!AppDistributionType.get().isSupportsUrls());
        options.nameAndDescription("actionApiCall").addComp(createApiComp());
        //        options.nameAndDescription("actionMacro")
        //                .addComp(createMacroComp());
        return options.build();
    }

    private Comp<?> createUrlComp() {
        var url = new SimpleStringProperty();
        action.subscribe((v) -> {
            var s = ActionUrls.toUrl(v);
            PlatformThread.runLaterIfNeeded(() -> {
                url.set(s);
            });
        });

        var copyButton = new ButtonComp(null, new FontIcon("mdi2c-clipboard-multiple-outline"), () -> {
                    ClipboardHelper.copyUrl(url.getValue());
                })
                .grow(false, true)
                .tooltipKey("createShortcut");
        var field = new TextFieldComp(url);
        field.grow(true, false);
        field.apply(struc -> struc.get().setEditable(false));
        var group = new InputGroupComp(List.of(field, copyButton));
        group.setMainReference(copyButton);
        group.hide(Bindings.isNull(url));
        return group;
    }

    private Comp<?> createDesktopComp() {
        var url = BindingsHelper.map(action, abstractAction -> ActionUrls.toUrl(abstractAction));
        var name = new SimpleStringProperty();
        action.subscribe((v) -> {
            var s = v.getShortcutName();
            PlatformThread.runLaterIfNeeded(() -> {
                name.set(s);
            });
        });
        var copyButton = new ButtonComp(null, new FontIcon("mdi2f-file-move-outline"), () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        var file =
                                DesktopShortcuts.createOpen(name.getValue(), "open \"" + url.getValue() + "\"", null);
                        DesktopHelper.browseFileInDirectory(file);
                    });
                })
                .grow(false, true)
                .tooltipKey("createShortcut");
        var field = new TextFieldComp(name);
        field.grow(true, false);
        var group = new InputGroupComp(List.of(field, copyButton));
        group.setMainReference(copyButton);
        group.hide(BindingsHelper.map(action, v -> !(v instanceof SerializableAction)));
        return group;
    }

    private Comp<?> createApiComp() {
        var url = "curl -X POST \"http://localhost:" + AppBeaconServer.get().getPort() + "/action\" ...";
        var text = AppI18n.observable("actionApiUrl", url);
        var prop = new SimpleStringProperty();
        prop.bind(text);

        var copyButton = new ButtonComp(null, new FontIcon("mdi2c-clipboard-multiple-outline"), () -> {
                    if (action.getValue() instanceof SerializableAction sa) {
                        ClipboardHelper.copyUrl(sa.toNode().toPrettyString());
                    }
                })
                .grow(false, true)
                .tooltipKey("copyBody");
        var field = new TextFieldComp(prop, true);
        field.grow(true, false);
        field.apply(struc -> struc.get().setEditable(false));
        var group = new InputGroupComp(List.of(field, copyButton));
        group.setMainReference(copyButton);
        group.hide(BindingsHelper.map(action, v -> !(v instanceof SerializableAction)));
        return group;
    }

    @SuppressWarnings("unused")
    private Comp<?> createMacroComp() {
        var button = new ButtonComp(
                AppI18n.observable("createMacro"), new FontIcon("mdi2c-clipboard-multiple-outline"), onCreateMacro);
        return button;
    }
}
