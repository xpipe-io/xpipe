package io.xpipe.app.comp.storage.collection;

import io.xpipe.app.comp.base.ListViewComp;

public class SourceCollectionListComp extends ListViewComp<SourceCollectionWrapper> {

    public SourceCollectionListComp() {
        super(
                SourceCollectionViewState.get().getShownGroups(),
                SourceCollectionViewState.get().getAllGroups(),
                SourceCollectionViewState.get().selectedGroupProperty(),
                SourceCollectionComp::new);
        styleClass("storage-group-list-comp");
        styleClass("bar");
        apply(s -> s.get().layout());
    }
}
