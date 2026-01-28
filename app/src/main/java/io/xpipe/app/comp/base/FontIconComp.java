package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionStructure;
import io.xpipe.app.comp.RegionStructureBuilder;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.value.ObservableValue;
import javafx.scene.layout.StackPane;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

@AllArgsConstructor
public class FontIconComp extends RegionStructureBuilder<StackPane, FontIconComp.Structure> {

    private final ObservableValue<String> icon;

    @Override
    public FontIconComp.Structure createBase() {
        var fi = new FontIcon();
        icon.subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                fi.setIconLiteral(val);
            });
        });

        var pane = new StackPane(fi);
        return new FontIconComp.Structure(fi, pane);
    }

    @Value
    public static class Structure implements RegionStructure<StackPane> {

        FontIcon icon;
        StackPane pane;

        @Override
        public StackPane get() {
            return pane;
        }
    }
}
