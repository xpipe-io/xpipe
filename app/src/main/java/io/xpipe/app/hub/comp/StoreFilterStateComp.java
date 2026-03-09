package io.xpipe.app.hub.comp;

import io.xpipe.app.action.QuickConnectProvider;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.OptionsBuilder;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;

import java.util.List;

public class StoreFilterStateComp extends SimpleRegionBuilder {

    @Override
    protected Region createSimple() {
        var state = StoreFilterState.get();

        var searches = state.getRecentSearches().getList();
        var searchesEmpty = Bindings.isEmpty(searches);
        var searchList = new ListBoxViewComp<String>(searches, searches, s -> {
            return new ButtonComp(new ReadOnlyObjectWrapper<>(s), () -> {

            });
        }, false);
        searchList.hide(searchesEmpty);
        var searchesLabel = new LabelComp(AppI18n.observable("recent"));
        searchesLabel.hide(searchesEmpty);

        var quickConnections = state.getRecentQuickConnections().getList();
        var quickConnectionsEmpty = Bindings.isEmpty(searches);
        var quickConnectionsList = new ListBoxViewComp<String>(quickConnections, quickConnections, s -> {
            return new ButtonComp(new ReadOnlyObjectWrapper<>(s), () -> {

            });
        }, false);

        var quickConnectionsPlaceholders = FXCollections.observableArrayList(QuickConnectProvider.getAll().stream()
                .map(quickConnectProvider -> quickConnectProvider.getPlaceholder())
                .toList());
        var quickConnectionsEmptyList = new ListBoxViewComp<String>(quickConnectionsPlaceholders, quickConnectionsPlaceholders, s -> {
            return new ButtonComp(new ReadOnlyObjectWrapper<>(s), () -> {

            });
        }, false);


        var quickConnectionsLabel = new LabelComp(AppI18n.observable("recent"));
        quickConnectionsLabel.hide(quickConnectionsEmpty);

        var otherLabel = new LabelComp(AppI18n.observable("recent"));
        var otherList = List.of("ssh://user@host", "sftp://user@host", "s3://host/path", "xpipe://action?...");

        var options = new OptionsBuilder()
                .name("recent")
                .addComp(searchList)
                .hide(searchesEmpty)
                .addComp(searchesLabel)
                .hide(searchesEmpty.not())
                .name("recent")
                .addComp(quickConnectionsList)
                .hide(quickConnectionsEmpty)
                .addComp(quickConnectionsEmptyList)
                .hide(quickConnectionsEmpty.not())
                .build();
        return options;
    }
}
