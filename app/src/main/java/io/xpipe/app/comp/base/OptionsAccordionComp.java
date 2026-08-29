package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.platform.*;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;

import lombok.Getter;
import net.synedra.validatorfx.GraphicDecorationStackPane;

import java.util.List;

@Getter
public class OptionsAccordionComp extends RegionBuilder<Accordion> {

    private final List<Entry> entries;

    public OptionsAccordionComp(List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public Accordion createSimple() {
        var acc = new Accordion();
        for (Entry entry : entries) {
            var r = entry.options.build();
            var val = entry.options.buildEffectiveValidator();
            var prop = val.validationResultProperty();
            var pane = new GraphicDecorationStackPane();
            pane.getChildren().add(r);

            var tp = new TitledPane();
            tp.textProperty().bind(entry.name());
            tp.setContent(pane);
            acc.getPanes().add(tp);

            var check = new Check()
                    .dependsOn("val", prop)
                    .withMethod(c -> {
                        var msgs = prop.getValue().getMessages();
                        if (!msgs.isEmpty()) {
                            c.error(msgs.getFirst().getText());
                        }
                    })
                    .decorates(tp)
                    .immediate();
            BindingsHelper.preserve(tp, check);
        }

        acc.getStyleClass().add("options-accordion-comp");
        acc.setExpandedPane(acc.getPanes().getFirst());
        return acc;
    }

    public record Entry(ObservableValue<String> name, OptionsBuilder options) {}
}
