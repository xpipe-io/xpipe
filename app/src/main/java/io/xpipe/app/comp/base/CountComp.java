package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;

import java.util.function.Function;

public class CountComp<T> extends Comp<CompStructure<Label>> {

    private final ObservableList<T> sub;
    private final ObservableList<T> all;
    private final Function<String, String> transformation;

    public CountComp(ObservableList<T> sub, ObservableList<T> all) {
        this(sub, all, Function.identity());
    }

    public CountComp(ObservableList<T> sub, ObservableList<T> all, Function<String, String> transformation) {
        this.sub = PlatformThread.sync(sub);
        this.all = PlatformThread.sync(all);
        this.transformation = transformation;
    }

    @Override
    public CompStructure<Label> createBase() {
        var label = new Label();
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setAlignment(Pos.CENTER);
        label.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            if (sub.size() == all.size()) {
                                return transformation.apply(all.size() + "");
                            } else {
                                return transformation.apply(sub.size() + "/" + all.size());
                            }
                        },
                        sub,
                        all));
        label.getStyleClass().add("count-comp");
        return new SimpleCompStructure<>(label);
    }
}
