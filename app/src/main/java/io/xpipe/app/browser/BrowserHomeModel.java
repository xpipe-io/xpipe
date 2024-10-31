package io.xpipe.app.browser;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.file.BrowserFileListModel;
import io.xpipe.app.browser.file.BrowserFileTransferMode;
import io.xpipe.app.browser.file.BrowserFileTransferOperation;
import io.xpipe.app.browser.file.FileSystemHelper;
import io.xpipe.app.browser.fs.OpenFileSystemCache;
import io.xpipe.app.browser.fs.OpenFileSystemComp;
import io.xpipe.app.browser.fs.OpenFileSystemHistory;
import io.xpipe.app.browser.fs.OpenFileSystemSavedState;
import io.xpipe.app.browser.session.BrowserAbstractSessionModel;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.browser.session.BrowserSessionTab;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataColor;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.ShellOpenFunction;
import io.xpipe.core.store.*;
import io.xpipe.core.util.FailableConsumer;
import io.xpipe.core.util.FailableRunnable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class BrowserHomeModel extends BrowserSessionTab {

    public BrowserHomeModel(BrowserAbstractSessionModel<?> browserModel) {
        super(browserModel, AppI18n.get("overview"), null);
    }

    @Override
    public Comp<?> comp() {
        return new BrowserWelcomeComp((BrowserSessionModel) browserModel);
    }

    @Override
    public boolean canImmediatelyClose() {
        return true;
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public void close() {

    }

    @Override
    public String getIcon() {
        return null;
    }

    @Override
    public DataColor getColor() {
        return null;
    }

    @Override
    public boolean isCloseable() {
        return false;
    }
}
