package io.xpipe.app.action;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ModalOverlayContentComp;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.util.OptionsBuilder;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;

public class ActionPickComp extends ModalOverlayContentComp {

    private final AbstractAction action;

    public ActionPickComp(AbstractAction action) {this.action = action;
    }

    @Override
    protected Region createSimple() {
        var prop = new SimpleObjectProperty<>(action);
        var top = new ActionConfigComp(prop);
        var bottom = new ActionShortcutComp(prop, () -> {
            getModalOverlay().close();
        });
        var options = new OptionsBuilder()
                .addComp(top)
                .addComp(bottom);
        return options.build();
    }
}
