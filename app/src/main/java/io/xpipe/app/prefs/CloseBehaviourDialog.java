package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.PrefsChoiceValue;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloseBehaviourDialog {

    public static boolean showIfNeeded() {
        if (OperationMode.isInShutdown()) {
            return true;
        }

        boolean set = AppCache.getBoolean("closeBehaviourSet", false);
        if (set) {
            return true;
        }

        Property<CloseBehaviour> prop =
                new SimpleObjectProperty<>(AppPrefs.get().closeBehaviour().getValue());
        var label = new LabelComp(AppI18n.observable("closeBehaviourAlertTitleHeader" ));
        label.apply(struc -> {
            struc.get().setWrapText(true);
        });
        var content =
                new VerticalComp(List.of(label, Comp.of(() -> {
                    ToggleGroup group = new ToggleGroup();
                    var vb = new VBox();
                    vb.setSpacing(7);
                    for (var cb : PrefsChoiceValue.getSupported(CloseBehaviour.class)) {
                        RadioButton rb = new RadioButton(cb.toTranslatedString().getValue());
                        rb.setToggleGroup(group);
                        rb.selectedProperty().addListener((c, o, n) -> {
                            if (n) {
                                prop.setValue(cb);
                            }
                        });
                        if (prop.getValue().equals(cb)) {
                            rb.setSelected(true);
                        }
                        vb.getChildren().add(rb);
                    }
                    return vb;
                }))).spacing(15).prefWidth(500);
        var oked = new AtomicBoolean();
        var modal = ModalOverlay.of("closeBehaviourAlertTitle", content);
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> {
            AppCache.update("closeBehaviourSet", true);
            AppPrefs.get().setFromExternal(AppPrefs.get().closeBehaviour(), prop.getValue());
            oked.set(true);
        }));
        modal.showAndWait();
        return oked.get();
    }
}
