/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class NavigationHistory {

    private final IntegerProperty cursor = new SimpleIntegerProperty(0);
    private final List<String> history = new ArrayList<>();
    private final BooleanBinding canGoBack = Bindings.createBooleanBinding(
        () -> cursor.get() > 0 && history.size() > 1, cursor);
    private final BooleanBinding canGoForth = Bindings.createBooleanBinding(
        () -> cursor.get() < history.size() - 1, cursor);

    public String getCurrent() {
        return history.size() > 0 ? history.get(cursor.get()) : null;
    }

    public void cd(String s) {
        if (s == null) {
            return;
        }
        var lastString = getCurrent();
        if (Objects.equals(lastString, s)) {
            return;
        }

        if (canGoForth.get()) {
            history.subList(cursor.get() + 1, history.size()).clear();
        }
        history.add(s);
        cursor.set(history.size() - 1);
    }

    public Optional<String> back() {
        if (!canGoBack.get()) {
            return Optional.empty();
        }
        cursor.set(cursor.get() - 1);
        return Optional.of(history.get(cursor.get()));
    }

    public Optional<String> forth() {
        if (!canGoForth.get()) {
            return Optional.empty();
        }
        cursor.set(cursor.get() + 1);
        return Optional.of(history.get(cursor.get()));
    }

    public BooleanBinding canGoBackProperty() {
        return canGoBack;
    }

    public BooleanBinding canGoForthProperty() {
        return canGoForth;
    }
}
