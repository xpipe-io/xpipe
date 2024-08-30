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
import io.xpipe.app.fxcomps.util.DerivedObservableList;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.util.ContextMenuHelper;
import io.xpipe.app.util.DataStoreFormatter;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
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
        var name = new LazyTextFieldComp(category.nameProperty())
                .styleClass("name")
                .createRegion();
        var showing = new SimpleBooleanProperty();

        var expandIcon = Bindings.createStringBinding(
                () -> {
                    var exp = category.getExpanded().get()
                            && category.getChildren().size() > 0;
                    return exp ? "mdal-keyboard_arrow_down" : "mdal-keyboard_arrow_right";
                },
                category.getExpanded(),
                category.getChildren());
        var expandButton = new IconButtonComp(expandIcon, () -> {
                    category.toggleExpanded();
                })
                .apply(struc -> AppFont.medium(struc.get()))
                .apply(struc -> {
                    struc.get().setAlignment(Pos.CENTER);
                    struc.get().setPadding(new Insets(-2, 0, 0, 0));
                    struc.get().setFocusTraversable(false);
                })
                .styleClass("expand-button")
                .tooltipKey("expand", new KeyCodeCombination(KeyCode.SPACE));

        var hover = new SimpleBooleanProperty();
        var statusIcon = Bindings.createStringBinding(
                () -> {
                    if (hover.get()) {
                        return "mdomz-settings";
                    }

                    if (!DataStorage.get().supportsSharing()
                            || !category.getCategory().canShare()) {
                        return "mdi2g-git";
                    }

                    return category.getSync().getValue() ? "mdi2g-git" : "mdi2c-cancel";
                },
                category.getSync(),
                hover);
        var statusButton = new IconButtonComp(statusIcon)
                .apply(struc -> AppFont.small(struc.get()))
                .apply(struc -> {
                    struc.get().setAlignment(Pos.CENTER);
                    struc.get().setPadding(new Insets(0, 0, 7, 0));
                    struc.get().setFocusTraversable(false);
                    hover.bind(struc.get().hoverProperty());
                })
                .apply(new ContextMenuAugment<>(
                        mouseEvent -> mouseEvent.getButton() == MouseButton.PRIMARY, null, () -> {
                            var cm = createContextMenu(name);
                            showing.bind(cm.showingProperty());
                            return cm;
                        }))
                .styleClass("status-button");

        var shownList = new DerivedObservableList<>(category.getAllContainedEntries(), true)
                .filtered(
                        storeEntryWrapper -> {
                            return storeEntryWrapper.matchesFilter(
                                    StoreViewState.get().getFilterString().getValue());
                        },
                        StoreViewState.get().getFilterString())
                .getList();
        var count = new CountComp<>(shownList, category.getAllContainedEntries(), string -> "(" + string + ")");

        var showStatus = hover.or(new SimpleBooleanProperty(DataStorage.get().supportsSharing()))
                .or(showing);
        var focus = new SimpleBooleanProperty();
        var h = new HorizontalComp(List.of(
                expandButton,
                Comp.hspacer(1),
                Comp.of(() -> name).hgrow(),
                Comp.hspacer(2),
                count,
                Comp.hspacer(7),
                statusButton.hide(showStatus.not())));
        h.padding(new Insets(0, 10, 0, (category.getDepth() * 10)));

        var categoryButton = new ButtonComp(null, h.createRegion(), category::select)
                .focusTraversable()
                .styleClass("category-button")
                .apply(struc -> hover.bind(struc.get().hoverProperty()))
                .apply(struc -> focus.bind(struc.get().focusedProperty()))
                .accessibleText(category.nameProperty())
                .grow(true, false);
        categoryButton.apply(new ContextMenuAugment<>(
                mouseEvent -> mouseEvent.getButton() == MouseButton.SECONDARY,
                keyEvent -> keyEvent.getCode() == KeyCode.SPACE,
                () -> createContextMenu(name)));
        categoryButton.apply(struc -> {
            struc.get().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.SPACE) {
                    category.toggleExpanded();
                    event.consume();
                }
            });
        });

        var l = category.getChildren()
                .sorted(Comparator.comparing(storeCategoryWrapper ->
                        storeCategoryWrapper.nameProperty().getValue().toLowerCase(Locale.ROOT)));
        var children =
                new ListBoxViewComp<>(l, l, storeCategoryWrapper -> new StoreCategoryComp(storeCategoryWrapper), false);
        children.styleClass("children");

        var hide = Bindings.createBooleanBinding(
                () -> {
                    return !category.getExpanded().get()
                            || category.getChildren().isEmpty();
                },
                category.getChildren(),
                category.getExpanded());
        var v = new VerticalComp(List.of(categoryButton, children.hide(hide)));
        v.styleClass("category");
        v.apply(struc -> {
            StoreViewState.get().getActiveCategory().subscribe(val -> {
                struc.get().pseudoClassStateChanged(SELECTED, val.equals(category));
            });

            category.getColor().subscribe((c) -> {
                DataColor.applyStyleClasses(c, struc.get());
            });
        });

        return v.createRegion();
    }

    private ContextMenu createContextMenu(Region text) {
        var contextMenu = ContextMenuHelper.create();
        AppFont.normal(contextMenu.getStyleableNode());

        var newCategory = new MenuItem(AppI18n.get("newCategory"), new FontIcon("mdi2p-plus-thick"));
        newCategory.setOnAction(event -> {
            DataStorage.get()
                    .addStoreCategory(
                            DataStoreCategory.createNew(category.getCategory().getUuid(), "New category"));
        });
        contextMenu.getItems().add(newCategory);

        contextMenu.getItems().add(new SeparatorMenuItem());

        var color = new Menu(AppI18n.get("color"), new FontIcon("mdi2f-format-color-fill"));
        var none = new MenuItem("None");
        none.setOnAction(event -> {
            category.getCategory().setColor(null);
            event.consume();
        });
        color.getItems().add(none);
        Arrays.stream(DataColor.values()).forEach(dataStoreColor -> {
            MenuItem m = new MenuItem(DataStoreFormatter.capitalize(dataStoreColor.getId()));
            m.setOnAction(event -> {
                category.getCategory().setColor(dataStoreColor);
                event.consume();
            });
            color.getItems().add(m);
        });
        contextMenu.getItems().add(color);

        if (DataStorage.get().supportsSharing() && category.getCategory().canShare()) {
            var share = new MenuItem();
            share.textProperty()
                    .bind(Bindings.createStringBinding(
                            () -> {
                                if (category.getSync().getValue()) {
                                    return AppI18n.get("unshare");
                                } else {
                                    return AppI18n.get("share");
                                }
                            },
                            category.getSync()));
            share.graphicProperty()
                    .bind(Bindings.createObjectBinding(
                            () -> {
                                if (category.getSync().getValue()) {
                                    return new FontIcon("mdi2b-block-helper");
                                } else {
                                    return new FontIcon("mdi2g-git");
                                }
                            },
                            category.getSync()));
            share.setOnAction(event -> {
                category.getSync().setValue(!category.getSync().getValue());
            });
            contextMenu.getItems().add(share);
        }

        var rename = new MenuItem(AppI18n.get("rename"), new FontIcon("mdal-edit"));
        rename.setOnAction(event -> {
            text.setDisable(false);
            text.requestFocus();
        });
        contextMenu.getItems().add(rename);

        contextMenu.getItems().add(new SeparatorMenuItem());

        var del = new MenuItem(AppI18n.get("remove"), new FontIcon("mdal-delete_outline"));
        del.setOnAction(event -> {
            category.delete();
        });
        contextMenu.getItems().add(del);

        return contextMenu;
    }
}
