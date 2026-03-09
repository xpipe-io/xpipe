package io.xpipe.app.hub.comp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.xpipe.app.action.LauncherUrlProvider;
import io.xpipe.app.action.QuickConnectProvider;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.platform.DerivedObservableList;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import lombok.Getter;

import java.util.List;

public class StoreFilterState {

    @Getter
    private final DerivedObservableList<String> recentSearches =
            DerivedObservableList.synchronizedArrayList(true);

    @Getter
    private final DerivedObservableList<String> recentQuickConnections =
            DerivedObservableList.synchronizedArrayList(true);

    @Getter
    private final StringProperty rawText = new SimpleStringProperty();

    @Getter
    private final ObservableBooleanValue isQuickConnectString = Bindings.createBooleanBinding(() -> {
        var v = rawText.getValue();
        if (v == null) {
            return false;
        }

        if (!v.contains(" ")) {
            return false;
        }

        return QuickConnectProvider.find(v).isPresent();
    }, rawText);

    @Getter
    private final ObservableBooleanValue isUrlString = Bindings.createBooleanBinding(() -> {
        var v = rawText.getValue();
        if (v == null) {
            return false;
        }

        if (!v.contains(" ")) {
            return false;
        }

        return LauncherUrlProvider.find(v).isPresent();
    }, rawText);

    @Getter
    private final ObservableBooleanValue isSearchString = Bindings.createBooleanBinding(() -> {
        return rawText.getValue() != null && rawText.getValue().length() > 1 && !isUrlString.getValue() && !isQuickConnectString.getValue();
    }, rawText, isQuickConnectString, isUrlString);

    @Getter
    private final ObservableStringValue effectiveFilter = Bindings.createStringBinding(() -> {
        return isSearchString.get() ? rawText.getValue() : null;
    }, rawText, isSearchString);

    private static StoreFilterState INSTANCE;

    public static StoreFilterState get() {
        return INSTANCE;
    }

    public static void init() {
        var type = TypeFactory.defaultInstance().constructType(new TypeReference<List<String>>() {});
        List<String> recentSearches = AppCache.getNonNull("recentSearches", type, () -> List.of());
        List<String> recentQuickConnections = AppCache.getNonNull("recentQuickConnections", type, () -> List.of());

        INSTANCE = new StoreFilterState();
        INSTANCE.recentSearches.setContent(recentSearches);
        INSTANCE.recentQuickConnections.setContent(recentQuickConnections);
    }


    public void putFilter(String s) {
        synchronized (recentSearches) {
            if (recentSearches.getList().size() == 3) {
                recentSearches.getList().addFirst(s);
                recentSearches.getList().removeLast();
            } else {
                recentSearches.getList().addFirst(s);
            }
        }
    }

    public void putQuickConnect(String s) {
        synchronized (recentQuickConnections) {
            if (recentQuickConnections.getList().size() == 3) {
                recentQuickConnections.getList().addFirst(s);
                recentQuickConnections.getList().removeLast();
            } else {
                recentQuickConnections.getList().addFirst(s);
            }
        }
    }
}
