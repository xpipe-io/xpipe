package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableIntegerValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;

import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public class CountComp extends Comp<CompStructure<Label>> {

    private final ObservableIntegerValue sub;
    private final ObservableIntegerValue all;
    private final Function<String, String> transformation;

    @Override
    public CompStructure<Label> createBase() {
        var label = new Label();
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setAlignment(Pos.CENTER);
        var binding = Bindings.createStringBinding(
                () -> {
                    if (sub.get() == all.get()) {
                        return transformation.apply(all.get() + "");
                    } else {
                        return transformation.apply(sub.get() + "/" + all.get());
                    }
                },
                sub,
                all);
        label.textProperty().bind(PlatformThread.sync(binding));
        label.getStyleClass().add("count-comp");
        return new SimpleCompStructure<>(label);
    }
}
