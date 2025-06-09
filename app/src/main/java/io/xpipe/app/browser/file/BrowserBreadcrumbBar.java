package io.xpipe.app.browser.file;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FilePath;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import atlantafx.base.controls.Breadcrumbs;

import java.util.ArrayList;
import java.util.List;

public class BrowserBreadcrumbBar extends SimpleComp {

    private final BrowserFileSystemTabModel model;

    public BrowserBreadcrumbBar(BrowserFileSystemTabModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        Callback<Breadcrumbs.BreadCrumbItem<String>, ButtonBase> crumbFactory = crumb -> {
            var name = crumb.getValue().equals("/") ? "/" : FileNames.getFileName(crumb.getValue());
            var btn = new Button(name, null);
            btn.setMnemonicParsing(false);
            btn.setFocusTraversable(false);
            return btn;
        };
        return createBreadcrumbs(crumbFactory, null);
    }

    private Region createBreadcrumbs(
            Callback<Breadcrumbs.BreadCrumbItem<String>, ButtonBase> crumbFactory,
            Callback<Breadcrumbs.BreadCrumbItem<String>, ? extends Node> dividerFactory) {

        var breadcrumbs = new Breadcrumbs<String>();
        breadcrumbs.setMinWidth(0);
        model.getCurrentPath().subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (val == null) {
                    breadcrumbs.setSelectedCrumb(null);
                    return;
                }

                var sc = model.getFileSystem().getShell();
                if (sc.isEmpty()) {
                    breadcrumbs.setDividerFactory(item -> item != null && !item.isLast() ? new Label("/") : null);
                } else {
                    breadcrumbs.setDividerFactory(item -> {
                        if (item == null) {
                            return null;
                        }

                        if (item.isFirst() && item.getValue().equals("/")) {
                            return new Label("");
                        }

                        return new Label(sc.get().getOsType().getFileSystemSeparator());
                    });
                }

                var elements = createBreadcumbHierarchy(val);
                var modifiedElements = new ArrayList<>(elements);
                if (val.toString().startsWith("/")) {
                    modifiedElements.addFirst("/");
                }
                Breadcrumbs.BreadCrumbItem<String> items =
                        Breadcrumbs.buildTreeModel(modifiedElements.toArray(String[]::new));
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

    private List<String> createBreadcumbHierarchy(FilePath filePath) {
        var f = filePath.toString() + "/";
        var list = new ArrayList<String>();
        int lastElementStart = 0;
        for (int i = 0; i < f.length(); i++) {
            if (f.charAt(i) == '\\' || f.charAt(i) == '/') {
                if (i - lastElementStart > 0) {
                    list.add(f.substring(0, i));
                }

                lastElementStart = i + 1;
            }
        }
        return list;
    }
}
