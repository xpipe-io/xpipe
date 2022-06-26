package io.xpipe.extension.comp;

import com.jfoenix.controls.JFXTabPane;
import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.SimpleCompStructure;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class TabPaneComp extends Comp<CompStructure<JFXTabPane>> {

    @Override
    public CompStructure<JFXTabPane> createBase() {
        JFXTabPane tabPane = new JFXTabPane();
        tabPane.getStyleClass().add("tab-pane-comp");

        for (var e : entries) {
            Tab tab = new Tab();
            var ll = new Label(null, new FontIcon(e.graphic()));
            ll.textProperty().bind(e.name());
            ll.getStyleClass().add("name");
            ll.setAlignment(Pos.CENTER);
            tab.setGraphic(ll);
            var content = e.comp().createRegion();
            tab.setContent(content);
            tabPane.getTabs().add(tab);
            content.prefWidthProperty().bind(tabPane.widthProperty());
        }

        return new SimpleCompStructure<>(tabPane);
    }

    private final List<Entry> entries;

    public TabPaneComp(List<Entry> entries) {
        this.entries = entries;
    }

    public static record Entry(ObservableValue<String> name, String graphic, Comp<?> comp) {

    }
}
