package io.xpipe.app.comp.store;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.comp.base.PrettySvgComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ScanAlert;
import io.xpipe.core.process.OsType;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import atlantafx.base.theme.Styles;
import org.kordamp.ikonli.javafx.FontIcon;

public class StoreIntroComp extends SimpleComp {

    private Region createIntro() {
        var title = new Label();
        title.textProperty().bind(AppI18n.observable("storeIntroTitle"));
        if (OsType.getLocal() != OsType.MACOS) {
            title.getStyleClass().add(Styles.TEXT_BOLD);
        }
        AppFont.setSize(title, 7);

        var introDesc = new Label();
        introDesc.textProperty().bind(AppI18n.observable("storeIntroDescription"));
        introDesc.setWrapText(true);
        introDesc.setMaxWidth(470);

        var scanButton = new Button(null, new FontIcon("mdi2m-magnify"));
        scanButton.textProperty().bind(AppI18n.observable("detectConnections"));
        scanButton.setOnAction(event -> ScanAlert.showAsync(DataStorage.get().local()));
        scanButton.setDefaultButton(true);
        var scanPane = new StackPane(scanButton);
        scanPane.setAlignment(Pos.CENTER);

        var img = PrettyImageHelper.ofFixedSize("graphics/Wave.svg", 80, 141).createRegion();
        var text = new VBox(title, introDesc);
        text.setSpacing(5);
        text.setAlignment(Pos.CENTER_LEFT);
        var hbox = new HBox(img, text);
        hbox.setSpacing(55);
        hbox.setAlignment(Pos.CENTER);

        var v = new VBox(hbox, scanPane);
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        v.setSpacing(10);
        v.getStyleClass().add("intro");
        return v;
    }

    private Region createImportIntro() {
        var title = new Label();
        title.textProperty().bind(AppI18n.observable("importConnectionsTitle"));
        if (OsType.getLocal() != OsType.MACOS) {
            title.getStyleClass().add(Styles.TEXT_BOLD);
        }
        AppFont.setSize(title, 7);

        var importDesc = new Label();
        importDesc.textProperty().bind(AppI18n.observable("storeIntroImportDescription"));
        importDesc.setWrapText(true);
        importDesc.setMaxWidth(470);

        var importButton = new Button(null, new FontIcon("mdi2g-git"));
        importButton.textProperty().bind(AppI18n.observable("importConnections"));
        importButton.setOnAction(event -> AppPrefs.get().selectCategory("sync"));
        var importPane = new StackPane(importButton);
        importPane.setAlignment(Pos.CENTER);

        var fi = new FontIcon("mdi2g-git");
        fi.setIconSize(80);
        var img = new StackPane(fi);
        img.setPrefWidth(100);
        img.setPrefHeight(150);
        var text = new VBox(title, importDesc);
        text.setSpacing(5);
        text.setAlignment(Pos.CENTER_LEFT);
        var hbox = new HBox(img, text);
        hbox.setSpacing(35);
        hbox.setAlignment(Pos.CENTER);

        var v = new VBox(hbox, importPane);
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        v.setSpacing(10);
        v.getStyleClass().add("intro");
        return v;
    }

    @Override
    public Region createSimple() {
        var intro = createIntro();
        var introImport = createImportIntro();
        var v = new VBox(intro, introImport);
        v.setSpacing(80);
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        var sp = new StackPane(v);
        sp.setPadding(new Insets(40, 0, 0, 0));
        sp.setAlignment(Pos.CENTER);
        sp.setPickOnBounds(false);
        return sp;
    }
}
