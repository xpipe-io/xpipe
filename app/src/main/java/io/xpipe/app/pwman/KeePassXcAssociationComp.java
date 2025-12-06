package io.xpipe.app.pwman;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.platform.OptionsBuilder;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class KeePassXcAssociationComp extends SimpleComp {

    private final KeePassXcAssociationKey associationKey;
    private final Runnable onRemove;

    public KeePassXcAssociationComp(KeePassXcAssociationKey associationKey, Runnable onRemove) {this.associationKey = associationKey;
        this.onRemove = onRemove;
    }

    @Override
    protected Region createSimple() {
        var key = associationKey.getKey().getSecretValue();
        var censoredKey = key.substring(0, 6) + "*".repeat(key.length() - 6);

        var nameLabel = new Label(associationKey.getId());
        nameLabel.getStyleClass().add(Styles.TEXT_BOLD);
        nameLabel.setPrefWidth(150);
        var keyLabel = new Label(censoredKey);
        keyLabel.setMaxWidth(2000);
        HBox.setHgrow(keyLabel, Priority.ALWAYS);
        var delButton = new IconButtonComp("mdi2t-trash-can-outline", onRemove).createRegion();
        var box = new HBox(nameLabel, keyLabel, delButton);
        box.setSpacing(8);
        box.setPadding(new Insets(5, 0, 5, 0));
        return box;
    }
}
