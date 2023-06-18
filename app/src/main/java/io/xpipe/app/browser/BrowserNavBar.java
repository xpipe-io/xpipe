package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.icon.FileIconManager;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.fxcomps.impl.StackComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

import java.util.List;

public class BrowserNavBar extends SimpleComp {

    private static final PseudoClass INVISIBLE = PseudoClass.getPseudoClass("invisible");

    private final OpenFileSystemModel model;

    public BrowserNavBar(OpenFileSystemModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var path = new SimpleStringProperty(model.getCurrentPath().get());
        SimpleChangeListener.apply(model.getCurrentPath(), (newValue) -> {
            path.set(newValue);
        });
        path.addListener((observable, oldValue, newValue) -> {
            var changed = model.cdOrRetry(newValue, true);
            changed.ifPresent(path::set);
        });

        var pathBar = new TextFieldComp(path, true)
                .styleClass(Styles.RIGHT_PILL)
                .styleClass("path-text")
                .apply(struc -> {
                    SimpleChangeListener.apply(struc.get().focusedProperty(), val -> {
                        struc.get()
                                .pseudoClassStateChanged(
                                        INVISIBLE,
                                        !val && !model.getInOverview().get());
                    });

                    SimpleChangeListener.apply(model.getInOverview(), val -> {
                        struc.get()
                                .pseudoClassStateChanged(
                                        INVISIBLE, !val && !struc.get().isFocused());
                    });

                    struc.get().setOnMouseClicked(event -> {
                        if (struc.get().isFocused()) {
                            return;
                        }

                        struc.get().end();
                        struc.get().selectAll();
                        struc.get().requestFocus();
                    });

                    struc.get().setPromptText("Overview of " + model.getName());
                })
                .shortcut(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), s -> {
                    s.get().requestFocus();
                })
                .accessibleText("Current path");

        var graphic = Bindings.createStringBinding(
                () -> {
                    var icon = model.getCurrentDirectory() != null
                            ? FileIconManager.getFileIcon(model.getCurrentDirectory(), false)
                            : "home_icon.png";
                    return icon;
                },
                model.getCurrentPath());
        var breadcrumbsGraphic = new PrettyImageComp(graphic, 22, 22)
                .padding(new Insets(0, 0, 1, 0))
                .styleClass("path-graphic")
                .createRegion();

        var graphicButton = new Button(null, breadcrumbsGraphic);
        graphicButton.setAccessibleText("Directory options");
        graphicButton.getStyleClass().add(Styles.LEFT_PILL);
        graphicButton.getStyleClass().add("path-graphic-button");
        new ContextMenuAugment<>(event -> event.getButton() == MouseButton.PRIMARY, () -> {
                    return model.getInOverview().get() ? null : new BrowserContextMenu(model, null);
                })
                .augment(new SimpleCompStructure<>(graphicButton));

        var breadcrumbs = new BrowserBreadcrumbBar(model).grow(false, true);

        var stack = new StackComp(List.of(pathBar, breadcrumbs))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .hgrow()
                .apply(struc -> {
                    var t = struc.get().getChildren().get(0);
                    var b = struc.get().getChildren().get(1);
                    b.visibleProperty()
                            .bind(Bindings.createBooleanBinding(
                                    () -> {
                                        return !t.isFocused()
                                                && !model.getInOverview().get();
                                    },
                                    t.focusedProperty(),
                                    model.getInOverview()));
                })
                .grow(false, true);

        var topBox = new HorizontalComp(List.of(Comp.of(() -> graphicButton), stack))
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT))
                .apply(struc -> {
                    ((Region) struc.get().getChildren().get(0))
                            .minHeightProperty()
                            .bind(((Region) struc.get().getChildren().get(1)).heightProperty());
                    ((Region) struc.get().getChildren().get(0))
                            .maxHeightProperty()
                            .bind(((Region) struc.get().getChildren().get(1)).heightProperty());
                })
                .apply(struc -> {
                    struc.get().setPickOnBounds(false);
                })
                .hgrow();

        return topBox.createRegion();
    }
}
