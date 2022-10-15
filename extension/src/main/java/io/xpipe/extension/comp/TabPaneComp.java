package io.xpipe.extension.comp;

import com.jfoenix.controls.JFXTabPane;
import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.SimpleCompStructure;
import io.xpipe.fxcomps.util.PlatformThread;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

@Getter
public class TabPaneComp extends Comp<CompStructure<JFXTabPane>> {

    private final Property<Entry> selected;
    private final List<Entry> entries;
    public TabPaneComp(Property<Entry> selected, List<Entry> entries) {
        this.selected = selected;
        this.entries = entries;
    }

    @Override
    public CompStructure<JFXTabPane> createBase() {
        JFXTabPane tabPane = new JFXTabPane();
        tabPane.getStyleClass().add("tab-pane-comp");

        for (var e : entries) {
            Tab tab = new Tab();
            var ll = new Label(null);
            if (e.graphic != null) {
                ll.setGraphic(new FontIcon(e.graphic()));
            }
            ll.textProperty().bind(e.name());
            ll.getStyleClass().add("name");
            ll.setAlignment(Pos.CENTER);
            tab.setGraphic(ll);
            var content = e.comp().createRegion();
            tab.setContent(content);
            tabPane.getTabs().add(tab);
            content.prefWidthProperty().bind(tabPane.widthProperty());
        }

        tabPane.getSelectionModel().select(entries.indexOf(selected.getValue()));
        tabPane.getSelectionModel().selectedIndexProperty().addListener((c, o, n) -> {
            selected.setValue(entries.get(n.intValue()));
        });
        selected.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                tabPane.getSelectionModel().select(entries.indexOf(n));
            });
        });

        return new SimpleCompStructure<>(tabPane);
    }

    public static record Entry(ObservableValue<String> name, String graphic, Comp<?> comp) {}
}
