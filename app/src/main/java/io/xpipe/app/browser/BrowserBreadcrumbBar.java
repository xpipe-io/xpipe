package io.xpipe.app.browser;

import atlantafx.base.controls.Breadcrumbs;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.core.impl.FileNames;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import java.util.ArrayList;

public class BrowserBreadcrumbBar extends SimpleComp {

    private final OpenFileSystemModel model;

    public BrowserBreadcrumbBar(OpenFileSystemModel model) {
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
        SimpleChangeListener.apply(PlatformThread.sync(model.getCurrentPath()), val -> {
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

            var elements = FileNames.splitHierarchy(val);
            var modifiedElements = new ArrayList<>(elements);
            if (val.startsWith("/")) {
                modifiedElements.add(0, "/");
            }
            Breadcrumbs.BreadCrumbItem<String> items =
                    Breadcrumbs.buildTreeModel(modifiedElements.toArray(String[]::new));
            breadcrumbs.setSelectedCrumb(items);
        });

        if (crumbFactory != null) {
            breadcrumbs.setCrumbFactory(crumbFactory);
        }
        if (dividerFactory != null) {
            breadcrumbs.setDividerFactory(dividerFactory);
        }

        breadcrumbs.selectedCrumbProperty().addListener((obs, old, val) -> {
            model.cd(val != null ? val.getValue() : null).ifPresent(s -> {
                model.cd(s);
            });
        });

        return breadcrumbs;
    }
}
