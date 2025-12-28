package io.xpipe.app.browser.file;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FilePath;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import atlantafx.base.controls.Breadcrumbs;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BrowserBreadcrumbBar extends SimpleComp {

    private final BrowserFileSystemTabModel model;
    private Instant lastHoverUpdate;

    public BrowserBreadcrumbBar(BrowserFileSystemTabModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        Callback<Breadcrumbs.BreadCrumbItem<FilePath>, ButtonBase> crumbFactory = crumb -> {
            var name = crumb.getValue().toString().equals("/")
                    ? "/"
                    : crumb.getValue().getFileName();
            var btn = new Button(name, null);
            btn.setMnemonicParsing(false);
            btn.setFocusTraversable(false);
            btn.setOnDragEntered(event -> onDragEntered(btn, crumb.getValue()));
            btn.setOnDragOver(event -> onDragOver(event));
            btn.setOnDragExited(event -> onDragExited(btn));
            return btn;
        };
        return createBreadcrumbs(crumbFactory, null);
    }

    private void onDragEntered(Button button, FilePath path) {
        button.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);

        var timestamp = Instant.now();
        lastHoverUpdate = timestamp;
        // Reduce printed window updates
        GlobalTimer.delay(
                () -> {
                    if (!timestamp.equals(lastHoverUpdate)) {
                        return;
                    }

                    model.cdAsync(path);
                },
                Duration.ofMillis(500));
    }

    private void onDragOver(DragEvent event) {
        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        event.consume();
    }

    private void onDragExited(Button button) {
        button.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), false);

        lastHoverUpdate = null;
    }

    private Region createBreadcrumbs(
            Callback<Breadcrumbs.BreadCrumbItem<FilePath>, ButtonBase> crumbFactory,
            Callback<Breadcrumbs.BreadCrumbItem<FilePath>, ? extends Node> dividerFactory) {

        var breadcrumbs = new Breadcrumbs<FilePath>();
        breadcrumbs.setMinWidth(0);
        model.getCurrentPath().subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (val == null) {
                    breadcrumbs.setSelectedCrumb(null);
                    return;
                }

                breadcrumbs.setDividerFactory(item -> {
                    if (item == null) {
                        return null;
                    }

                    if (item.isFirst() && item.getValue().toString().equals("/")) {
                        return new Label("");
                    }

                    return new Label(model.getFileSystem().getFileSeparator());
                });

                var elements = createBreadcrumbHierarchy(val);
                Breadcrumbs.BreadCrumbItem<FilePath> items =
                        Breadcrumbs.buildTreeModel(elements.toArray(FilePath[]::new));
                breadcrumbs.setSelectedCrumb(items);
            });
        });

        if (crumbFactory != null) {
            breadcrumbs.setCrumbFactory(crumbFactory);
        }
        if (dividerFactory != null) {
            breadcrumbs.setDividerFactory(dividerFactory);
        }

        breadcrumbs.selectedCrumbProperty().addListener((obs, old, val) -> {
            ThreadHelper.runAsync(() -> {
                BooleanScope.executeExclusive(model.getBusy(), () -> {
                    model.cdSync(val != null ? val.getValue().toString() : null);
                    var now = model.getCurrentPath().getValue();
                    // If we initiated a cd from the navbar, but it was rejected, reflect the changes
                    if (!Objects.equals(now, val != null ? val.getValue() : null)) {
                        Platform.runLater(() -> {
                            breadcrumbs.setSelectedCrumb(old);
                        });
                    }
                });
            });
        });

        return breadcrumbs;
    }

    private List<FilePath> createBreadcrumbHierarchy(FilePath filePath) {
        var f = filePath.toDirectory().toString();
        var list = new ArrayList<FilePath>();
        int lastElementStart = 0;
        for (int i = 0; i < f.length(); i++) {
            if (f.charAt(i) == '\\' || f.charAt(i) == '/') {
                if (i - lastElementStart > 0) {
                    list.add(FilePath.of(f.substring(0, i)).toDirectory());
                }

                lastElementStart = i + 1;
            }
        }

        if (filePath.toString().startsWith("/")) {
            list.addFirst(FilePath.of("/"));
        }

        return list;
    }
}
