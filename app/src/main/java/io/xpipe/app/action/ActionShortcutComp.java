package io.xpipe.app.action;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.*;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ActionShortcutComp extends SimpleComp {

    private final Property<AbstractAction> action;
    private final Runnable onCreateMacro;

    public ActionShortcutComp(Property<AbstractAction> action, Runnable onCreateMacro) {this.action = action;
        this.onCreateMacro = onCreateMacro;
    }

    @Override
    protected Region createSimple() {
        var options = new OptionsBuilder();
        options.nameAndDescription("actionDesktopShortcut")
                .addComp(createDesktopComp());
        options.nameAndDescription("actionUrlShortcut")
                .addComp(createUrlComp());
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
                var file = DesktopShortcuts.createCliOpen(
                        url.getValue(),
                        name.getValue());
                DesktopHelper.browseFileInDirectory(file);
            });
        })
                .grow(false, true)
                .tooltipKey("createShortcut");
        var field = new TextFieldComp(name);
        field.grow(true, false);
        var group = new InputGroupComp(List.of(field, copyButton));
        return group;
    }

    private Comp<?> createMacroComp() {
        var button = new ButtonComp(AppI18n.observable("createMacro"), new FontIcon("mdi2c-clipboard-multiple-outline"), () -> {
            onCreateMacro.run();
        });
        return button;
    }
}
