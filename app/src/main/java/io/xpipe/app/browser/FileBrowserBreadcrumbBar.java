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

public class FileBrowserBreadcrumbBar extends SimpleComp {

    private final OpenFileSystemModel model;

    public FileBrowserBreadcrumbBar(OpenFileSystemModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        Callback<Breadcrumbs.BreadCrumbItem<String>, ButtonBase> crumbFactory = crumb -> {
            var btn = new Button(FileNames.getFileName(crumb.getValue()), null);
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
                breadcrumbs.setDividerFactory(item -> item != null && !item.isLast() ? new Label(sc.get().getOsType().getFileSystemSeparator()) : null);
            }

            var elements = FileNames.splitHierarchy(val);
            Breadcrumbs.BreadCrumbItem<String> items = Breadcrumbs.buildTreeModel(elements.toArray(String[]::new));
            breadcrumbs.setSelectedCrumb(items);
        });

        if (crumbFactory != null) {
            breadcrumbs.setCrumbFactory(crumbFactory);
        }
        if (dividerFactory != null) {
            breadcrumbs.setDividerFactory(dividerFactory);
        }

        breadcrumbs.selectedCrumbProperty().addListener((obs, old, val) -> {
            model.cd(val.getValue()).ifPresent(s -> {
                model.cd(s);
            });
        });

        return breadcrumbs;
    }
}
