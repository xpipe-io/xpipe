import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.action.XPipeUrlProvider;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.beacon.api.*;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.action.impl.*;
import io.xpipe.app.browser.menu.impl.*;
import io.xpipe.app.browser.menu.impl.compress.*;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.action.impl.*;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.store.DataStoreProvider;
import io.xpipe.app.util.AppJacksonModule;
import io.xpipe.app.util.JacksonMapper;
import io.xpipe.app.util.LicenseProvider;

import org.slf4j.spi.SLF4JServiceProvider;
import tools.jackson.databind.JacksonModule;

open module io.xpipe.app {
    exports io.xpipe.app.beacon;
    exports io.xpipe.app.core;
    exports io.xpipe.app.util;
    exports io.xpipe.app;
    exports io.xpipe.app.issue;
    exports io.xpipe.app.comp.base;
    exports io.xpipe.app.core.mode;
    exports io.xpipe.app.prefs;
    exports io.xpipe.app.storage;
    exports io.xpipe.app.update;
    exports io.xpipe.app.ext;
    exports io.xpipe.app.test;
    exports io.xpipe.app.browser.action;
    exports io.xpipe.app.browser;
    exports io.xpipe.app.browser.icon;
    exports io.xpipe.app.core.check;
    exports io.xpipe.app.terminal;
    exports io.xpipe.app.browser.file;
    exports io.xpipe.app.core.window;
    exports io.xpipe.app.comp;
    exports io.xpipe.app.icon;
    exports io.xpipe.app.pwman;
    exports io.xpipe.app.rdp;
    exports io.xpipe.app.vnc;
    exports io.xpipe.app.action;
    exports io.xpipe.app.browser.menu;
    exports io.xpipe.app.browser.menu.impl;
    exports io.xpipe.app.browser.action.impl;
    exports io.xpipe.app.browser.menu.impl.compress;
    exports io.xpipe.app.hub.action;
    exports io.xpipe.app.hub.action.impl;
    exports io.xpipe.app.process;
    exports io.xpipe.app.secret;
    exports io.xpipe.app.platform;
    exports io.xpipe.app.spice;
    exports io.xpipe.app.identity;
    exports io.xpipe.app.beacon.api;
    exports io.xpipe.app.webtop;
    exports io.xpipe.app.hub.section;
    exports io.xpipe.app.hub.entry;
    exports io.xpipe.app.hub.list;
    exports io.xpipe.app.hub.category;
    exports io.xpipe.app.hub.creation;
    exports io.xpipe.app.store;
    exports io.xpipe.app.fs;

    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.slf4j;
    requires org.slf4j.jdk.platform.logging;
    requires atlantafx.base;
    requires com.dlsc.atlantafx.themes;
    requires com.vladsch.flexmark;
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires net.synedra.validatorfx;
    requires io.xpipe.modulefs;
    requires static lombok;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires javafx.base;
    requires static org.junit.jupiter.api;
    requires javafx.controls;
    requires javafx.media;
    requires javafx.web;
    requires javafx.graphics;
    requires org.kordamp.ikonli.javafx;
    requires io.sentry;
    requires info.picocli;
    requires java.instrument;
    requires java.management;
    requires jdk.management;
    requires jdk.management.agent;
    requires com.shinyhut.vernacular;
    requires org.kordamp.ikonli.core;
    requires jdk.httpserver;
    requires com.github.weisj.jsvg;
    requires java.net.http;
    requires org.bouncycastle.provider;
    requires org.jetbrains.annotations;
    requires io.modelcontextprotocol.sdk.mcp;
    requires reactor.core;
    requires org.reactivestreams;

    // Required runtime modules
    requires jdk.charsets;
    requires jdk.crypto.cryptoki;
    requires jdk.localedata;
    requires jdk.accessibility;
    requires org.kordamp.ikonli.material2;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.bootstrapicons;
    requires jdk.zipfs;
    requires org.int4.fx.builders;
    requires com.google.zxing;
    requires java.desktop;

    uses ActionProvider;
    uses PrefsProvider;
    uses DataStoreProvider;
    uses ModuleLayerLoader;
    uses ScanProvider;
    uses BrowserActionProvider;
    uses LicenseProvider;
    uses io.xpipe.app.util.LicensedFeature;
    uses BeaconInterface;
    uses DataStorageExtensionProvider;
    uses ProcModuleProvider;
    uses ShellDialect;
    uses CloudSetupProvider;
    uses CliProvider;
    uses AuthModuleProvider;
    uses JacksonModule;

    provides ActionProvider with
            GradleRunMenuProvider,
            RefreshHubLeafProvider,
            SetupToolActionProvider,
            XPipeUrlProvider,
            OpenHubMenuLeafProvider,
            OpenSplitHubBatchProvider,
            EditHubLeafProvider,
            CloneHubLeafProvider,
            DownloadMenuProvider,
            RefreshChildrenHubLeafProvider,
            ScanHubBatchProvider,
            RunCommandInBrowserActionProvider,
            RunCommandInBackgroundActionProvider,
            RunCommandInTerminalActionProvider,
            ComputeDirectorySizesMenuProvider,
            FollowLinkMenuProvider,
            BackMenuProvider,
            ForwardMenuProvider,
            RefreshDirectoryMenuProvider,
            OpenFileDefaultMenuProvider,
            OpenFileWithMenuProvider,
            OpenDirectoryMenuProvider,
            OpenDirectoryInNewTabMenuProvider,
            ScanHubLeafProvider,
            StartOnInitHubLeafProvider,
            BrowseHubLeafProvider,
            RefreshActionProvider,
            ToggleActionProvider,
            OpenTerminalInDirectoryMenuProvider,
            OpenNativeFileDetailsMenuProvider,
            BrowseInNativeManagerMenuProvider,
            BrowseInNativeManagerActionProvider,
            ApplyFileEditActionProvider,
            TransferFilesActionProvider,
            EditFileMenuProvider,
            RunFileMenuProvider,
            RenameMenuProvider,
            ChmodMenuProvider,
            ChownMenuProvider,
            ChgrpActionProvider,
            ChgrpMenuProvider,
            CopyMenuProvider,
            CopyPathMenuProvider,
            PasteMenuProvider,
            CompressMenuProvider,
            NewItemMenuProvider,
            DeleteActionProvider,
            ComputeDirectorySizesActionProvider,
            DeleteMenuProvider,
            ChownActionProvider,
            ChmodActionProvider,
            TarActionProvider,
            UntarActionProvider,
            GzipActionProvider,
            GunzipActionProvider,
            ZipActionProvider,
            UnzipActionProvider,
            UnzipHereUnixMenuProvider,
            UnzipDirectoryUnixMenuProvider,
            UnzipHereWindowsActionProvider,
            UnzipDirectoryWindowsActionProvider,
            UntarHereMenuProvider,
            UntarGzHereMenuProvider,
            GunzipUnixMenuProvider,
            UntarDirectoryMenuProvider,
            UntarGzDirectoryMenuProvider,
            JavapMenuProvider,
            JarMenuProvider,
            MoveFileActionProvider,
            NewFileActionProvider,
            NewDirectoryActionProvider,
            NewLinkActionProvider,
            OpenDirectoryActionProvider,
            OpenFileDefaultActionProvider,
            OpenFileNativeDetailsActionProvider,
            OpenFileWithActionProvider;
    provides JacksonModule with
            AppJacksonModule;
    provides ModuleLayerLoader with
            AuthModuleProvider.Loader,
            ProcModuleProvider.Loader,
            BeaconInterface.Loader,
            DataStorageExtensionProvider.Loader,
            DataStoreProvider.Loader,
            JacksonMapper.Loader,
            ActionProvider.Loader,
            PrefsProvider.Loader,
            LicenseProvider.Loader,
            ScanProvider.Loader,
            ShellDialects.Loader,
            CloudSetupProvider.Loader,
            CliProvider.Loader;
    provides SLF4JServiceProvider with
            AppLogs.Slf4jProvider;
    provides BeaconInterface with
            ShellStartExchange,
            ShellStopExchange,
            ShellExecExchange,
            DaemonModeExchange,
            DaemonStatusExchange,
            DaemonFocusExchange,
            DaemonOpenExchange,
            DaemonStopExchange,
            HandshakeExchange,
            StoreQueryExchange,
            StoreInfoExchange,
            StoreRemoveExchange,
            StoreAddExchange,
            CategoryAddExchange,
            CategoryQueryExchange,
            CategoryInfoExchange,
            CategoryRemoveExchange,
            ActionExchange,
            StoreRefreshExchange,
            AskpassExchange,
            TerminalPrepareExchange,
            TerminalRegisterExchange,
            TerminalWaitExchange,
            TerminalLaunchExchange,
            TerminalExternalLaunchExchange,
            SshLaunchExchange,
            FsReadExchange,
            FsBlobExchange,
            FsWriteExchange,
            FsScriptExchange,
            DaemonVersionExchange,
            SecretEncryptExchange,
            SecretDecryptExchange;
}
