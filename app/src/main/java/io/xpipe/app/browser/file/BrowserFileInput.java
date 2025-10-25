package io.xpipe.app.browser.file;

import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.ConnectionFileSystem;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.ext.FileInfo;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ElevationFunction;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.io.InputStream;

public interface BrowserFileInput {

    static BrowserFileInput openFileInput(BrowserFileSystemTabModel model, FileEntry file) throws Exception {
        if (model.isClosed()) {
            return BrowserFileInput.none();
        }

        var defOutput = createFileInputImpl(model, file, false);
        if (model.getFileSystem().getShell().isEmpty()) {
            return defOutput;
        }

        var sc = model.getFileSystem().getShell().orElseThrow();
        var requiresSudo =
                sc.getOsType() != OsType.WINDOWS && requiresSudo(model, (FileInfo.Unix) file.getInfo(), file.getPath());

        if (!requiresSudo) {
            return defOutput;
        }

        var elevate = AppDialog.confirm("fileReadSudo");
        if (!elevate) {
            return defOutput;
        }

        var rootOutput = createFileInputImpl(model, file, true);
        return rootOutput;
    }

    private static boolean requiresSudo(BrowserFileSystemTabModel model, FileInfo.Unix info, FilePath filePath)
            throws Exception {
        if (model.getFileSystem().getShell().isEmpty()) {
            return false;
        }

        var sc = model.getFileSystem().getShell().get();
        if (sc.view().isRoot()) {
            return false;
        }

        if (info != null) {
            var otherWrite = info.getPermissions().charAt(6) == 'r';
            if (otherWrite) {
                return false;
            }

            var userOwned = info.getUid() != null
                            && sc.view().getPasswdFile().getUidForUser(sc.view().user()) == info.getUid()
                    || info.getUser() != null && sc.view().user().equals(info.getUser());
            var userWrite = info.getPermissions().charAt(0) == 'r';
            if (userOwned && userWrite) {
                return false;
            }
        }

        var test = model.getFileSystem()
                .getShell()
                .orElseThrow()
                .command(CommandBuilder.of().add("test", "-r").addFile(filePath))
                .executeAndCheck();
        return !test;
    }

    private static BrowserFileInput createFileInputImpl(
            BrowserFileSystemTabModel model, FileEntry file, boolean elevate) throws Exception {
        var shell = model.getFileSystem().getShell();
        var sc = shell.isEmpty()
                ? null
                : elevate
                        ? shell.orElseThrow()
                                .identicalDialectSubShell()
                                .elevated(ElevationFunction.elevated(null))
                                .start()
                        : model.getFileSystem().getShell().orElseThrow().start();
        var fs = elevate ? new ConnectionFileSystem(sc) : model.getFileSystem();
        var output = new BrowserFileInput() {

            @Override
            public InputStream open() throws Exception {
                try {
                    return fs.openInput(file.getPath());
                } catch (Exception ex) {
                    if (elevate) {
                        fs.close();
                    }
                    throw ex;
                }
            }

            @Override
            public void onFinish() throws Exception {
                if (elevate) {
                    fs.close();
                }
            }
        };
        return output;
    }

    static BrowserFileInput none() {
        return new BrowserFileInput() {

            @Override
            public InputStream open() {
                return null;
            }

            @Override
            public void onFinish() throws Exception {}
        };
    }

    static BrowserFileInput of(InputStream in) {
        return new BrowserFileInput() {
            @Override
            public InputStream open() throws Exception {
                return in;
            }

            @Override
            public void onFinish() throws Exception {}
        };
    }

    InputStream open() throws Exception;

    void onFinish() throws Exception;
}
