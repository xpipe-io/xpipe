package io.xpipe.ext.proc.store;

import io.xpipe.core.process.ShellType;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.util.CustomComboBoxBuilder;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Map;

@AllArgsConstructor
public class ShellTypeChoiceComp extends SimpleComp {

    public static final Map<ShellType, String> ICONS = Map.of(
            ShellTypes.CMD, "cmd.png",
            ShellTypes.POWERSHELL, "powershell.png",
            ShellTypes.ZSH, "cmd.png",
            ShellTypes.SH, "cmd.png",
            ShellTypes.BASH, "cmd.png");

    private final Property<ShellType> selected;

    private Region createGraphic(ShellType s) {
        if (s == null) {
            return createEmptyGraphic();
        }

        var img = XPipeDaemon.getInstance().image("base:" + ICONS.get(s));
        var imgView = new ImageView(img);
        imgView.setFitWidth(16);
        imgView.setFitHeight(16);

        var name = s.getDisplayName();

        return new Label(name, imgView);
    }

    private Region createEmptyGraphic() {
        var img = XPipeDaemon.getInstance().image("proc:defaultShell_icon.png");
        var imgView = new ImageView(img);
        imgView.setFitWidth(16);
        imgView.setFitHeight(16);

        return new Label(I18n.get("default"), imgView);
    }

    @Override
    protected Region createSimple() {
        var comboBox = new CustomComboBoxBuilder<>(selected, this::createGraphic, null, e -> true);
        comboBox.add(null);
        Arrays.stream(ShellTypes.getAllShellTypes()).forEach(shellType -> comboBox.add(shellType));
        ComboBox<Node> cb = comboBox.build();
        cb.getStyleClass().add("choice-comp");
        cb.setMaxWidth(2000);
        return cb;
    }
}
