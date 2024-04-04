package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.comp.base.LazyTextFieldComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.store.StoreCategoryWrapper;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.util.ListBindingsHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.util.ContextMenuHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@EqualsAndHashCode(callSuper = true)
@Value
public class StoreCategoryComp extends SimpleComp {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    StoreCategoryWrapper category;

    @Override
    protected Region createSimple() {
        var i = Bindings.createStringBinding(
                () -> {
                    if (!DataStorage.get().supportsSharing()
                            || !category.getCategory().canShare()) {
                        return "mdal-keyboard_arrow_right";
                    }

                    return category.getShare().getValue() ? "mdi2a-account-convert" : "mdi2a-account-cancel";
                },
                category.getShare());
        var icon = new IconButtonComp(i)
                .apply(struc -> AppFont.small(struc.get()))
                .apply(struc -> {
                    struc.get().setAlignment(Pos.CENTER);
                    struc.get().setPadding(new Insets(0, 0, 6, 0));
                    struc.get().setFocusTraversable(false);
                });
        var name = new LazyTextFieldComp(category.nameProperty())
                .apply(struc -> {
                    struc.get().prefWidthProperty().unbind();
                    struc.get().setPrefWidth(150);
                    struc.getTextField().minWidthProperty().bind(struc.get().widthProperty());
                })
                .styleClass("name")
                .createRegion();
        var showing = new SimpleBooleanProperty();
        var settings = new IconButtonComp("mdomz-settings")
                .styleClass("settings")
                .apply(new ContextMenuAugment<>(mouseEvent -> mouseEvent.getButton() == MouseButton.PRIMARY, null, () -> {
                    var cm = createContextMenu(name);
                    showing.bind(cm.showingProperty());
                    return cm;
                }));
        var shownList = ListBindingsHelper.filteredContentBinding(
                category.getContainedEntries(),
                storeEntryWrapper -> {
                    return storeEntryWrapper.shouldShow(
                            StoreViewState.get().getFilterString().getValue());
                },
                StoreViewState.get().getFilterString());
        var count = new CountComp<>(shownList, category.getContainedEntries(), string -> "(" + string + ")");
        var hover = new SimpleBooleanProperty();
        var focus = new SimpleBooleanProperty();
        var h = new HorizontalComp(List.of(
                icon,
                Comp.hspacer(4),
                Comp.of(() -> name),
                Comp.hspacer(),
                count.hide(hover.or(showing).or(focus)),
                settings.hide(hover.not().and(showing.not()).and(focus.not()))));
        h.padding(new Insets(0, 10, 0, (category.getDepth() * 10)));

        var categoryButton = new ButtonComp(null, h.createRegion(), category::select)
                .styleClass("category-button")
                .apply(struc -> hover.bind(struc.get().hoverProperty()))
                .apply(struc -> focus.bind(struc.get().focusedProperty()))
                .accessibleText(category.nameProperty())
                .grow(true, false);
        categoryButton.apply(new ContextMenuAugment<>(
                mouseEvent -> mouseEvent.getButton() == MouseButton.SECONDARY, keyEvent -> keyEvent.getCode() == KeyCode.SPACE, () -> createContextMenu(name)));

        var l = category.getChildren()
                .sorted(Comparator.comparing(
                        storeCategoryWrapper -> storeCategoryWrapper.getName().toLowerCase(Locale.ROOT)));
        var children = new ListBoxViewComp<>(l, l, storeCategoryWrapper -> new StoreCategoryComp(storeCategoryWrapper));

        var emptyBinding = Bindings.isEmpty(category.getChildren());
        var v = new VerticalComp(List.of(categoryButton, children.hide(emptyBinding)));
        v.styleClass("category");
        v.apply(struc -> {
            StoreViewState.get().getActiveCategory().subscribe(val -> {
                struc.get().pseudoClassStateChanged(SELECTED, val.equals(category));
            });
        });

        return v.createRegion();
    }

    private ContextMenu createContextMenu(Region text) {
        var contextMenu = ContextMenuHelper.create();

        var newCategory = new MenuItem(AppI18n.get("newCategory"), new FontIcon("mdi2p-plus-thick"));
        newCategory.setOnAction(event -> {
            DataStorage.get()
                    .addStoreCategory(
                            DataStoreCategory.createNew(category.getCategory().getUuid(), "New category"));
        });
        contextMenu.getItems().add(newCategory);

        if (DataStorage.get().supportsSharing() && category.getCategory().canShare()) {
            var share = new MenuItem();
            share.textProperty()
                    .bind(Bindings.createStringBinding(
                            () -> {
                                if (category.getShare().getValue()) {
                                    return AppI18n.get("unshare");
                                } else {
                                    return AppI18n.get("share");
                                }
                            },
                            category.getShare()));
            share.graphicProperty()
                    .bind(Bindings.createObjectBinding(
                            () -> {
                                if (category.getShare().getValue()) {
                                    return new FontIcon("mdi2b-block-helper");
                                } else {
                                    return new FontIcon("mdi2s-share");
                                }
                            },
                            category.getShare()));
            share.setOnAction(event -> {
                category.getShare().setValue(!category.getShare().getValue());
            });
            contextMenu.getItems().add(share);
        }

        var rename = new MenuItem(AppI18n.get("rename"), new FontIcon("mdal-edit"));
        rename.setOnAction(event -> {
            text.requestFocus();
        });
        contextMenu.getItems().add(rename);

        var del = new MenuItem(AppI18n.get("remove"), new FontIcon("mdal-delete_outline"));
        del.setOnAction(event -> {
            category.delete();
        });
        contextMenu.getItems().add(del);

        return contextMenu;
    }
}
