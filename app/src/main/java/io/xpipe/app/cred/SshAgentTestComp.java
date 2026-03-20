package io.xpipe.app.cred;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;

public class SshAgentTestComp extends SimpleRegionBuilder {

    private final Runnable beforeTest;
    private final ObservableValue<? extends SshIdentityAgentStrategy> sshIdentityStrategy;

    public SshAgentTestComp(
            Runnable beforeTest, ObservableValue<? extends SshIdentityAgentStrategy> sshIdentityStrategy) {
        this.beforeTest = beforeTest;
        this.sshIdentityStrategy = sshIdentityStrategy;
    }

    @Override
    protected Region createSimple() {
        var button = new ButtonComp(AppI18n.observable("test"), new LabelGraphic.IconGraphic("mdi2p-play"), null);
        button.padding(new Insets(6, 9, 6, 9));
        button.apply(struc -> {
            struc.setOnAction(event -> {
                DataStoreEntryRef<ShellStore> refToUse =
                        DataStorage.get().local().ref();
                ThreadHelper.runFailableAsync(() -> {
                    beforeTest.run();
                    var list = SshAgentKeyList.listAgentIdentities(refToUse, sshIdentityStrategy.getValue());
                    Platform.runLater(() -> {
                        var popover = new Popover();
                        popover.setArrowLocation(Popover.ArrowLocation.LEFT_CENTER);

                        if (list.size() > 0) {
                            var content = new VBox();
                            content.setPadding(new Insets(10));
                            content.setFillWidth(true);
                            var header = new Label(AppI18n.get("sshAgentHasKeys"));
                            header.setPadding(new Insets(0, 0, 8, 8));
                            content.getChildren().add(header);
                            for (SshAgentKeyList.Entry entry : list) {
                                var buttonName = entry.getType() + " "
                                        + (entry.getName() != null ? entry.getName() : entry.getPublicKey());
                                var entryButton = new Button(buttonName);
                                entryButton.setMaxWidth(400);
                                entryButton.getStyleClass().add(Styles.FLAT);
                                entryButton.setMinWidth(400);
                                entryButton.setAlignment(Pos.CENTER_LEFT);
                                entryButton.setMnemonicParsing(false);
                                content.getChildren().add(entryButton);
                            }
                            popover.setContentNode(content);
                        } else {
                            var content = new Label(AppI18n.get("sshAgentNoKeys"));
                            content.setPadding(new Insets(10));
                            popover.setContentNode(content);
                        }

                        var target = struc;
                        popover.show(target);
                    });
                });
                event.consume();
            });
        });
        return button.build();
    }
}
