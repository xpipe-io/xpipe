package io.xpipe.app.hub.comp;

import io.xpipe.app.action.LauncherUrlProvider;
import io.xpipe.app.action.QuickConnectProvider;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.DerivedObservableList;

import io.xpipe.app.util.GlobalTimer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.Getter;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class StoreFilterState {

    @Getter
    private final DerivedObservableList<String> recentSearches = DerivedObservableList.synchronizedArrayList(true);

    @Getter
    private final DerivedObservableList<String> recentQuickConnections =
            DerivedObservableList.synchronizedArrayList(true);

    @Getter
    private final StringProperty fieldText = new SimpleStringProperty();

    @Getter
    private final StringProperty rawText = new SimpleStringProperty();

    @Getter
    private final ObservableBooleanValue isQuickConnectString = Bindings.createBooleanBinding(
            () -> {
                var v = rawText.getValue();
                if (v == null) {
                    return false;
                }

                return QuickConnectProvider.find(v).isPresent();
            },
            rawText);

    @Getter
    private final ObservableBooleanValue isUrlString = Bindings.createBooleanBinding(
            () -> {
                var v = rawText.getValue();
                if (v == null) {
                    return false;
                }

                return LauncherUrlProvider.find(v).isPresent();
            },
            rawText);

    private final BooleanProperty forceFilter = new SimpleBooleanProperty();

    @Getter
    private final ObservableBooleanValue isSearchString = Bindings.createBooleanBinding(
            () -> {
                return rawText.getValue() != null
                        && (forceFilter.getValue() || (!isUrlString.getValue() && !isQuickConnectString.getValue()));
            },
            rawText,
            isQuickConnectString,
            isUrlString,
            forceFilter);

    @Getter
    private final ObservableValue<StoreFilter> effectiveFilter = Bindings.createObjectBinding(
            () -> {
                return isSearchString.get() ? StoreFilter.of(rawText.getValue()) : null;
            },
            rawText,
            isSearchString,
            forceFilter);

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

        INSTANCE.addListeners();
    }

    private void addListeners() {
        var updateCount = new SimpleIntegerProperty(0);
        fieldText.subscribe(ignored -> {
            forceFilter.set(false);

            var index = updateCount.getValue() + 1;
            updateCount.setValue(index);

            GlobalTimer.delay(() -> {
                if (index != updateCount.get()) {
                    return;
                }

                Platform.runLater(() -> {
                    rawText.setValue(fieldText.getValue());
                });
            }, Duration.ofMillis(350));
        });
    }

    public static void reset() {
        AppCache.update("recentSearches", INSTANCE.recentSearches.getList());
        AppCache.update("recentQuickConnections", INSTANCE.recentQuickConnections.getList());
        INSTANCE = null;
    }

    public void set(String s) {
        fieldText.setValue(s);
    }

    public void putFilter(String s) {
        synchronized (recentSearches) {
            var l = recentSearches.getList();
            l.remove(s);
            if (l.size() == 3) {
                l.addFirst(s);
                l.removeLast();
            } else {
                l.addFirst(s);
            }
        }
    }

    public void putQuickConnect(String s) {
        synchronized (recentQuickConnections) {
            var l = recentQuickConnections.getList();
            l.remove(s);
            if (l.size() == 3) {
                l.addFirst(s);
                l.removeLast();
            } else {
                l.addFirst(s);
            }
        }
    }

    public boolean onFocusLost() {
        if (!isSearchString.get()) {
            return false;
        }

        return apply(false);
    }

    public boolean onApply() {
        return apply(true);
    }

    private boolean apply(boolean force) {
        rawText.setValue(fieldText.getValue());

        if (rawText.getValue() == null) {
            return false;
        }

        if (isSearchString.getValue()) {
            putFilter(rawText.getValue());
            return false;
        }

        if (isUrlString.getValue()) {
            var provider = LauncherUrlProvider.find(rawText.getValue());
            if (provider.isEmpty()) {
                return false;
            }

            try {
                var action = provider.get().createAction(URI.create(rawText.get()));
                action.executeAsync();
                return true;
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return false;
            }
        }

        if (isQuickConnectString.getValue()) {
            var prefix = !rawText.getValue().contains(" ");
            if (force && prefix) {
                forceFilter.set(true);
                return false;
            }

            var provider = QuickConnectProvider.find(rawText.getValue());
            if (provider.isEmpty()) {
                return false;
            }

            var r = StoreQuickConnect.launchQuickConnect(rawText.getValue());
            if (r) {
                putQuickConnect(rawText.getValue());
            }
            return r;
        }

        return false;
    }
}
