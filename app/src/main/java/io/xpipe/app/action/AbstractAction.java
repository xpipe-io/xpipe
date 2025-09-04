package io.xpipe.app.action;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.LicensedFeature;
import io.xpipe.app.util.ThreadHelper;

import lombok.experimental.SuperBuilder;

import java.util.*;
import java.util.function.Consumer;

@SuperBuilder
public abstract class AbstractAction {

    private static final Set<AbstractAction> active = new HashSet<>();
    private static boolean closed;
    private static Consumer<AbstractAction> pick;

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

    private static final AppLayoutModel.QueueEntry queueEntry = new AppLayoutModel.QueueEntry(
            AppI18n.observable("cancelActionPicker"), new LabelGraphic.IconGraphic("mdal-cancel_presentation"), () -> {
                cancelPick();
            });

    public static synchronized void cancelPick() {
        AppLayoutModel.get().getQueueEntries().remove(queueEntry);
        pick = null;
    }

    public static void reset() {
        closed = true;
        for (int i = 50; i > 0; i--) {
            synchronized (active) {
                var count = active.size();
                if (count == 0) {
                    break;
                }
            }

            // Wait 5s max
            ThreadHelper.sleep(100);
        }

        synchronized (active) {
            for (AbstractAction abstractAction : active) {
                TrackEvent.info("Action has not quit after timeout: " + abstractAction.toString());
            }
        }
    }

    public boolean executeSync() {
        if (closed) {
            return false;
        }

        synchronized (AbstractAction.class) {
            if (pick != null) {
                TrackEvent.withTrace("Picked action").tags(toDisplayMap()).handle();
                pick.accept(this);
                pick = null;
                return false;
            }
        }

        return executeSyncImpl(true);
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
            executeSyncImpl(true);
        });
    }

    public boolean executeSyncImpl(boolean confirm) {
        if (confirm && !ActionConfirmation.confirmAction(this)) {
            return false;
        }

        if (closed) {
            return false;
        }

        checkLicense();

        synchronized (active) {
            active.add(this);
        }

        TrackEvent.withTrace("Starting action execution").tags(toDisplayMap()).handle();

        try {
            if (!beforeExecute()) {
                return false;
            }
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).handle();
            return false;
        }

        try {
            executeImpl();
            return true;
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).handle();
            return false;
        } finally {
            afterExecute();
            synchronized (active) {
                active.remove(this);
            }
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

    public boolean forceConfirmation() {
        return false;
    }

    public LicensedFeature getLicensedFeature() {
        return null;
    }

    protected void checkLicense() {
        var feature = getLicensedFeature();
        if (feature != null) {
            feature.throwIfUnsupported();
        }
    }

    protected void afterExecute() {}

    public abstract Map<String, String> toDisplayMap();
}
