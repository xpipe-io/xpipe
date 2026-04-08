package io.xpipe.app.hub.comp;

import io.xpipe.app.action.QuickConnectProvider;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.OptionsBuilder;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;

import java.util.List;

public class StoreFilterStateComp extends SimpleRegionBuilder {

    private ButtonComp createButton(String text, String value) {
        var button = new ButtonComp(new ReadOnlyObjectWrapper<>(text), () -> {
            if (value != null) {
                StoreFilterState.get().set(value);
            }
        });
        button.disable(value == null);
        button.apply(r -> r.setAlignment(Pos.CENTER_LEFT));
        button.style(Styles.FLAT);
        button.maxWidth(250);
        return button;
    }

    @Override
    protected Region createSimple() {
        var state = StoreFilterState.get();

        var searches = state.getRecentSearches().getList();
        var searchesEmpty = Bindings.isEmpty(searches);
        var searchesList = new ListBoxViewComp<>(searches, searches, s -> createButton(s, s), false);

        var searchesPlaceholders = FXCollections.observableList(List.of(
                AppI18n.get("recentSearchesDescriptionNames"),
                AppI18n.get("recentSearchesDescriptionTags"),
                AppI18n.get("recentSearchesDescriptionTypes"),
                AppI18n.get("recentSearchesDescriptionState"),
                AppI18n.get("recentSearchesDescriptionJoin")));
        var searchesEmptyList =
                new ListBoxViewComp<>(searchesPlaceholders, searchesPlaceholders, s -> createButton(s, null), false);

        var quickConnections = state.getRecentQuickConnections().getList();
        var quickConnectionsEmpty = Bindings.isEmpty(quickConnections);
        var quickConnectionsList =
                new ListBoxViewComp<>(quickConnections, quickConnections, s -> createButton(s, s), false);

        var quickConnectionsPlaceholders = FXCollections.observableArrayList(QuickConnectProvider.getAll().stream()
                .map(p -> p.getPlaceholder())
                .toList());
        var quickConnectionsEmptyList = new ListBoxViewComp<>(
                quickConnectionsPlaceholders,
                quickConnectionsPlaceholders,
                s -> createButton(s, s.split(" ")[0] + " "),
                false);

        var options = new OptionsBuilder()
                .addComp(new LabelComp(AppI18n.observable("recentSearches")))
                .hide(searchesEmpty)
                .addComp(searchesList)
                .hide(searchesEmpty)
                .addComp(new LabelComp(AppI18n.observable("recentQuickConnections")))
                .hide(quickConnectionsEmpty)
                .addComp(quickConnectionsList)
                .hide(quickConnectionsEmpty)
                .addComp(RegionBuilder.hseparator())
                .addComp(new LabelComp(AppI18n.observable("recentSearchesDescription")))
                .addComp(searchesEmptyList)
                .addComp(new LabelComp(AppI18n.observable("recentQuickConnectionsDescription")))
                .addComp(quickConnectionsEmptyList)
                .build();
        options.getStyleClass().add("store-filter-state-comp");
        return options;
    }
}
