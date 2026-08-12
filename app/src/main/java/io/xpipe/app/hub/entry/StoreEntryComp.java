package io.xpipe.app.hub.entry;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.RegionDescriptor;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.*;
import io.xpipe.app.hub.action.HubBranchProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.HubMenuItemProvider;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.hub.list.StoreEntryBatchSelectComp;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.platform.*;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.Region;

import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class StoreEntryComp extends SimpleRegionBuilder {

    public static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    public static final PseudoClass INCOMPLETE = PseudoClass.getPseudoClass("incomplete");
    public static final ObservableDoubleValue INFO_WIDTH = Bindings.createDoubleBinding(
            () -> {
                var w = App.getApp().getStage().getWidth();
                if (w > 1400) {
                    return (w / 1.8) - 100;
                } else if (w >= 1000) {
                    return (w / 2.0) - 100;
                } else {
                    return (w / 1.7) - 50;
                }
            },
            App.getApp().getStage().widthProperty());
    private static String DEFAULT_NOTES = null;
    protected final StoreSection section;
    protected final BaseRegionBuilder<?, ?> content;
    protected final IntegerProperty contextMenuCount = new SimpleIntegerProperty();

    public StoreEntryComp(StoreSection section, BaseRegionBuilder<?, ?> content) {
        this.section = section;
        this.content = content;
    }

    public static StoreEntryComp create(StoreSection section, BaseRegionBuilder<?, ?> content, boolean preferLarge) {
        var forceCondensed = AppPrefs.get() != null
                && AppPrefs.get().condenseConnectionDisplay().get();
        if (!preferLarge || forceCondensed) {
            return new DenseStoreEntryComp(section, content);
        } else {
            return new StandardStoreEntryComp(section, content);
        }
    }

    public static StoreEntryComp of(StoreSection e) {
        var prov = e.getWrapper().getEntry().getProvider();
        if (prov != null) {
            return prov.customEntryComp(e, e.getDepth() == 1);
        } else {
            var forceCondensed = AppPrefs.get() != null
                    && AppPrefs.get().condenseConnectionDisplay().get();
            return forceCondensed ? new DenseStoreEntryComp(e, null) : new StandardStoreEntryComp(e, null);
        }
    }

    private static String getDefaultNotes() {
        var prefs = AppPrefs.get().notesTemplate().getValue();
        if (prefs != null) {
            return prefs;
        }

        if (DEFAULT_NOTES == null) {
            AppResources.with(AppResources.MAIN_MODULE, "misc/notes_default.md", f -> {
                DEFAULT_NOTES = Files.readString(f);
            });
        }
        return DEFAULT_NOTES;
    }

    public StoreEntryWrapper getWrapper() {
        return section.getWrapper();
    }

    public abstract boolean isFullSize();

    public abstract int getHeight();

    @Override
    protected final Region createSimple() {
        var r = createContent();
        var name = (Region) r.lookup(".name");

        r.getStyleClass().add("store-entry-comp");
        r.setPadding(Insets.EMPTY);
        r.setMaxWidth(10000);
        r.setFocusTraversable(true);
        RegionDescriptor.builder()
                .name(getWrapper().getShownName())
                .description(getWrapper().getShownDescription())
                .showTooltips(false)
                .build()
                .apply(r);

        r.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() != MouseButton.PRIMARY) {
                return;
            }

            if (getWrapper().getRenaming().get()) {
                return;
            }

            var count = AppPrefs.get().requireDoubleClickForConnections().get() ? 2 : 1;
            if (mouseEvent.getClickCount() != count) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                getWrapper().executeDefaultAction();
            });
            mouseEvent.consume();
        });

        new ContextMenuAugment<>(
                        mouseEvent -> mouseEvent.getButton() == MouseButton.SECONDARY,
                        null,
                        () -> this.createContextMenu(name))
                .accept(r);

        var loading =
                new LoadingOverlayComp(RegionBuilder.of(() -> r), getWrapper().getEffectiveBusy(), false);
        if (OsType.ofLocal() == OsType.MACOS) {
            AppFontSizes.base(r);
        } else if (OsType.ofLocal() == OsType.LINUX) {
            AppFontSizes.xl(r);
        } else {
            AppFontSizes.apply(r, sizes -> {
                if (sizes.getBase().equals("10.5")) {
                    return sizes.getXl();
                } else {
                    return sizes.getLg();
                }
            });
        }
        return loading.build();
    }

    protected abstract Region createContent();

    protected void applyState(Node node) {
        getWrapper().getValidity().subscribe(val -> {
            switch (val) {
                case LOAD_FAILED -> {
                    node.pseudoClassStateChanged(FAILED, true);
                    node.pseudoClassStateChanged(INCOMPLETE, false);
                }
                case INCOMPLETE -> {
                    node.pseudoClassStateChanged(FAILED, false);
                    node.pseudoClassStateChanged(INCOMPLETE, true);
                }
                default -> {
                    node.pseudoClassStateChanged(FAILED, false);
                    node.pseudoClassStateChanged(INCOMPLETE, false);
                }
            }
        });
    }

    protected BaseRegionBuilder<?, ?> createInformation() {
        var shown = Bindings.createObjectBinding(
                () -> {
                    var info = getWrapper().getShownInformation().getValue();
                    return info;
                },
                getWrapper().getShownInformation());
        var info = new StoreEntryInformationComp(getWrapper(), shown);
        return info;
    }

    protected BaseRegionBuilder<?, ?> createName() {
        var prop = new SimpleStringProperty();
        getWrapper().getShownName().subscribe(prop::setValue);
        prop.addListener((observable, oldValue, newValue) -> {
            if (!AppPrefs.get().censorMode().get()) {
                getWrapper().getName().setValue(newValue);
            }
        });
        var name = new LazyTextFieldComp(prop);
        name.style("name");
        name.applyStructure(struc -> {
            getWrapper().getRenaming().bind(struc.getTextField().focusedProperty());
        });
        return name;
    }

    private BaseRegionBuilder<?, ?> createTag(ObservableObjectValue<String> val) {
        var tagsLabel = new LabelComp(val);
        tagsLabel.minWidth(Region.USE_PREF_SIZE);
        tagsLabel.style("store-entry-tag");
        tagsLabel.apply(struc -> struc.setOpacity(0.85));
        tagsLabel.hide(Bindings.isNull(val));
        tagsLabel.apply(struc -> struc.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                getWrapper().toggleTag(val.get());
            }
            event.consume();
        }));
        return tagsLabel;
    }

    protected BaseRegionBuilder<?, ?> createTags() {
        var l = FXCollections.<BaseRegionBuilder<?, ?>>observableArrayList();
        Listeners.subscribeList(getWrapper().getTags(), () -> {
            l.clear();
            for (String tag : getWrapper().getTags()) {
                l.add(createTag(new ReadOnlyStringWrapper(tag)));
            }
        });
        return new HorizontalComp(l).spacing(4);
    }

    protected BaseRegionBuilder<?, ?> createOrderIndex() {
        var prop = new SimpleStringProperty();
        getWrapper().getOrderIndex().subscribe(number -> {
            prop.set(number.toString());
        });
        var tag = createTag(prop);
        var p = AppPrefs.get();
        tag.show(Bindings.createBooleanBinding(
                () -> {
                    return p.developerMode().getValue()
                            && p.developerShowOrderIndices().get();
                },
                p.developerShowOrderIndices(),
                p.developerMode()));
        return tag;
    }

    protected BaseRegionBuilder<?, ?> createScopeIcon() {
        var button = new IconButtonComp("mdi2a-account");
        button.style("user-icon");
        button.describe(d -> d.nameKey("restrictedConnection"));
        button.apply(struc -> {
            AppFontSizes.base(struc);
        });
        button.hide(Bindings.not(getWrapper().getAccessScopeRestricted()).or(AppSizeBreakpoints.compactMode()));
        button.apply(struc -> struc.setOpacity(0.85));
        return button;
    }

    protected BaseRegionBuilder<?, ?> createTemplateIcon() {
        var button = new IconButtonComp("mdal-content_copy");
        button.style("template-icon");
        button.describe(d -> d.nameKey("template"));
        button.apply(struc -> {
            AppFontSizes.base(struc);
        });
        button.hide(Bindings.not(getWrapper().getTemplate()).or(AppSizeBreakpoints.compactMode()));
        button.apply(struc -> struc.setOpacity(0.85));
        return button;
    }

    protected BaseRegionBuilder<?, ?> createPinIcon() {
        var button = new IconButtonComp("mdi2p-pin-outline");
        button.disable(new SimpleBooleanProperty(true));
        button.describe(d -> d.nameKey("pinned"));
        button.apply(struc -> {
            AppFontSizes.xs(struc);
            struc.setOpacity(1.0);
        });
        button.hide(Bindings.not(getWrapper().getPinToTop()).or(AppSizeBreakpoints.portraitMode()));
        button.apply(struc -> struc.setOpacity(0.85));
        return button;
    }

    protected BaseRegionBuilder<?, ?> createIcon(int w, int h, Consumer<Node> fontSize) {
        var icon = new StoreEntryIconComp(getWrapper(), w, h);
        icon.apply(struc -> {
            struc.opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                if (!getWrapper().getValidity().getValue().isUsable()) {
                                    return 0.5;
                                }

                                return !getWrapper().getEffectiveBusy().get() ? 1.0 : 0.15;
                            },
                            getWrapper().getValidity(),
                            getWrapper().getEffectiveBusy()));
        });
        var loading = new LoadingIconComp(getWrapper().getEffectiveBusy(), fontSize);
        loading.prefWidth(w);
        loading.prefHeight(h);
        var stack = new StackComp(List.of(icon, loading));
        return stack;
    }

    protected Region createButtonBar(Region name) {
        var list = DerivedObservableList.wrap(getWrapper().getMajorActionProviders(), false);
        var buttons = list.mapped(actionProvider -> {
                    var button = buildButton(actionProvider);
                    return button.build();
                })
                .filtered(region -> region != null)
                .getList();

        var ig = new InputGroup();
        Runnable update = () -> {
            var l = new ArrayList<Node>(buttons);
            var settingsButton = createSettingsButton(name).build();
            l.add(settingsButton);
            l.forEach(o -> o.getStyleClass().remove(Styles.FLAT));
            ig.getChildren().setAll(l);
        };
        buttons.subscribe(update);
        update.run();
        ig.setAlignment(Pos.CENTER_RIGHT);
        ig.getStyleClass().add("button-bar");
        AppFontSizes.base(ig);
        return ig;
    }

    private BaseRegionBuilder<?, ?> buildButton(HubMenuItemProvider<?> p) {
        var leaf = p instanceof HubLeafProvider<?> l ? l : null;
        var branch = p instanceof HubBranchProvider<?> b ? b : null;
        var button = new IconButtonComp(
                p.getIcon(getWrapper().getEntry().ref()),
                leaf != null
                        ? () -> {
                            leaf.execute(getWrapper().getEntry().ref());
                        }
                        : null);
        if (branch != null) {
            button.apply(new ContextMenuAugment<>(
                    mouseEvent -> mouseEvent.getButton() == MouseButton.PRIMARY, keyEvent -> false, () -> {
                        var cm = MenuHelper.createContextMenu();
                        var children = branch
                                .getChildren(getWrapper().getEntry().ref())
                                .stream()
                                .filter(hubMenuItemProvider -> {
                                    return hubMenuItemProvider.isApplicable(
                                            getWrapper().getEntry().ref());
                                })
                                .toList();
                        var cats = Arrays.stream(StoreActionCategory.values())
                                .collect(Collectors.toCollection(ArrayList::new));
                        cats.addFirst(null);
                        for (var cat : cats) {
                            var catChildren = children.stream()
                                    .filter(actionProvider -> actionProvider.getCategory() == cat)
                                    .toList();
                            if (catChildren.isEmpty()) {
                                continue;
                            }

                            catChildren.forEach(childProvider -> {
                                var menu = buildMenuItemForAction(getWrapper(), childProvider);
                                if (menu != null) {
                                    cm.getItems().add(menu);
                                }
                            });
                            cm.getItems().add(new SeparatorMenuItem());
                        }

                        if (cm.getItems().getLast() instanceof SeparatorMenuItem) {
                            cm.getItems().removeLast();
                        }

                        return cm;
                    }));
        }
        button.describe(d -> d.name(p.getName(getWrapper().getEntry().ref())));
        return button;
    }

    protected BaseRegionBuilder<?, ?> createSettingsButton(Region name) {
        var settingsButton = new IconButtonComp("mdi2d-dots-horizontal-circle-outline", null);
        settingsButton.style("settings");
        settingsButton.describe(d -> d.nameKey("more"));
        settingsButton.apply(new ContextMenuAugment<>(
                event -> event.getButton() == MouseButton.PRIMARY,
                null,
                () -> StoreEntryComp.this.createContextMenu(name)));
        return settingsButton;
    }

    protected BaseRegionBuilder<?, ?> createBatchSelection() {
        var c = new StoreEntryBatchSelectComp(section);
        c.hide(StoreViewState.get().getBatchMode().not());
        return c;
    }

    private void handleContextMenuCount(ContextMenu contextMenu) {
        var ref = new WeakReference<>(contextMenu);
        contextMenuCount.set(contextMenuCount.get() + 1);
        contextMenuCount.addListener((observable, oldValue, newValue) -> {
            var cm = ref.get();
            if (cm != null) {
                cm.hide();
            }
        });
    }

    protected ContextMenu createContextMenu(Region name) {
        var contextMenu = MenuHelper.createContextMenu();
        handleContextMenuCount(contextMenu);

        var cats = Arrays.stream(StoreActionCategory.values()).collect(Collectors.toCollection(ArrayList::new));
        cats.addFirst(null);
        for (var cat : cats) {
            var items = new ArrayList<MenuItem>();

            for (var p : getWrapper().getMinorActionProviders()) {
                var item = buildMenuItemForAction(getWrapper(), p);
                if (item == null || p.getCategory() != cat) {
                    continue;
                }

                items.add(item);
            }

            if (cat == StoreActionCategory.CONFIGURATION
                    && getWrapper().getEntry().getValidity() != DataStoreEntry.Validity.LOAD_FAILED) {
                var rename = new MenuItem(AppI18n.get("rename"), new FontIcon("mdal-edit"));
                rename.setOnAction(event -> {
                    name.requestFocus();
                });
                items.add(items.size(), rename);

                var notes = new MenuItem(AppI18n.get("addNotes"), new FontIcon("mdi2c-comment-text-outline"));
                notes.setOnAction(event -> {
                    StoreNotesComp.showDialog(getWrapper(), getDefaultNotes());
                    event.consume();
                });
                notes.visibleProperty().bind(BindingsHelper.map(getWrapper().getNotes(), s -> s == null));
                items.add(items.size(), notes);

                if (getWrapper().getEntry().getProvider() != null
                        && getWrapper().getEntry().getProvider().getCreationCategory() != null) {
                    var template = new MenuItem();
                    template.graphicProperty()
                            .bind(Bindings.createObjectBinding(
                                    () -> {
                                        var is = getWrapper().getTemplate().get();
                                        return is
                                                ? new FontIcon("mdi2c-credit-card-off-outline")
                                                : new FontIcon("mdi2c-credit-card-multiple-outline");
                                    },
                                    getWrapper().getTemplate()));
                    template.textProperty()
                            .bind(Bindings.createStringBinding(
                                    () -> {
                                        var is = getWrapper().getTemplate().get();
                                        return is
                                                ? AppI18n.get("untemplateConfiguration")
                                                : AppI18n.get("templateConfiguration");
                                    },
                                    AppI18n.activeLanguage(),
                                    getWrapper().getTemplate()));
                    template.setOnAction(event -> getWrapper()
                            .getEntry()
                            .setTemplate(!getWrapper().getTemplate().get()));
                    items.add(template);
                }
            }

            if (cat == StoreActionCategory.DEVELOPER) {
                if (AppPrefs.get().developerMode().getValue()) {
                    var browse = new MenuItem(
                            AppI18n.get("browseInternalStorage"), new FontIcon("mdi2f-folder-open-outline"));
                    browse.setOnAction(event ->
                            DesktopHelper.browseFile(getWrapper().getEntry().getDirectory()));
                    items.add(browse);
                }

                if (AppPrefs.get().enableHttpApi().get()) {
                    var copyId = new MenuItem(AppI18n.get("copyId"), new FontIcon("mdi2c-content-copy"));
                    copyId.setOnAction(event -> ClipboardHelper.copyText(
                            getWrapper().getEntry().getUuid().toString()));
                    items.add(copyId);
                }
            }

            if (cat == StoreActionCategory.APPEARANCE) {
                if (section.getDepth() == 1) {
                    var color = new Menu(AppI18n.get("color"), new FontIcon("mdi2f-format-color-fill"));
                    var none = new MenuItem();
                    none.textProperty().bind(AppI18n.observable("none"));
                    none.setOnAction(event -> {
                        getWrapper().getEntry().setColor(null);
                        event.consume();
                    });
                    none.setGraphic(DataStoreColor.createDisplayGraphic(null));
                    color.getItems().add(none);
                    Arrays.stream(DataStoreColor.values()).forEach(dataStoreColor -> {
                        MenuItem m = new MenuItem();
                        m.textProperty().bind(AppI18n.observable(dataStoreColor.getId()));
                        m.setOnAction(event -> {
                            getWrapper().getEntry().setColor(dataStoreColor);
                            event.consume();
                        });
                        m.setGraphic(DataStoreColor.createDisplayGraphic(dataStoreColor));
                        color.getItems().add(m);
                    });
                    items.add(color);
                }

                {
                    var tags = new Menu(AppI18n.get("tags"), new FontIcon("mdi2t-tag-text-outline"));

                    var allTags = StoreViewState.get().getAllAvailableTags();
                    for (String tag : allTags) {
                        var tagItem = new MenuItem(tag);
                        if (getWrapper().getTags().contains(tag)) {
                            tagItem.setGraphic(new FontIcon("mdi2c-check"));
                        }
                        tagItem.addEventFilter(ActionEvent.ACTION, event -> {
                            getWrapper().toggleTag(tag);
                            event.consume();
                        });
                        tags.getItems().add(tagItem);
                    }

                    if (allTags.size() > 0) {
                        tags.getItems().add(new SeparatorMenuItem());
                    }

                    var index = MenuHelper.createMenuItem(
                            new LabelGraphic.IconGraphic("mdi2t-tag-plus-outline"), "createTag");
                    index.setOnAction(event -> {
                        var tagName = new SimpleStringProperty();
                        var modal = ModalOverlay.of("addNewTag", new TextFieldComp(tagName).prefWidth(350));
                        modal.withDefaultButtons(() -> {
                            getWrapper().getEntry().addTag(tagName.getValue());
                        });
                        modal.show();
                        event.consume();
                    });
                    tags.getItems().add(index);

                    items.add(tags);
                }

                if (getWrapper().canBreakOutCategory()) {
                    var breakOut = new MenuItem();
                    var is = getWrapper().getBreakoutCategory().isPresent();
                    if (is) {
                        breakOut.textProperty().bind(AppI18n.observable("mergeCategory"));
                        breakOut.setGraphic(new FontIcon("mdi2c-collapse-all-outline"));
                    } else {
                        breakOut.textProperty().bind(AppI18n.observable("breakOutCategory"));
                        breakOut.setGraphic(new FontIcon("mdi2e-expand-all-outline"));
                    }
                    breakOut.setOnAction(event -> {
                        if (is) {
                            getWrapper().mergeBreakOutCategory();
                        } else {
                            getWrapper().breakOutCategory();
                        }
                        event.consume();
                    });
                    items.add(breakOut);
                }
            }

            if (cat == StoreActionCategory.DELETION) {
                var del = new MenuItem(AppI18n.get("remove"), new FontIcon("mdal-delete_outline"));
                del.disableProperty()
                        .bind(Bindings.createBooleanBinding(
                                () -> {
                                    return !getWrapper().getDeletable().get();
                                },
                                getWrapper().getDeletable()));
                del.setOnAction(event -> getWrapper().delete());
                contextMenu.getItems().add(del);
            }

            if (items.isEmpty()) {
                continue;
            }

            contextMenu.getItems().addAll(items);
            contextMenu.getItems().add(new SeparatorMenuItem());
        }

        return contextMenu;
    }

    public static MenuItem buildMenuItemForAction(StoreEntryWrapper wrapper, ActionProvider p) {
        var leaf = p instanceof HubLeafProvider<?> l ? l : null;
        var branch = p instanceof HubBranchProvider<?> b ? b : null;
        var cs = leaf != null ? leaf : branch;

        if (cs == null || (leaf != null && leaf.isDefault())) {
            return null;
        }

        var name = cs.getName(wrapper.getEntry().ref());
        var icon = cs.getIcon(wrapper.getEntry().ref());
        var item = branch != null
                ? new Menu(null, icon.createGraphicNode())
                : new MenuItem(null, icon.createGraphicNode());
        item.textProperty().bind(name);

        Menu menu = item instanceof Menu m ? m : null;

        if (branch != null) {
            var items = branch.getChildren(wrapper.getEntry().ref()).stream()
                    .filter(actionProvider -> wrapper.showActionProvider(actionProvider, false))
                    .map(c -> buildMenuItemForAction(wrapper, c))
                    .toList();
            menu.getItems().addAll(items);
            return menu;
        }

        item.setOnAction(event -> {
            leaf.execute(wrapper.getEntry().ref());
            event.consume();
            if (event.getTarget() instanceof Menu m) {
                m.getParentPopup().hide();
            }
        });

        return item;
    }
}
