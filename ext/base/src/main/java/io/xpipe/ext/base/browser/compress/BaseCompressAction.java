package io.xpipe.ext.base.browser.compress;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FilePath;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public abstract class BaseCompressAction implements BrowserAction, BranchAction {

    private final boolean directory;

    public BaseCompressAction(boolean directory) {this.directory = directory;}

    @Override
    public void init(OpenFileSystemModel model) throws Exception {
        var sc = model.getFileSystem().getShell().orElseThrow();

        var foundTar = CommandSupport.findProgram(sc, "tar");
        model.getCache().getInstalledApplications().put("tar", foundTar.isPresent());

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
            model.getCache().getInstalledApplications().put("zip", found.isPresent());
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
        return AppI18n.observable(directory ? "compressContents" : "compress");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var ext = List.of("zip", "tar", "tar.gz", "tgz", "7z", "rar", "xar");
        if (entries.stream().anyMatch(browserEntry -> ext.stream().anyMatch(s -> browserEntry.getRawFileEntry().getPath().toLowerCase().endsWith("." + s)))) {
            return false;
        }

        return directory ? entries.size() == 1 && entries.getFirst().getRawFileEntry().getKind() == FileKind.DIRECTORY : entries.size() >= 1;
    }

    @Override
    public List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return List.of(
                new Windows7zAction(),
                new WindowsZipAction(),
                new UnixZipAction(),
                new TarBasedAction(false) {
                   @Override
                   protected String getExtension() {
                       return "tar";
                   }
        },
                new TarBasedAction(true) {

                    @Override
                    protected String getExtension() {
                        return "tar.gz";
                    }
                });
    }

    private abstract class Action implements LeafAction {

        @Override
        public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
            var name = new SimpleStringProperty(directory ? entries.getFirst().getFileName() : null);
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
                if (directory) {
                    command.addQuoted(rel.toDirectory().toWindows() + "*");
                } else {
                    command.addFile(rel.toWindows());
                }
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
            var command = CommandBuilder.of().add("zip", "-r", "-");
            for (int i = 0; i < entries.size(); i++) {
                var rel = new FilePath(entries.get(i).getRawFileEntry().getPath()).relativize(base).toUnix();
                if (directory) {
                    command.add(".");
                } else {
                    command.addFile(rel);
                }
            }
            command.add(">").addFile(target);

            if (directory) {
                model.runAsync(() -> {
                    var sc = model.getFileSystem().getShell().orElseThrow();
                    sc.command(command).withWorkingDirectory(entries.getFirst().getRawFileEntry().getPath()).execute();
                }, true);
            } else {
                model.runCommandAsync(command, true);
            }
        }

        @Override
        protected String getExtension() {
            return "zip";
        }

        @Override
        public boolean isActive(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return model.getCache().getInstalledApplications().get("zip");
        }

        @Override
        public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS;
        }
    }

    private class Windows7zAction extends Action {

        @Override
        protected void create(String fileName, OpenFileSystemModel model, List<BrowserEntry> entries) {
            var base = new FilePath(model.getCurrentDirectory().getPath());
            var target = base.join(fileName);
            var command = CommandBuilder.of().addFile(model.getCache().getMultiPurposeCache().get("7zExecutable").toString()).add("a").add("-r").addFile(target);
            for (int i = 0; i < entries.size(); i++) {
                var rel = new FilePath(entries.get(i).getRawFileEntry().getPath()).relativize(base);
                if (directory) {
                    command.addQuoted(".\\" + rel.toDirectory().toWindows() + "*");
                } else {
                    command.addFile(rel.toWindows());
                }
            }

            model.runCommandAsync(command, true);
        }

        @Override
        protected String getExtension() {
            return "7z";
        }

        @Override
        public boolean isActive(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return model.getCache().getMultiPurposeCache().containsKey("7zExecutable");
        }

        @Override
        public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() == OsType.WINDOWS;
        }
    }

    private abstract class TarBasedAction extends Action {

        private final boolean gz;

        private TarBasedAction(boolean gz) {this.gz = gz;}

        @Override
        protected void create(String fileName, OpenFileSystemModel model, List<BrowserEntry> entries) {
            var tar = CommandBuilder.of().add("tar", "-c", "-v").addIf(gz, "-z").add("-f").addFile(fileName);
            var base = new FilePath(model.getCurrentDirectory().getPath());

            if (directory) {
                var dir = new FilePath(entries.getFirst().getRawFileEntry().getPath()).toDirectory().toUnix();
                var command = CommandBuilder.of().add("find").addFile(dir).add("|", "sed", "s,^" + dir + ",,", "|");
                command.add(tar).add("-C").addFile(dir).add("-T", "-");
                model.runCommandAsync(command, true);
            } else {
                var command = CommandBuilder.of().add(tar);
                for (BrowserEntry entry : entries) {
                    var rel = new FilePath(entry.getRawFileEntry().getPath()).relativize(base);
                    command.addFile(rel);
                }
                model.runCommandAsync(command, true);
            }
        }

        @Override
        public boolean isActive(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return model.getCache().getInstalledApplications().get("tar");
        }

        @Override
        public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS || !directory;
        }
    }
}
