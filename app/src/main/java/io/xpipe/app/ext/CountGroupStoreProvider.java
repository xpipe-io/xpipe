package io.xpipe.app.ext;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.comp.StoreSection;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

public interface CountGroupStoreProvider extends DataStoreProvider {

    @Override
    default ObservableValue<String> informationString(StoreSection section) {
        return Bindings.createStringBinding(
                () -> {
                    var all = section.getAllChildren().getList();
                    var shown = section.getShownChildren().getList();
                    if (shown.size() == 0) {
                        return AppI18n.get("noConnections");
                    }

                    var string = all.size() == shown.size() ? all.size() : shown.size() + "/" + all.size();
                    return all.size() > 0
                            ? (all.size() == 1
                                    ? AppI18n.get("hasConnection", string)
                                    : AppI18n.get("hasConnections", string))
                            : AppI18n.get("noConnections");
                },
                section.getShownChildren().getList(),
                section.getAllChildren().getList(),
                AppI18n.activeLanguage());
    }
}
