package io.xpipe.app.storage;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.DataStorageAccessType;
import io.xpipe.app.secret.DataStorageAccessHandler;
import io.xpipe.app.secret.EncryptionPrincipal;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.*;

public class DataStoreAccessScopeComp extends SimpleRegionBuilder {

    private final Property<DataStoreAccessScope> scope;

    public DataStoreAccessScopeComp(Property<DataStoreAccessScope> scope) {
        this.scope = scope;

        if (scope.getValue() == null) {
            scope.setValue(DataStoreAccessScope.encryption());
        }
    }

    @Override
    protected Region createSimple() {
        var handler = DataStorageAccessHandler.getInstance();
        var auth = handler.getType();

        if (auth != DataStorageAccessType.ROLE || !handler.isAccessible() || !handler.isAccessSubRestricted()) {
            var l = new LabelComp(AppI18n.observable("unavailable"));

            var settings = new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                        AppPrefs.get().selectCategory("vaultAccess");
                    })
                    .padding(new Insets(7));
            return new HorizontalComp(List.of(l, settings))
                    .spacing(10)
                    .apply(hBox -> hBox.setAlignment(Pos.CENTER_LEFT))
                    .build();
        }

        var allPrincipals = FXCollections.observableArrayList(handler.getAllEncryptionPrincipals());
        allPrincipals.removeIf(
                encryptionPrincipal -> encryptionPrincipal.getName().equals("vault"));
        allPrincipals.remove(handler.getEncryptAllPrincipal());
        allPrincipals.sort(Comparator.comparing(encryptionPrincipal -> encryptionPrincipal.getName()));

        var selectedPrincipals = FXCollections.observableArrayList(
                scope.getValue().isAccessSubRestricted() ? scope.getValue().getPrincipals() : List.of());
        selectedPrincipals.addListener((ListChangeListener<? super EncryptionPrincipal>) c -> {
            if (selectedPrincipals.isEmpty()) {
                scope.setValue(DataStoreAccessScope.encryption());
                return;
            }

            scope.setValue(DataStoreAccessScope.of(new HashSet<>(selectedPrincipals)));
        });

        var description = Bindings.createStringBinding(
                () -> {
                    var s = scope.getValue();
                    var all = DataStoreAccessScope.encryption().equals(s);
                    if (all) {
                        return AppI18n.get("allRoles");
                    }

                    if (s.getPrincipals().size() <= 3) {
                        return String.join(
                                ", ",
                                s.getPrincipals().stream()
                                        .map(EncryptionPrincipal::getName)
                                        .sorted()
                                        .toList());
                    }

                    return AppI18n.get("manyRoles", s.getPrincipals().size());
                },
                scope,
                AppI18n.activeLanguage());
        var button = new ButtonComp(description, new FontIcon("mdi2a-account-group-outline"), null);
        button.apply(struc -> struc.setOnAction(event -> {
            if (!allPrincipals.isEmpty()) {
                var selector = new ListSelectorComp<>(
                        allPrincipals,
                        r -> r.getName(),
                        r -> new LabelGraphic.IconGraphic("mdi2a-account"),
                        selectedPrincipals,
                        r -> !handler.getCurrentEncryptionPrincipals().contains(r),
                        () -> true);
                var header = new LabelComp(AppI18n.observable("restrictAccessTo")).style(Styles.TEXT_BOLD);
                var content = new VerticalComp(List.of(header, selector)).spacing(10);
                var popover = new Popover();
                popover.setArrowLocation(Popover.ArrowLocation.TOP_CENTER);
                popover.setContentNode(content.prefWidth(300).build());
                popover.show(struc);
            }
            event.consume();
        }));
        return button.build();
    }
}
