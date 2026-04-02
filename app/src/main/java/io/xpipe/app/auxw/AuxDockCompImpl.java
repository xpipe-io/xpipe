package io.xpipe.app.auxw;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.platform.PlatformThread;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AuxDockCompImpl extends SimpleRegionBuilder {

    @Override
    protected Region createSimple() {
        var bar = createBar();
        var content = createContent();
        var vbox = new VBox(bar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        vbox.getStyleClass().add("remote-desktop-dock");
        vbox.focusWithinProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                var w = AppAuxiliaryWindow.get();
                var target = vbox.getScene().getRoot().lookup(".button:hover");
                if (target == null || target.getProperties().get("entry").equals(w.getSelected().getValue())) {
                    Platform.runLater(() -> {
                        w.focus();
                    });
                }
            }
        });
        return vbox;
    }

    private Region createBar() {
        var w = AppAuxiliaryWindow.get();
        var bar = new ToolBar();
        w.getProcesses().addListener((ListChangeListener<? super AuxEntry>) c -> {
            PlatformThread.runLaterIfNeeded(() -> {
                bar.getItems().clear();
                for (var entry : c.getList()) {
                    var b = new Button(entry.getName());
                    b.setGraphic(PrettyImageHelper.ofFixedSizeSquare(entry.getIcon(), 16).build());
                    if (entry.getColor() != null) {
                        b.getStyleClass().add(entry.getColor().getId());
                    }
                    b.getStyleClass().add("color-box");
                    b.getProperties().put("entry", entry);
                    b.setOnAction(event -> {
                        w.select(entry);
                        event.consume();
                    });
                    bar.getItems().add(b);
                }
            });
        });
        w.getSelected().addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                for (Node item : bar.getItems()) {
                    if (item.getProperties().get("entry").equals(newValue)) {
                        item.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
                    } else {
                        item.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), false);
                    }
                }
            });
        });
        return bar;
    }

    private Region createContent() {
        var w = AppAuxiliaryWindow.get();
        var sp = new WindowDockComp<>(w.getModel());
        sp.style("content");
        return sp.build();
    }
}
