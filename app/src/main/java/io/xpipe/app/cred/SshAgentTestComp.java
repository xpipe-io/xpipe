package io.xpipe.app.cred;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class SshAgentTestComp extends SimpleRegionBuilder {

    private final ObservableValue<? extends SshIdentityStrategy> sshIdentityStrategy;

    public SshAgentTestComp(ObservableValue<? extends SshIdentityStrategy> sshIdentityStrategy) {this.sshIdentityStrategy = sshIdentityStrategy;}

    @Override
    protected Region createSimple() {
        var button = new ButtonComp(AppI18n.observable("test"), new LabelGraphic.IconGraphic("mdi2p-play"), null);
        button.apply(struc -> {
            struc.setOnAction(event -> {
                DataStoreEntryRef<ShellStore> refToUse = DataStorage.get().local().ref();
                ThreadHelper.runFailableAsync(() -> {
                    var list = SshAgentKeyList.listAgentIdentities(refToUse, sshIdentityStrategy.getValue());
                    Platform.runLater(() -> {
                        var popover = new Popover();
                        popover.setArrowLocation(Popover.ArrowLocation.LEFT_CENTER);

                        if (list.size() > 0) {
                            var content = new VBox();
                            content.setPadding(new Insets(10));
                            content.setFillWidth(true);
                            content.getChildren().add(new Label(AppI18n.get("sshAgentHasKeys")));
                            for (SshAgentKeyList.Entry entry : list) {
                                var entryButton = new Button(entry.getName() + " (" + entry.getType() + ")");
                                entryButton.maxWidth(10000);
                                entryButton.getStyleClass().add(Styles.FLAT);
                                content.getChildren().add(entryButton);
                            }
                            popover.setContentNode(content);
                        } else {
                            popover.setContentNode(new Label(AppI18n.get("sshAgentNoKeys")));
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
