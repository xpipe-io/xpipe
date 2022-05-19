package io.xpipe.extension.comp;

import io.xpipe.core.source.DataSourceDescriptor;
import io.xpipe.fxcomps.Comp;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DynamicOptionsBuilder<T extends DataSourceDescriptor<?>> {

    private final  List<DynamicOptionsComp.Entry> entries = new ArrayList<>();
    private final List<Property<?>> props = new ArrayList<>();

    public DynamicOptionsBuilder<T> addText(ObservableValue<String> name, Property<String> prop) {
        var comp = new TextField();
        comp.textProperty().bindBidirectional(prop);
        entries.add(new DynamicOptionsComp.Entry(name, Comp.of(() -> comp)));
        return this;
    }

    public Region build(Supplier<T> creator, Property<T> toBind) {
        var bind = Bindings.createObjectBinding(() -> creator.get(), props.toArray(Observable[]::new));
        toBind.bind(bind);
        return new DynamicOptionsComp(entries).createRegion();
    }
}
