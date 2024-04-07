package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.StringSource;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class StringSourceComp extends SimpleComp {

    private final Property<DataStoreEntryRef<ShellStore>> fileSystem;
    private final Property<StringSource> stringSource;

    public <T extends ShellStore> StringSourceComp(ObservableValue<DataStoreEntryRef<T>> fileSystem, Property<StringSource> stringSource) {
        this.stringSource = stringSource;
        this.fileSystem = new SimpleObjectProperty<>();
        fileSystem.subscribe(val -> {
            this.fileSystem.setValue(val.get().ref());
        });
    }

    @Override
    protected Region createSimple() {
        var inPlace = new SimpleObjectProperty<>(stringSource.getValue() instanceof StringSource.InPlace i ? i.get() : null);
        var fs = stringSource.getValue() instanceof StringSource.File f ? f.getFile() : null;
        var file = new SimpleObjectProperty<>(stringSource.getValue() instanceof StringSource.File f ? f.getFile().serialize() : null);
        var showText = new SimpleBooleanProperty(inPlace.get() != null);

        var stringField = new TextAreaComp(inPlace);
        stringField.hide(showText.not());
        var fileComp = new ContextualFileReferenceChoiceComp(fileSystem, file);
        fileComp.hide(showText);

        var tr = stringField.createRegion();
        var button = new IconButtonComp("mdi2c-checkbox-marked-outline", () -> {
            showText.set(!showText.getValue());
        }).createRegion();
        AnchorPane.setBottomAnchor(button, 10.0);
        AnchorPane.setRightAnchor(button, 10.0);
        var anchorPane = new AnchorPane(tr, button);
        AnchorPane.setBottomAnchor(tr, 0.0);
        AnchorPane.setTopAnchor(tr, 0.0);
        AnchorPane.setLeftAnchor(tr, 0.0);
        AnchorPane.setRightAnchor(tr, 0.0);

        var fr = fileComp.createRegion();

        return new StackPane(tr, fr);
    }
}
