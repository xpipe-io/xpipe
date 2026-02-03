package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreColor;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class StoreSectionBaseComp extends RegionBuilder<VBox> {

    private static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");
    private static final PseudoClass ROOT = PseudoClass.getPseudoClass("root");
    private static final PseudoClass TOP = PseudoClass.getPseudoClass("top");
    private static final PseudoClass SUB = PseudoClass.getPseudoClass("sub");
    private static final PseudoClass ODD = PseudoClass.getPseudoClass("odd-depth");
    private static final PseudoClass EVEN = PseudoClass.getPseudoClass("even-depth");

    protected final StoreSection section;

    public StoreSectionBaseComp(StoreSection section) {
        this.section = section;
    }

    protected ObservableBooleanValue effectiveExpanded(ObservableBooleanValue expanded) {
        return section.getWrapper() != null
                ? Bindings.createBooleanBinding(
                        () -> {
                            return expanded.get()
                                    && section.getShownChildren().getList().size() > 0;
                        },
                        expanded,
                        section.getShownChildren().getList())
                : new SimpleBooleanProperty(true);
    }

    protected void addPseudoClassListeners(VBox vbox, ObservableBooleanValue expanded) {
        var observable = effectiveExpanded(expanded);
        BindingsHelper.preserve(this, observable);
        observable.subscribe(val -> {
            vbox.pseudoClassStateChanged(EXPANDED, val);
        });

        vbox.pseudoClassStateChanged(EVEN, section.getDepth() % 2 == 0);
        vbox.pseudoClassStateChanged(ODD, section.getDepth() % 2 != 0);
        vbox.pseudoClassStateChanged(ROOT, section.getDepth() == 0);
        vbox.pseudoClassStateChanged(SUB, section.getDepth() > 1);
        vbox.pseudoClassStateChanged(TOP, section.getDepth() == 1);

        if (section.getWrapper() != null) {
            if (section.getDepth() == 1) {
                BindingsHelper.attach(vbox, section.getWrapper().getColor(), val -> {
                    var newList = new ArrayList<>(vbox.getStyleClass());
                    newList.removeIf(s -> Arrays.stream(DataStoreColor.values())
                            .anyMatch(dataStoreColor -> dataStoreColor.getId().equals(s)));
                    newList.remove("gray");
                    newList.add("color-box");
                    if (val != null) {
                        newList.add(val.getId());
                    } else {
                        newList.add("gray");
                    }
                    vbox.getStyleClass().setAll(newList);
                });
            }

            BindingsHelper.attach(vbox, section.getWrapper().getPerUser(), val -> {
                vbox.pseudoClassStateChanged(PseudoClass.getPseudoClass("per-user"), val);
            });
        }
    }

    protected void addVisibilityListeners(VBox root, Pane pane, Supplier<HBox> hbox) {
        AtomicReference<HBox> built = new AtomicReference<>();
        Consumer<Boolean> update = (visible) -> {
            if (visible) {
                // Ignore any changes before this was added to the scene
                if (root.getScene() == null && built.get() == null) {
                    return;
                }

                if (!root.isVisible()) {
                    return;
                }

                if (built.get() == null) {
                    built.set(hbox.get());
                }

                pane.getChildren().setAll(built.get());
            } else {
                if (root.isVisible()) {
                    return;
                }

                pane.getChildren().clear();
            }
        };

        root.visibleProperty().subscribe((newValue) -> {
            if (root.getScene() == null) {
                update.accept(newValue);
            } else {
                Platform.runLater(() -> {
                    update.accept(newValue);
                });
            }
        });
    }

    protected ListBoxViewComp<StoreSection> createChildrenList(
            Function<StoreSection, BaseRegionBuilder<?, ?>> function, ObservableBooleanValue hide) {
        var content = new ListBoxViewComp<>(
                section.getShownChildren().getList(),
                section.getAllChildren().getList(),
                function,
                section.getWrapper() == null);
        content.setVisibilityControl(true);
        content.minHeight(0);
        content.hgrow();
        content.style("children-content");
        content.hide(hide);
        content.apply(struc -> struc.setFocusTraversable(false));
        return content;
    }

    protected RegionBuilder<Button> createExpandButton(Runnable action, int width, ObservableBooleanValue expanded) {
        var icon = Bindings.createObjectBinding(
                () -> new LabelGraphic.IconGraphic(
                        expanded.get() && section.getShownChildren().getList().size() > 0
                                ? "mdal-keyboard_arrow_down"
                                : "mdal-keyboard_arrow_right"),
                expanded,
                section.getShownChildren().getList());
        var expandButton = new IconButtonComp(icon, action);
        expandButton
                .minWidth(width)
                .prefWidth(width)
                .describe(d -> d.nameKey("expand"))
                .disable(Bindings.size(section.getShownChildren().getList()).isEqualTo(0))
                .style("expand-button")
                .maxHeight(100);
        return expandButton;
    }

    protected RegionBuilder<Button> createQuickAccessButton(int width, Consumer<StoreSection> r) {
        var quickAccessDisabled = Bindings.createBooleanBinding(
                () -> {
                    return section.getShownChildren().getList().isEmpty();
                },
                section.getShownChildren().getList());
        var quickAccessButton = new StoreQuickAccessButtonComp(section, r)
                .style("quick-access-button")
                .minWidth(width)
                .prefWidth(width)
                .maxHeight(100)
                .describe(d -> d.nameKey("quickAccess"))
                .disable(quickAccessDisabled);
        return quickAccessButton;
    }
}
