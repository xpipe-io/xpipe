package io.xpipe.app.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.entry.StoreEntryBadge;
import io.xpipe.app.hub.entry.StoreEntryInformation;
import io.xpipe.app.hub.section.StoreSection;

public interface CountGroupStoreProvider extends DataStoreProvider {

    @Override
    default boolean includeInConnectionCount() {
        return false;
    }

    @Override
    default StoreEntryInformation buildInformation(StoreSection section) {
        var all = section.getAllChildren().getList();
        var allCount = all.stream()
                .filter(s -> !excludeNonCountable()
                        || s.getWrapper().getEntry().getProvider().includeInConnectionCount())
                .count();
        var shown = section.getShownChildren().getList();
        var shownCount = shown.stream()
                .filter(s -> !excludeNonCountable()
                        || s.getWrapper().getEntry().getProvider().includeInConnectionCount())
                .count();
        if (allCount == 0) {
            return StoreEntryInformation.of(
                    StoreEntryBadge.ofFailure(AppI18n.get("no" + getCountTranslationKey() + "s"))
                            .withCompressedName("0/0"));
        }

        var string = shownCount + "/" + allCount;
        var s = allCount == 1
                ? AppI18n.get("has" + getCountTranslationKey(), string)
                : AppI18n.get("has" + getCountTranslationKey() + "s", string);
        var badge = allCount == shownCount
                ? StoreEntryBadge.ofSuccess(s)
                : shownCount > 0 ? StoreEntryBadge.ofIndeterminant(s) : StoreEntryBadge.ofFailure(s);
        return StoreEntryInformation.of(badge.withCompressedName(shownCount + "/" + allCount));
    }

    String getCountTranslationKey();

    default boolean excludeNonCountable() {
        return true;
    }
}
