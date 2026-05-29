package io.xpipe.app.action;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.ClipboardHelper;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.*;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Region;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ActionShortcutComp extends SimpleRegionBuilder {

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
        options.nameAndDescription("actionCommand").addComp(createCommandComp());
        options.name(AppDistributionType.get().isSupportsUrls() ? "actionUrlShortcut" : "actionUrlShortcutDisabled");
        options.description(
                AppDistributionType.get().isSupportsUrls()
                        ? AppI18n.observable("actionUrlShortcutDescription")
                        : AppI18n.observable(
                                "actionUrlShortcutDisabledDescription",
                                AppDistributionType.get().toTranslatedString().getValue()));
        options.addComp(createUrlComp()).disable(!AppDistributionType.get().isSupportsUrls());
        options.nameAndDescription("actionApiCall").addComp(createApiComp());
        return options.build();
    }

    private BaseRegionBuilder<?, ?> createUrlComp() {
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
                .describe(d -> d.nameKey("copyUrl"));
        var field = new TextFieldComp(url);
        field.apply(struc -> struc.setEditable(false));
        var group = new InputGroupComp(List.of(field, copyButton));
        group.setMainReference(field);
        group.hide(Bindings.isNull(url));
        return group;
    }


    private BaseRegionBuilder<?, ?> createCommandComp() {
        var command = new SimpleStringProperty();
        action.subscribe((v) -> {
            var s = ActionUrls.toUrl(v);
            ThreadHelper.runFailableAsync(() -> {
                var exec =  AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe";
                var inPath = LocalShell.getShell().view().findProgram(exec).isPresent();
                var defaultDataDir = AppProperties.get().getDefaultDataDir().equals(AppProperties.get().getDataDir());
                var c = (inPath ? exec : "\"" + AppInstallation.ofCurrent().getCliExecutablePath() + "\"") + " open \""
                        + s + "\"" + (!defaultDataDir ? " -d \"" + AppProperties.get().getDataDir() + "\"" : "");
                Platform.runLater(() -> {
                    command.set(c);
                });
            });
        });

        var copyButton = new ButtonComp(null, new FontIcon("mdi2c-clipboard-multiple-outline"), () -> {
                    ClipboardHelper.copyUrl(command.getValue());
                })
                .describe(d -> d.nameKey("copy"));
        var field = new TextFieldComp(command);
        field.apply(struc -> struc.setEditable(false));
        var group = new InputGroupComp(List.of(field, copyButton));
        group.setMainReference(field);
        group.hide(Bindings.isNull(command));
        return group;
    }

    private BaseRegionBuilder<?, ?> createDesktopComp() {
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
                        var file = DesktopShortcuts.createOpen(
                                name.getValue(),
                                "open \"" + url.getValue() + "\" -d \""
                                        + AppProperties.get().getDataDir() + "\"",
                                null);
                        DesktopHelper.browseFileInDirectory(file);
                    });
                })
                .describe(d -> d.nameKey("createShortcut"));
        var field = new TextFieldComp(name);
        var group = new InputGroupComp(List.of(field, copyButton));
        group.setMainReference(field);
        group.hide(BindingsHelper.map(action, v -> !(v instanceof SerializableAction)));
        return group;
    }

    private BaseRegionBuilder<?, ?> createApiComp() {
        var url = "curl -X POST \"http://localhost:" + AppBeaconServer.get().getPort() + "/action\" ...";
        var text = AppI18n.observable("actionApiUrl", url);
        var prop = new SimpleStringProperty();
        prop.bind(text);

        var copyButton = new ButtonComp(null, new FontIcon("mdi2c-clipboard-multiple-outline"), () -> {
                    if (action.getValue() instanceof SerializableAction sa) {
                        ClipboardHelper.copyUrl(sa.toNode().toPrettyString());
                    }
                })
                .describe(d -> d.nameKey("copyBody"));
        var field = new TextFieldComp(prop, true);
        field.apply(struc -> struc.setEditable(false));
        var group = new InputGroupComp(List.of(field, copyButton));
        group.setMainReference(field);
        group.hide(BindingsHelper.map(action, v -> !(v instanceof SerializableAction)));
        return group;
    }
}
