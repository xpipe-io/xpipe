package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.IconButtonComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class StoreSectionBaseComp extends Comp<CompStructure<VBox>> {

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
        return section.getWrapper() != null ? Bindings.createBooleanBinding(
                        () -> {
                            return expanded.get()
                                    && section.getShownChildren().getList().size() > 0;
                        },
                        expanded,
                        section.getShownChildren().getList()) : new SimpleBooleanProperty(true);
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
                section.getWrapper().getColor().subscribe(val -> {
                    var newList = new ArrayList<>(vbox.getStyleClass());
                    newList.removeIf(s -> Arrays.stream(DataColor.values()).anyMatch(dataStoreColor -> dataStoreColor.getId().equals(s)));
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

            section.getWrapper().getPerUser().subscribe(val -> {
                vbox.pseudoClassStateChanged(PseudoClass.getPseudoClass("per-user"), val);
            });
        }
    }

    protected void addVisibilityListeners(VBox root, HBox hbox) {
        var children = new ArrayList<>(hbox.getChildren());
        hbox.getChildren().clear();
        root.visibleProperty().subscribe((newValue) -> {
            if (newValue) {
                hbox.getChildren().addAll(children);
            } else {
                hbox.getChildren().removeAll(children);
            }
        });
    }

    protected ListBoxViewComp<StoreSection> createChildrenList(Function<StoreSection, Comp<?>> function, ObservableBooleanValue hide) {
        var content = new ListBoxViewComp<>(
                section.getShownChildren().getList(),
                section.getAllChildren().getList(),
                (StoreSection e) -> {
                    return function.apply(e).grow(true, false);
                },
                section.getWrapper() == null);
        content.setVisibilityControl(true);
        content.minHeight(0);
        content.hgrow();
        content.styleClass("children-content");
        content.hide(hide);
        return content;
    }

    protected Comp<CompStructure<Button>> createExpandButton(Runnable action, int width, ObservableBooleanValue expanded) {
        var icon = Bindings.createObjectBinding(() -> new LabelGraphic.IconGraphic(
                expanded.get() && section.getShownChildren().getList().size() > 0 ?
                        "mdal-keyboard_arrow_down" :
                        "mdal-keyboard_arrow_right"), expanded, section.getShownChildren().getList());
        var expandButton = new IconButtonComp(icon,
                action);
        expandButton
                .minWidth(width)
                .prefWidth(width)
                .accessibleText(Bindings.createStringBinding(
                        () -> {
                            return "Expand " + section.getWrapper().getName().getValue();
                        },
                        section.getWrapper().getName()))
                .disable(Bindings.size(section.getShownChildren().getList()).isEqualTo(0))
                .styleClass("expand-button")
                .maxHeight(100);
        return expandButton;
    }

    protected Comp<CompStructure<Button>> createQuickAccessButton(int width, Consumer<StoreSection> r) {
        var quickAccessDisabled = Bindings.createBooleanBinding(
                () -> {
                    return section.getShownChildren().getList().isEmpty();
                },
                section.getShownChildren().getList());
        var quickAccessButton = new StoreQuickAccessButtonComp(section, r)
                .styleClass("quick-access-button")
                .minWidth(width)
                .prefWidth(width)
                .maxHeight(100)
                .accessibleText(Bindings.createStringBinding(
                        () -> {
                            return "Access " + section.getWrapper().getName().getValue();
                        },
                        section.getWrapper().getName()))
                .disable(quickAccessDisabled);
        return quickAccessButton;
    }
}
