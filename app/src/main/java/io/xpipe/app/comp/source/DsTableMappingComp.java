package io.xpipe.app.comp.source;

import io.xpipe.core.source.TableMapping;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.impl.LabelComp;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class DsTableMappingComp extends SimpleComp {

    ObservableValue<TableMapping> mapping;

    public DsTableMappingComp(ObservableValue<TableMapping> mapping) {
        this.mapping = PlatformThread.sync(mapping);
    }

    @Override
    protected Region createSimple() {
        var grid = new GridPane();
        grid.getStyleClass().add("table-mapping-comp");

        SimpleChangeListener.apply(mapping, val -> {
            for (int i = 0; i < val.getInputType().getSize(); i++) {
                var input = new LabelComp(val.getInputType().getNames().get(i));
                grid.add(input.createRegion(), 0, i);
                grid.add(new LabelComp("->").createRegion(), 1, i);
                var map = val.map(i).orElse(-1);
                var output =
                        new LabelComp(map != -1 ? val.getOutputType().getNames().get(map) : I18n.get("discarded"));
                grid.add(output.createRegion(), 2, i);

                if (i % 2 != 0) {
                    grid.getChildren().stream().skip((i * 3)).forEach(node -> node.getStyleClass()
                            .add("odd"));
                }
            }
        });

        return grid;
    }
}
