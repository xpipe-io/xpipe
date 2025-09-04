package io.xpipe.app.browser.file;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.core.FilePath;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import atlantafx.base.controls.Breadcrumbs;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

public class BrowserBreadcrumbBar extends SimpleComp {

    private final BrowserFileSystemTabModel model;

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
            return btn;
        };
        return createBreadcrumbs(crumbFactory, null);
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

                var elements = createBreadcumbHierarchy(val);
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
            model.cdAsync(val != null ? val.getValue() : null);
        });

        return breadcrumbs;
    }

    private List<FilePath> createBreadcumbHierarchy(FilePath filePath) {
        var f = filePath.toDirectory().toString();
        var list = new ArrayList<FilePath>();
        int lastElementStart = 0;
        for (int i = 0; i < f.length(); i++) {
            if (f.charAt(i) == '\\' || f.charAt(i) == '/') {
                if (i - lastElementStart > 0) {
                    list.add(FilePath.of(f.substring(0, i)));
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
