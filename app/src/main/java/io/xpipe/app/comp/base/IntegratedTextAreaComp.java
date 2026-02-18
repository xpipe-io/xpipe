package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionStructure;
import io.xpipe.app.comp.RegionStructureBuilder;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.ext.StatefulDataStore;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.process.SystemState;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FileOpener;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import lombok.Builder;
import lombok.Value;

public class IntegratedTextAreaComp extends RegionStructureBuilder<AnchorPane, IntegratedTextAreaComp.Structure> {

    private final Property<String> value;
    private final boolean lazy;
    private final String identifier;
    private final ObservableValue<String> fileType;

    public IntegratedTextAreaComp(
            Property<String> value, boolean lazy, String identifier, ObservableValue<String> fileType) {
        this.value = value;
        this.lazy = lazy;
        this.identifier = identifier;
        this.fileType = fileType;
    }

    public static IntegratedTextAreaComp script(
            ObservableValue<DataStoreEntryRef<ShellStore>> host, Property<ShellScript> value) {
        var type = Bindings.createStringBinding(
                () -> {
                    return host.getValue() != null
                                    && host.getValue().getStore() instanceof StatefulDataStore<?> sd
                                    && sd.getState() instanceof SystemState ss
                                    && ss.getShellDialect() != null
                            ? ss.getShellDialect().getScriptFileEnding()
                            : "sh";
                },
                host);
        return script(value, type);
    }

    public static IntegratedTextAreaComp script(Property<ShellScript> value, ObservableValue<String> fileType) {
        var string = new SimpleStringProperty();
        value.subscribe(shellScript -> {
            string.set(shellScript != null ? shellScript.getValue() : null);
        });
        string.addListener((observable, oldValue, newValue) -> {
            value.setValue(newValue != null ? new ShellScript(newValue) : null);
        });
        var i = new IntegratedTextAreaComp(string, false, "script", fileType);
        return i;
    }

    private Region createOpenButton() {
        return new IconButtonComp(
                        "mdal-edit",
                        () -> FileOpener.openString(
                                identifier + (fileType.getValue() != null ? "." + fileType.getValue() : ""),
                                this,
                                value.getValue(),
                                (s) -> {
                                    Platform.runLater(() -> value.setValue(s));
                                }))
                .style("edit-button")
                .apply(struc -> struc.getStyleClass().remove(Styles.FLAT))
                .describe(d -> d.nameKey("edit"))
                .build();
    }

    @Override
    public Structure createBase() {
        var textArea = new TextAreaComp(value, lazy);
        textArea.applyStructure(struc -> {
            struc.getTextArea()
                    .prefRowCountProperty()
                    .bind(Bindings.createIntegerBinding(
                            () -> {
                                var val = value.getValue() != null ? value.getValue() : "";
                                var valCount = (int) val.lines().count() + (val.endsWith("\n") ? 1 : 0);

                                var promptVal = struc.getTextArea().getPromptText() != null ? struc.getTextArea().getPromptText() : "";
                                var promptValCount = (int) promptVal.lines().count() + (promptVal.endsWith("\n") ? 1 : 0);

                                var count = Math.max(valCount, promptValCount);
                                // Somehow the handling of trailing newlines is weird
                                // This makes the handling better for JavaFX text areas
                                count++;
                                return Math.max(1, count);
                            },
                            value,
                            struc.getTextArea().promptTextProperty()));
        });
        var textAreaStruc = textArea.buildStructure();
        var copyButton = createOpenButton();
        var pane = new AnchorPane(textAreaStruc.get(), copyButton);
        pane.setPickOnBounds(false);
        AnchorPane.setTopAnchor(copyButton, 7.0);
        AnchorPane.setRightAnchor(copyButton, 7.0);
        AnchorPane.setLeftAnchor(textAreaStruc.get(), 0.0);
        AnchorPane.setRightAnchor(textAreaStruc.get(), 0.0);
        pane.maxHeightProperty().bind(textAreaStruc.get().heightProperty());
        return new Structure(pane, textAreaStruc.getTextArea());
    }

    @Value
    @Builder
    public static class Structure implements RegionStructure<AnchorPane> {
        AnchorPane pane;
        TextArea textArea;

        @Override
        public AnchorPane get() {
            return pane;
        }
    }
}
