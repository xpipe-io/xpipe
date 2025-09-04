package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.beans.value.ObservableValue;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class StoreCreationQueueEntry extends AppLayoutModel.QueueEntry {

    public static StoreCreationQueueEntry of(StoreCreationModel model, ModalOverlay modal) {
        var provider = model.getProvider().getValue();
        var graphic = provider != null
                && provider.getDisplayIconFileName(model.getStore().get()) != null
                ? new LabelGraphic.ImageGraphic(
                provider.getDisplayIconFileName(model.getStore().get()), 20)
                : new LabelGraphic.IconGraphic("mdi2b-beaker-plus-outline");
        return new StoreCreationQueueEntry(AppI18n.observable(model.storeTypeNameKey() + "Add"), graphic, () -> {
            AppLayoutModel.get().selectConnections();
            modal.show();
        }, model.getExistingEntry());
    }

    public static Optional<AppLayoutModel.QueueEntry> findExisting(DataStoreEntry entry) {
        if (entry == null) {
            return Optional.empty();
        }

        return AppLayoutModel.get().getQueueEntries().stream()
                .filter(queueEntry -> queueEntry instanceof StoreCreationQueueEntry q && entry.equals(q.getEntry()))
                .findFirst();
    }

    DataStoreEntry entry;

    public StoreCreationQueueEntry(ObservableValue<String> name, LabelGraphic icon, Runnable action, DataStoreEntry entry) {
        super(name, icon, action);
        this.entry = entry;
    }
}
