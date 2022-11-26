package io.xpipe.extension.comp;

import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.DataStoreProviders;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.util.CustomComboBoxBuilder;
import io.xpipe.extension.util.XPipeDaemon;
import io.xpipe.fxcomps.SimpleComp;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

import java.util.function.Predicate;
import java.util.stream.Stream;

/*
TODO: Integrate store validation more into this comp.
 */
@AllArgsConstructor
public class ShellStoreChoiceComp<T extends ShellStore> extends SimpleComp {

    private final Property<T> selected;
    private final Class<T> storeClass;
    private final Predicate<T> applicableCheck;
    private final Predicate<T> supportCheck;

    private Region createGraphic(T s) {
        var provider = DataStoreProviders.byStore(s);
        var imgView =
                new PrettyImageComp(new SimpleStringProperty(provider.getDisplayIconFileName()), 16, 16).createRegion();

        var name = XPipeDaemon.getInstance().getNamedStores().stream()
                .filter(e -> e.equals(s))
                .findAny()
                .flatMap(store -> XPipeDaemon.getInstance().getStoreName(store))
                .orElse(I18n.get("localMachine"));

        return new Label(name, imgView);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Region createSimple() {
        var comboBox = new CustomComboBoxBuilder<T>(selected, this::createGraphic, null, n -> {
            if (n != null) {
                try {
                    n.checkComplete();
                    // n.test();
                } catch (Exception ex) {
                    var name = XPipeDaemon.getInstance().getNamedStores().stream()
                            .filter(e -> e.equals(n))
                            .findAny()
                            .flatMap(store -> XPipeDaemon.getInstance().getStoreName(store))
                            .orElse(I18n.get("localMachine"));
                    ErrorEvent.fromMessage(I18n.get("extension.namedHostNotActive", name))
                            .reportable(false)
                            .handle();
                    return false;
                }
            }

            //            if (n != null && !supportCheck.test(n)) {
            //                var name = XPipeDaemon.getInstance().getNamedStores().stream()
            //                        .filter(e -> e.equals(n)).findAny()
            //                        .flatMap(store ->
            // XPipeDaemon.getInstance().getStoreName(store)).orElse(I18n.get("localMachine"));
            //                ErrorEvent.fromMessage(I18n.get("extension.namedHostFeatureUnsupported",
            // name)).reportable(false).handle();
            //                return false;
            //            }
            return true;
        });

        var available = Stream.concat(
                        Stream.of(new LocalStore()),
                        XPipeDaemon.getInstance().getNamedStores().stream()
                                .filter(s -> storeClass.isAssignableFrom(s.getClass()) && applicableCheck.test((T) s))
                                .map(s -> (ShellStore) s))
                .toList();
        available.forEach(s -> comboBox.add((T) s));
        ComboBox<Node> cb = comboBox.build();
        cb.getStyleClass().add("choice-comp");
        return cb;
    }
}
