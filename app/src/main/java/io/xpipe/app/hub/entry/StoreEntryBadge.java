package io.xpipe.app.hub.entry;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.platform.ClipboardHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.MenuHelper;
import io.xpipe.app.store.HostAddressStore;
import io.xpipe.app.util.*;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public interface StoreEntryBadge {

    enum CompressBehaviour {
        DONT_COMPRESS,
        COMPRESS_TO_GRAPHIC,
        HIDE
    }

    @FunctionalInterface
    interface Action {

        static Action provider(String id) {
            var p = ActionProvider.byId(id);
            return (wrapper, button) -> {
                if (p instanceof HubLeafProvider<?> l
                        && l.getApplicableClass()
                                .isAssignableFrom(wrapper.getEntry().getStore().getClass())
                        && l.isApplicable(wrapper.getEntry().ref())) {
                    l.createAction(wrapper.getEntry().ref()).executeAsync();
                }
            };
        }

        static Action providerMenu(String... ids) {
            var cm = new AtomicReference<ContextMenu>();
            return new Action() {
                @Override
                public void run(StoreEntryWrapper wrapper, Button button) {
                    if (cm.get() == null) {
                        cm.set(MenuHelper.createContextMenu());
                    }

                    var provs = getProviders(wrapper);
                    cm.get().getItems().clear();
                    for (var p : provs) {
                        var item = StoreEntryComp.buildMenuItemForAction(wrapper, p);
                        cm.get().getItems().add(item);
                    }
                    MenuHelper.show(cm.get(), button, Side.BOTTOM);
                }

                @Override
                public boolean checkApplicable(StoreEntryWrapper w) {
                    return !getProviders(w).isEmpty();
                }

                private List<ActionProvider> getProviders(StoreEntryWrapper wrapper) {
                    var provs = Arrays.stream(ids)
                            .map(s -> ActionProvider.byId(s))
                            .filter(p -> p instanceof HubLeafProvider<?> l
                                    && l.getApplicableClass()
                                            .isAssignableFrom(wrapper.getEntry()
                                                    .getStore()
                                                    .getClass())
                                    && l.isApplicable(wrapper.getEntry().ref().asNeeded()))
                            .toList();
                    return provs;
                }
            };
        }

        default boolean checkApplicable(StoreEntryWrapper w) {
            return true;
        }

        void run(StoreEntryWrapper wrapper, Button button);
    }

    static StoreEntryBadge ofRunningState(String s, String runningCheck) {
        s = DataStoreFormatter.capitalize(s);
        return ofRunningState(s, runningCheck.equals(s), false);
    }

    static StoreEntryBadge ofRunningState(String s, String runningCheck, String stoppedCheck) {
        s = DataStoreFormatter.capitalize(s);
        return ofRunningState(s, runningCheck.equals(s), stoppedCheck.equals(s));
    }

    static StoreEntryBadge ofRunningState(String s, boolean runningCheck, boolean stoppedCheck) {
        return ofRunningState(s, runningCheck, stoppedCheck, false);
    }

    static StoreEntryBadge ofRunningState(String s, String runningCheck, String stoppedCheck, String pausedCheck) {
        s = DataStoreFormatter.capitalize(s);
        return ofRunningState(s, runningCheck.equals(s), stoppedCheck.equals(s), pausedCheck.equals(s));
    }

    static StoreEntryBadge ofRunningState(String s, boolean runningCheck, boolean stoppedCheck, boolean pausedCheck) {
        if (s == null) {
            return null;
        }

        s = DataStoreFormatter.capitalize(s);

        StoreEntryBadge b;
        if (runningCheck) {
            b = ofRunning(s);
        } else if (stoppedCheck) {
            b = ofStopped(s);
        } else if (pausedCheck) {
            b = ofPaused(s);
        } else {
            b = ofIndeterminant(s);
        }

        return b.withAction(Action.providerMenu("startStore", "stopStore", "pauseStore", "restartStore"));
    }

    static StoreEntryBadge ofAuth(String s) {
        if (s == null) {
            return null;
        }

        return of("mdi2a-account", s);
    }


    static StoreEntryBadge ofUsername(String s) {
        if (s == null) {
            return null;
        }

        return of("mdi2a-account", s).withCopyAction();
    }

    static StoreEntryBadge ofUsername(String display, String copy) {
        if (display == null) {
            return null;
        }

        return of("mdi2a-account", display).withCopyAction(copy);
    }

    static StoreEntryBadge ofPassword(String s) {
        if (s == null) {
            return null;
        }

        return of("mdi2l-lock-open-plus-outline", s);
    }

    static StoreEntryBadge ofCommand(String s) {
        if (s == null) {
            return null;
        }

        return of("mdi2c-console", s).withCopyAction();
    }

    static StoreEntryBadge ofKey(String s) {
        if (s == null) {
            return null;
        }

        return of("mdi2k-key-plus", s);
    }

    static StoreEntryBadge ofConnectionType(String s) {
        if (s == null) {
            return null;
        }

        return of("mdi2c-connection", s).withCompressBehaviour(CompressBehaviour.HIDE);
    }

    static StoreEntryBadge ofAddress(String s) {
        if (s == null) {
            return null;
        }

        return of("mdi2s-server-network-outline", s).withCopyAction();
    }

    static StoreEntryBadge ofAddress(HostAddress addr) {
        if (addr == null) {
            return null;
        }

        var effective = addr.getIpv4Address().orElse(null);
        if (effective == null) {
            return null;
        }

        var cm = new AtomicReference<ContextMenu>();
        return of("mdi2s-server-network-outline", effective).withAction((wrapper, b) -> {
            if (wrapper.getEntry().getStore() instanceof HostAddressStore has) {
                b.setDisable(true);
                ThreadHelper.runFailableAsync(() -> {
                    try {
                        has.refreshHostAddressOrThrow();
                    } finally {
                        Platform.runLater(() -> {
                            b.setDisable(false);
                        });
                    }

                    var refreshed = has.getHostAddress();
                    if (refreshed == null || refreshed.isEmpty()) {
                        return;
                    }

                    if (refreshed.isSingle()) {
                        ClipboardHelper.copyText(refreshed.get());
                        return;
                    }

                    Platform.runLater(() -> {
                        if (cm.get() == null) {
                            cm.set(MenuHelper.createContextMenu());
                        }

                        cm.get().getItems().clear();
                        for (var a : refreshed.getAvailable()) {
                            var i = new MenuItem();
                            i.setText(a);
                            i.setOnAction(event -> {
                                ClipboardHelper.copyText(a);
                                event.consume();
                            });
                            cm.get().getItems().add(i);
                        }
                        MenuHelper.show(cm.get(), b, Side.BOTTOM);
                    });
                });
            } else {
                ClipboardHelper.copyText(effective);
            }
        });
    }

    static StoreEntryBadge ofNetworkInfo(String s) {
        if (s == null) {
            return null;
        }

        return of("mdi2s-server-network-outline", s).withCompressBehaviour(CompressBehaviour.HIDE);
    }

    static StoreEntryBadge ofSetting(String s) {
        if (s == null) {
            return null;
        }

        return of("mdomz-settings", s).withCompressBehaviour(CompressBehaviour.HIDE);
    }

    static StoreEntryBadge ofFile(String s) {
        if (s == null) {
            return null;
        }

        return of("mdi2f-file-outline", s).withCompressBehaviour(CompressBehaviour.HIDE);
    }

    static StoreEntryBadge ofWeb(String s) {
        if (s == null) {
            return null;
        }

        return of("mdi2w-web", s);
    }

    static StoreEntryBadge of(LabelGraphic graphic, String s) {
        return new Simple(graphic, s);
    }

    static StoreEntryBadge of(String icon, String s) {
        return new Simple(new LabelGraphic.IconGraphic(icon), s);
    }

    static StoreEntryBadge ofString(String s) {
        return new Simple(null, s);
    }

    static StoreEntryBadge ofSystemName(OsType.Any type, String s) {
        if (type == null && s == null) {
            return null;
        }

        var img = OsLogoRegistry.getImage(s, type);
        var graphic = img != null ? new LabelGraphic.ImageGraphic(img, 24) : null;

        s = s.replaceAll("^Microsoft ", "");
        s = s.replace("Enterprise Evaluation", "Enterprise");

        return graphic != null ? of(graphic, s) : ofSuccess(s);
    }

    static StoreEntryBadge ofUnknownSystemName() {
        var s = AppI18n.get("unknown");
        var img = OsLogoRegistry.getUnknownImage();
        var graphic = img != null ? new LabelGraphic.ImageGraphic(img, 24) : null;
        return of(graphic, s);
    }

    static StoreEntryBadge ofRunning(String s) {
        if (s == null) {
            return null;
        }

        var graphic = new LabelGraphic.IconGraphic("mdi2p-play", "inner-icon");
        var border = new LabelGraphic.IconGraphic("mdi2s-square-rounded-outline", "outer-icon");
        var stack = new LabelGraphic.IconStackGraphic(List.of(border, graphic));
        return new Simple(stack, s).withStyleClass("success-badge");
    }

    static StoreEntryBadge ofStopped(String s) {
        if (s == null) {
            return null;
        }

        var graphic = new LabelGraphic.IconGraphic("mdi2s-stop", "inner-icon");
        var border = new LabelGraphic.IconGraphic("mdi2s-square-rounded-outline", "outer-icon");
        var stack = new LabelGraphic.IconStackGraphic(List.of(border, graphic));
        return new Simple(stack, s).withStyleClass("failure-badge");
    }

    static StoreEntryBadge ofPaused(String s) {
        if (s == null) {
            return null;
        }

        var graphic = new LabelGraphic.IconGraphic("mdi2p-pause", "inner-icon");
        var border = new LabelGraphic.IconGraphic("mdi2s-square-rounded-outline", "outer-icon");
        var stack = new LabelGraphic.IconStackGraphic(List.of(border, graphic));
        return new Simple(stack, s).withStyleClass("indeterminant-badge");
    }

    static StoreEntryBadge ofFailure(String s) {
        if (s == null) {
            return null;
        }

        var graphic = new LabelGraphic.IconGraphic("mdi2l-lightning-bolt", "inner-icon");
        var border = new LabelGraphic.IconGraphic("mdi2s-square-rounded-outline", "outer-icon");
        var stack = new LabelGraphic.IconStackGraphic(List.of(border, graphic));
        return new Simple(stack, s).withStyleClass("failure-badge");
    }

    static StoreEntryBadge ofSuccess(String s) {
        if (s == null) {
            return null;
        }

        var graphic = new LabelGraphic.IconGraphic("mdal-check", "inner-icon");
        var border = new LabelGraphic.IconGraphic("mdi2s-square-rounded-outline", "outer-icon");
        var stack = new LabelGraphic.IconStackGraphic(List.of(border, graphic));
        return new Simple(stack, s).withStyleClass("success-badge");
    }

    static StoreEntryBadge ofIndeterminant(String s) {
        if (s == null) {
            return null;
        }

        var graphic = new LabelGraphic.IconGraphic("mdsmz-remove", "inner-icon");
        var border = new LabelGraphic.IconGraphic("mdi2s-square-rounded-outline", "outer-icon");
        var stack = new LabelGraphic.IconStackGraphic(List.of(border, graphic));
        return new Simple(stack, s).withStyleClass("indeterminant-badge");
    }

    static StoreEntryBadge ofLicense(LicensedFeature... ls) {
        var effective = Arrays.stream(ls).filter(l -> l != null).toList();
        if (effective.isEmpty()) {
            return null;
        }

        var licenseReq = effective.stream()
                .map(licensedFeature -> licensedFeature.getDescriptionSuffix())
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(null);
        if (licenseReq == null) {
            return null;
        }

        var supported = effective.stream().anyMatch(l -> !l.isSupported());
        return supported ? of("mdi2p-professional-hexagon", licenseReq).withStyleClass("license-badge") : null;
    }

    LabelGraphic getGraphic();

    String getName();

    StoreEntryBadge withStyleClass(String s);

    StoreEntryBadge withAction(Action action);

    StoreEntryBadge censored();

    StoreEntryBadge withCompressedName(String s);

    StoreEntryBadge withCompressBehaviour(CompressBehaviour behaviour);

    CompressBehaviour getCompressBehaviour();

    Optional<String> getStyleClass();

    Optional<String> getCompressedName();

    Optional<Action> getAction();

    public StoreEntryBadge withCopyAction();

    public StoreEntryBadge withCopyAction(String s);

    class Simple implements StoreEntryBadge {

        private final LabelGraphic graphic;
        private final String name;
        private final String compressedName;
        private final String styleClass;
        private final CompressBehaviour compressBehaviour;
        private final Action action;

        public Simple(LabelGraphic graphic, String name) {
            this.graphic = graphic;
            this.name = name;
            this.styleClass = null;
            this.compressedName = null;
            this.action = null;
            this.compressBehaviour = CompressBehaviour.COMPRESS_TO_GRAPHIC;
        }

        private Simple(
                LabelGraphic graphic,
                String name,
                String compressedName,
                String styleClass,
                CompressBehaviour compressBehaviour,
                Action action) {
            this.graphic = graphic;
            this.name = name;
            this.compressedName = compressedName;
            this.styleClass = styleClass;
            this.compressBehaviour = compressBehaviour;
            this.action = action;
        }

        public StoreEntryBadge withCopyAction() {
            return withCopyAction(name);
        }


        public StoreEntryBadge withCopyAction(String s) {
            if (s == null) {
                return this;
            }

            return new Simple(graphic, name, compressedName, styleClass, compressBehaviour, new Action() {
                @Override
                public void run(StoreEntryWrapper wrapper, Button button) {
                    ClipboardHelper.copyText(s);
                }
            });
        }


        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Simple simple)) {
                return false;
            }
            return Objects.equals(graphic, simple.graphic)
                    && Objects.equals(name, simple.name)
                    && Objects.equals(compressedName, simple.compressedName)
                    && Objects.equals(styleClass, simple.styleClass)
                    && compressBehaviour == simple.compressBehaviour;
        }

        @Override
        public int hashCode() {
            return Objects.hash(graphic, name, compressedName, styleClass, compressBehaviour);
        }

        public Simple withStyleClass(String styleClass) {
            return new Simple(graphic, name, compressedName, styleClass, compressBehaviour, action);
        }

        @Override
        public StoreEntryBadge withAction(Action action) {
            return new Simple(graphic, name, compressedName, styleClass, compressBehaviour, action);
        }

        @Override
        public Simple censored() {
            return new Simple(
                    graphic,
                    name != null ? "*".repeat(name.length()) : null,
                    compressedName,
                    styleClass,
                    compressBehaviour,
                    action);
        }

        @Override
        public LabelGraphic getGraphic() {
            return graphic;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public StoreEntryBadge withCompressedName(String s) {
            return new Simple(graphic, name, s, styleClass, compressBehaviour, action);
        }

        @Override
        public StoreEntryBadge withCompressBehaviour(CompressBehaviour behaviour) {
            return new Simple(graphic, name, compressedName, styleClass, behaviour, action);
        }

        @Override
        public CompressBehaviour getCompressBehaviour() {
            return compressBehaviour;
        }

        @Override
        public Optional<String> getStyleClass() {
            return Optional.ofNullable(styleClass);
        }

        @Override
        public Optional<String> getCompressedName() {
            return Optional.ofNullable(compressedName);
        }

        @Override
        public Optional<Action> getAction() {
            return Optional.ofNullable(action);
        }

        @Override
        public String toString() {
            return (graphic instanceof LabelGraphic.IconGraphic i ? i.getIcon() + " " : "") + name;
        }
    }
}
