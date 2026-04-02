package io.xpipe.app.auxw;

import io.xpipe.app.comp.SimpleRegionBuilder;
import javafx.collections.ListChangeListener;
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
        return vbox;
    }

    private Region createBar() {
        var w = AppAuxiliaryWindow.get();
        var bar = new ToolBar();
        w.getProcesses().addListener((ListChangeListener<? super AuxEntry>) c -> {
            bar.getItems().clear();
            for (var entry : c.getList()) {
                var b = new Button(entry.getName());
                b.setOnAction(event -> {
                    w.select(entry);
                    event.consume();
                });
                bar.getItems().add(b);
            }
        });
        return bar;
    }

    private Region createContent() {
        var w = AppAuxiliaryWindow.get();
        var sp = new WindowDockComp<>(w.getModel());
        return sp.build();
    }
}
