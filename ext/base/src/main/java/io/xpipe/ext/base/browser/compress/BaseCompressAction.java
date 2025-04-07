package io.xpipe.ext.base.browser.compress;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserBranchAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
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

public abstract class BaseCompressAction implements BrowserAction, BrowserBranchAction {

    private final boolean directory;

    public BaseCompressAction(boolean directory) {
        this.directory = directory;
    }

    @Override
    public void init(BrowserFileSystemTabModel model) throws Exception {
        var sc = model.getFileSystem().getShell().orElseThrow();

        var foundTar = CommandSupport.findProgram(sc, "tar");
        model.getCache().getInstalledApplications().put("tar", foundTar.isPresent());

        if (sc.getOsType() == OsType.WINDOWS) {
            var found = CommandSupport.findProgram(sc, "7z");
            if (found.isPresent()) {
                model.getCache().getMultiPurposeCache().put("7zExecutable", "7z");
                return;
            }

            var pf = sc.command(sc.getShellDialect().getPrintEnvironmentVariableCommand("ProgramFiles"))
                    .readStdoutOrThrow();
            var loc = FilePath.of(pf).join("7-Zip", "7z.exe").toWindows();
            if (model.getFileSystem().fileExists(loc)) {
                model.getCache().getMultiPurposeCache().put("7zExecutable", loc);
            }
        } else {
            var found = CommandSupport.findProgram(sc, "zip");
            model.getCache().getInstalledApplications().put("zip", found.isPresent());
        }
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2a-archive");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable(directory ? "compressContents" : "compress");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var ext = List.of("zip", "tar", "tar.gz", "tgz", "7z", "rar", "xar");
        if (entries.stream().anyMatch(browserEntry -> ext.stream().anyMatch(s -> browserEntry
                .getRawFileEntry()
                .getPath()
                .toString()
                .toLowerCase()
                .endsWith("." + s)))) {
            return false;
        }

        return directory
                ? entries.size() == 1 && entries.getFirst().getRawFileEntry().getKind() == FileKind.DIRECTORY
                : entries.size() >= 1;
    }

    @Override
    public List<BrowserLeafAction> getBranchingActions(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
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

    private abstract class Action implements BrowserLeafAction {

        @Override
        public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var name = new SimpleStringProperty(directory ? entries.getFirst().getFileName() : null);
            var modal = ModalOverlay.of(
                    "base.archiveName",
                    Comp.of(() -> {
                                var creationName = new TextField();
                                creationName.textProperty().bindBidirectional(name);
                                return creationName;
                            })
                            .prefWidth(350));
            modal.withDefaultButtons(() -> {
                var fixedName = name.getValue();
                if (fixedName == null) {
                    return;
                }

                if (!fixedName.endsWith(getExtension())) {
                    fixedName = fixedName + "." + getExtension();
                }

                create(fixedName, model, entries);
            });
            modal.show();
        }

        @Override
        public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return new SimpleStringProperty("." + getExtension());
        }

        protected abstract void create(String fileName, BrowserFileSystemTabModel model, List<BrowserEntry> entries);

        protected abstract String getExtension();
    }

    private class WindowsZipAction extends Action {

        @Override
        protected void create(String fileName, BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var base = model.getCurrentDirectory().getPath();
            var target = base.join(fileName);
            var command = CommandBuilder.of()
                    .add("Compress-Archive", "-Force", "-DestinationPath")
                    .addFile(target)
                    .add("-Path");
            for (int i = 0; i < entries.size(); i++) {
                var rel = entries.get(i).getRawFileEntry().getPath().relativize(base);
                if (directory) {
                    command.addQuoted(rel.toDirectory().toWindows() + "*");
                } else {
                    command.addFile(rel.toWindows());
                }
                if (i != entries.size() - 1) {
                    command.add(",");
                }
            }

            model.runAsync(
                    () -> {
                        var sc = model.getFileSystem().getShell().orElseThrow();
                        if (ShellDialects.isPowershell(sc)) {
                            sc.command(command).withWorkingDirectory(base).execute();
                        } else {
                            try (var sub = sc.subShell(ShellDialects.POWERSHELL)) {
                                sub.command(command).withWorkingDirectory(base).execute();
                            }
                        }
                    },
                    true);
        }

        @Override
        protected String getExtension() {
            return "zip";
        }

        @Override
        public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() == OsType.WINDOWS;
        }
    }

    private class UnixZipAction extends Action {

        @Override
        protected void create(String fileName, BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var base = model.getCurrentDirectory().getPath();
            var target = base.join(fileName);
            var command = CommandBuilder.of().add("zip", "-r", "-");
            for (BrowserEntry entry : entries) {
                var rel = entry.getRawFileEntry().getPath().relativize(base).toUnix();
                if (directory) {
                    command.add(".");
                } else {
                    command.addFile(rel);
                }
            }
            command.add(">").addFile(target);

            if (directory) {
                model.runAsync(
                        () -> {
                            var sc = model.getFileSystem().getShell().orElseThrow();
                            sc.command(command)
                                    .withWorkingDirectory(
                                            entries.getFirst().getRawFileEntry().getPath())
                                    .execute();
                        },
                        true);
            } else {
                model.runCommandAsync(command, true);
            }
        }

        @Override
        protected String getExtension() {
            return "zip";
        }

        @Override
        public boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return model.getCache().getInstalledApplications().get("zip");
        }

        @Override
        public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS;
        }
    }

    private class Windows7zAction extends Action {

        @Override
        protected void create(String fileName, BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var base = model.getCurrentDirectory().getPath();
            var target = base.join(fileName);
            var command = CommandBuilder.of()
                    .addFile(model.getCache()
                            .getMultiPurposeCache()
                            .get("7zExecutable")
                            .toString())
                    .add("a")
                    .add("-r")
                    .addFile(target);
            for (BrowserEntry entry : entries) {
                var rel = entry.getRawFileEntry().getPath().relativize(base);
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
        public boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return model.getCache().getMultiPurposeCache().containsKey("7zExecutable");
        }

        @Override
        public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() == OsType.WINDOWS;
        }
    }

    private abstract class TarBasedAction extends Action {

        private final boolean gz;

        private TarBasedAction(boolean gz) {
            this.gz = gz;
        }

        @Override
        protected void create(String fileName, BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            var tar = CommandBuilder.of()
                    .add("tar", "-c")
                    .addIf(gz, "-z")
                    .add("-f")
                    .addFile(fileName);
            var base = model.getCurrentDirectory().getPath();

            if (directory) {
                var dir = entries.getFirst().getRawFileEntry().getPath();
                // Fix for bsd find, remove /
                var command = CommandBuilder.of()
                        .add("find")
                        .addFile(dir.removeTrailingSlash().toUnix())
                        .add("|", "sed")
                        .addLiteral("s,^" + dir.toDirectory().toUnix() + "*,,")
                        .add("|");
                command.add(tar).add("-C").addFile(dir.toDirectory().toUnix()).add("-T", "-");
                model.runCommandAsync(command, true);
            } else {
                var command = CommandBuilder.of().add(tar);
                for (BrowserEntry entry : entries) {
                    var rel = entry.getRawFileEntry().getPath().relativize(base);
                    command.addFile(rel);
                }
                model.runCommandAsync(command, true);
            }
        }

        @Override
        public boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return model.getCache().getInstalledApplications().get("tar");
        }

        @Override
        public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
            return model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS || !directory;
        }
    }
}
