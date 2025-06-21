package io.xpipe.app.action;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.ThreadHelper;

import lombok.experimental.SuperBuilder;

import java.util.*;
import java.util.function.Consumer;

@SuperBuilder
public abstract class AbstractAction {

    private static final Set<AbstractAction> active = new HashSet<>();
    private static boolean closed;
    private static Consumer<AbstractAction> pick;

    private static final AppLayoutModel.QueueEntry queueEntry = new AppLayoutModel.QueueEntry(
            AppI18n.observable("cancelActionPicker"), new LabelGraphic.IconGraphic("mdal-cancel_presentation"), () -> {
                cancelPick();
            });

    public static synchronized void expectPick() {
        if (pick != null) {
            return;
        }

        var show = !AppCache.getBoolean("pickIntroductionShown", false);
        if (show) {
            var modal = ModalOverlay.of("actionPickerTitle", AppDialog.dialogTextKey("actionPickerDescription"));
            modal.addButton(ModalButton.ok());
            modal.showAndWait();
            AppCache.update("pickIntroductionShown", true);
        }

        AppLayoutModel.get().getQueueEntries().add(queueEntry);
        pick = action -> {
            cancelPick();
            var modal = ModalOverlay.of("actionShortcuts", new ActionPickComp(action).prefWidth(600));
            modal.show();
        };
    }

    public static synchronized void cancelPick() {
        AppLayoutModel.get().getQueueEntries().remove(queueEntry);
        pick = null;
    }

    public static void reset() {
        closed = true;
        for (int i = 10; i > 0; i--) {
            synchronized (active) {
                var count = active.size();
                if (count == 0) {
                    break;
                }
            }

            // Wait 10s max
            ThreadHelper.sleep(1000);
        }

        synchronized (active) {
            for (AbstractAction abstractAction : active) {
                TrackEvent.trace("Action has not quit after timeout: " + abstractAction.toString());
            }
        }
    }

    public void executeSync() {
        if (closed) {
            return;
        }

        synchronized (AbstractAction.class) {
            if (pick != null) {
                TrackEvent.withTrace("Picked action").tags(toDisplayMap()).handle();
                pick.accept(this);
                pick = null;
                return;
            }
        }

        executeSyncImpl();
    }

    public void executeAsync() {
        if (closed) {
            return;
        }

        synchronized (AbstractAction.class) {
            if (pick != null) {
                TrackEvent.withTrace("Picked action").tags(toDisplayMap()).handle();
                pick.accept(this);
                pick = null;
                return;
            }
        }

        ThreadHelper.runAsync(() -> {
            executeSyncImpl();
        });
    }

    private void executeSyncImpl() {
        if (!ActionConfirmation.confirmAction(this)) {
            return;
        }

        if (closed) {
            return;
        }

        synchronized (active) {
            active.add(this);
        }

        TrackEvent.withTrace("Starting action execution").tags(toDisplayMap()).handle();

        try {
            if (!beforeExecute()) {
                return;
            }
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).handle();
            return;
        }

        try {
            executeImpl();
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).handle();
        } finally {
            afterExecute();
            synchronized (active) {
                active.remove(this);
            }

            TrackEvent.withTrace("Finished action execution").tag("id", getId()).handle();
        }
    }

    public String getId() {
        return getProvider().getId();
    }

    public String getDisplayName() {
        var id = getId();
        return id != null ? DataStoreFormatter.camelCaseToName(id) : "?";
    }

    public ActionProvider getProvider() {
        var clazz = getClass();
        var enc = clazz.getEnclosingClass();
        if (enc == null) {
            throw new IllegalStateException("No enclosing instance of " + clazz);
        }
        return ActionProvider.ALL.stream()
                .filter(actionProvider -> actionProvider.getClass().equals(enc))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    public String getShortcutName() {
        return getDisplayName();
    }

    public abstract void executeImpl() throws Exception;

    protected boolean beforeExecute() throws Exception {
        return true;
    }

    public boolean isMutation() {
        return false;
    }

    protected void afterExecute() {}

    public abstract Map<String, String> toDisplayMap();
}
