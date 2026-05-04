package io.xpipe.app.ext;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.comp.StoreSection;

import io.xpipe.app.hub.comp.SystemStateComp;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

public interface CountGroupStoreProvider extends DataStoreProvider {

    @Override
    default boolean includeInConnectionCount() {
        return false;
    }

    @Override
    default BaseRegionBuilder<?, ?> stateDisplay(StoreSection section) {
        return new SystemStateComp(Bindings.createObjectBinding(() -> {
            return section.getShownChildren().getList().isEmpty() ? SystemStateComp.State.OTHER : SystemStateComp.State.SUCCESS;
        }, section.getShownChildren().getList()));
    }

    @Override
    default ObservableValue<String> informationString(StoreSection section) {
        return Bindings.createStringBinding(
                () -> {
                    var all = section.getAllChildren().getList();
                    var shown = section.getShownChildren().getList();
                    if (all.size() == 0) {
                        return AppI18n.get("no" + getCountTranslationKey() + "s");
                    }

                    var string = all.size() == shown.size() ? all.size() : shown.size() + "/" + all.size();
                    return all.size() == 1
                                    ? AppI18n.get("has" + getCountTranslationKey(), string)
                                    : AppI18n.get("has" + getCountTranslationKey() + "s", string);
                },
                section.getShownChildren().getList(),
                section.getAllChildren().getList(),
                AppI18n.activeLanguage());
    }

    String getCountTranslationKey();
}
