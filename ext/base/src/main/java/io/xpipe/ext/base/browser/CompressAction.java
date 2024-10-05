package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class CompressAction implements BrowserAction, BranchAction {

    @Override
    public void init(OpenFileSystemModel model) throws Exception {
        var sc = model.getFileSystem().getShell().orElseThrow();
        if (sc.getOsType() == OsType.WINDOWS) {
            var found = CommandSupport.findProgram(sc, "7z");
            if (found.isPresent()) {
                model.getCache().getMultiPurposeCache().put("7zExecutable","7z");
                return;
            }

            var pf = sc.command(sc.getShellDialect().getPrintEnvironmentVariableCommand("ProgramFiles")).readStdoutOrThrow();
            var loc = new FilePath(pf).join("7-Zip", "7z.exe").toWindows();
            if (model.getFileSystem().fileExists(loc)) {
                model.getCache().getMultiPurposeCache().put("7zExecutable", loc);
            }
        } else {
            var found = CommandSupport.findProgram(sc, "zip");
            model.getCache().getInstalledApplications().put("zip",found.isPresent());
        }
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2a-archive");
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("compress");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return entries.size() >= 1;
    }

    @Override
    public List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return List.of(
                new Windows7zAction(),
                new WindowsZipAction(),
                new UnixZipAction(),
                new TarBasedAction() {
                   @Override
                   protected String getExtension() {
                       return "tar";
                   }
        },
                new TarBasedAction() {

                    @Override
                    protected String getExtension() {
                        return "tar.gz";
                    }
                });
    }

    private abstract class Action implements LeafAction {

        @Override
        public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
            var name = new SimpleStringProperty();
            model.getOverlay()
                    .setValue(new ModalOverlayComp.OverlayContent(
                            "base.archiveName",
                            Comp.of(() -> {
                                        var creationName = new TextField();
                                        creationName.textProperty().bindBidirectional(name);
                                        return creationName;
                                    })
                                    .prefWidth(350),
                            null,
                            "finish",
                            () -> {
                                var fixedName = name.getValue();
                                if (fixedName == null) {
                                    return;
                                }

                                if (!fixedName.endsWith(getExtension())) {
                                    fixedName = fixedName + "." + getExtension();
                                }

                                create(fixedName, model, entries);
                            },
                            true));
        }

        @Override
        public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty("." + getExtension());
        }

        protected abstract void create(String fileName, OpenFileSystemModel model, List<BrowserEntry> entries);

        protected abstract String getExtension();
    }

    private class WindowsZipAction extends Action {

        @Override
        protected void create(String fileName, OpenFileSystemModel model, List<BrowserEntry> entries) {
            var base = new FilePath(model.getCurrentDirectory().getPath());
            var target = base.join(fileName);
            var command = CommandBuilder.of().add("Compress-Archive", "-Force", "-DestinationPath").addFile(target).add("-Path");
            for (int i = 0; i < entries.size(); i++) {
                var rel = new FilePath(entries.get(i).getRawFileEntry().getPath()).relativize(base);
                command.addFile(rel);
                if (i != entries.size() - 1) {
                    command.add(",");
                }
            }

            model.runAsync(() -> {
                var sc = model.getFileSystem().getShell().orElseThrow();
                if (ShellDialects.isPowershell(sc)) {
                    sc.command(command).withWorkingDirectory(base.toString()).execute();
                } else {
                    try (var sub = sc.subShell(ShellDialects.POWERSHELL)) {
                        sub.command(command).withWorkingDirectory(base.toString()).execute();
                    }
                }
            }, true);
        }

        @Override
        protected String getExtension() {
            return "zip";
        }

        @Override
        public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() == OsType.WINDOWS;
        }
    }

    private class UnixZipAction extends Action {

        @Override
        protected void create(String fileName, OpenFileSystemModel model, List<BrowserEntry> entries) {
            var base = new FilePath(model.getCurrentDirectory().getPath());
            var target = base.join(fileName);
            var command = CommandBuilder.of().add("zip").addFile(target);
            for (int i = 0; i < entries.size(); i++) {
                var rel = new FilePath(entries.get(i).getRawFileEntry().getPath()).relativize(base);
                command.addFile(rel);
            }

            model.runCommandAsync(command, true);
        }

        @Override
        protected String getExtension() {
            return "zip";
        }

        @Override
        public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS && model.getCache().getInstalledApplications().get("zip");
        }
    }

    private class Windows7zAction extends Action {

        @Override
        protected void create(String fileName, OpenFileSystemModel model, List<BrowserEntry> entries) {
            var base = new FilePath(model.getCurrentDirectory().getPath());
            var target = base.join(fileName);
            var command = CommandBuilder.of().addFile(model.getCache().getMultiPurposeCache().get("7zExecutable").toString()).add("a").addFile(target);
            for (int i = 0; i < entries.size(); i++) {
                var rel = new FilePath(entries.get(i).getRawFileEntry().getPath()).relativize(base);
                command.addFile(rel);
            }

            model.runCommandAsync(command, true);
        }

        @Override
        protected String getExtension() {
            return "7z";
        }

        @Override
        public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() == OsType.WINDOWS && model.getCache().getMultiPurposeCache().containsKey("7zExecutable");
        }
    }

    private abstract class TarBasedAction extends Action {

        @Override
        protected void create(String fileName, OpenFileSystemModel model, List<BrowserEntry> entries) {
            var command = CommandBuilder.of().add("tar", "-a", "-c", "-f").addFile(fileName);
            var base = new FilePath(model.getCurrentDirectory().getPath());
            for (BrowserEntry entry : entries) {
                var rel = new FilePath(entry.getRawFileEntry().getPath()).relativize(base);
                command.addFile(rel);
            }
            model.runCommandAsync(command, true);
        }
    }
}
