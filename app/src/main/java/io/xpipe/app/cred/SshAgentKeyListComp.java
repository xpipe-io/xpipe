package io.xpipe.app.cred;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class SshAgentKeyListComp extends SimpleRegionBuilder {

    private final ObservableValue<DataStoreEntryRef<ShellStore>> ref;
    private final ObservableValue<? extends SshIdentityStrategy> sshIdentityStrategy;
    private final StringProperty value;

    public SshAgentKeyListComp(ObservableValue<DataStoreEntryRef<ShellStore>> ref, ObservableValue<? extends SshIdentityStrategy> sshIdentityStrategy, StringProperty value) {
        this.ref = ref;
        this.sshIdentityStrategy = sshIdentityStrategy;
        this.value = value;}

    @Override
    protected Region createSimple() {
        var field = new TextFieldComp(value);
        field.apply(struc -> struc.setPromptText(
                "ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIBmhLUTJiP...== <key comment>"));
        var button = new ButtonComp(null, new LabelGraphic.IconGraphic("mdi2m-magnify-scan"), null);
        button.apply(struc -> {
            struc.setOnAction(event -> {
                DataStoreEntryRef<ShellStore> refToUse = ref.getValue() != null ? ref.getValue() : DataStorage.get().local().ref();
                ThreadHelper.runFailableAsync(() -> {
                    var list = SshAgentKeyList.listAgentIdentities(refToUse, sshIdentityStrategy.getValue());
                    Platform.runLater(() -> {
                        var popover = new Popover();
                        popover.setArrowLocation(Popover.ArrowLocation.TOP_CENTER);

                        if (list.size() > 0) {
                            var content = new VBox();
                            content.setPadding(new Insets(10));
                            content.setFillWidth(true);
                            content.getChildren().add(new Label(AppI18n.get("sshAgentHasKeys")));
                            for (SshAgentKeyList.Entry entry : list) {
                                var entryButton = new Button(entry.getName() + " (" + entry.getType() + ")");
                                entryButton.maxWidth(10000);
                                entryButton.getStyleClass().add(Styles.FLAT);
                                entryButton.setOnAction(e -> {
                                    value.setValue(entry.getPublicKey());
                                    popover.hide();
                                    e.consume();
                                });
                                content.getChildren().add(entryButton);
                            }
                            popover.setContentNode(content);
                        } else {
                            popover.setContentNode(new Label(AppI18n.get("sshAgentNoKeys")));
                        }

                        var target = struc.getParent().getChildrenUnmodifiable().getFirst();
                        popover.show(target);
                    });
                });
                event.consume();
            });
        });
        var inputGroup = new InputGroupComp(List.of(field, button));
        inputGroup.setMainReference(field);
        return inputGroup.build();
    }
}
