package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ScanDialog;
import io.xpipe.core.OsType;

import javafx.beans.property.ReadOnlyIntegerWrapper;
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
        title.getStyleClass().add(Styles.TEXT_BOLD);
        AppFontSizes.title(title);

        var introDesc = new Label();
        introDesc.textProperty().bind(AppI18n.observable("storeIntroDescription"));
        introDesc.setWrapText(true);
        introDesc.setMaxWidth(470);

        var scanButton = new Button(null, new FontIcon("mdi2m-magnify"));
        scanButton.textProperty().bind(AppI18n.observable("detectConnections"));
        scanButton.setOnAction(
                event -> ScanDialog.showSingleAsync(DataStorage.get().local()));
        scanButton.getStyleClass().add(Styles.ACCENT);
        var scanPane = new StackPane(scanButton);
        scanPane.setAlignment(Pos.CENTER);

        var img = PrettyImageHelper.ofSpecificFixedSize("graphics/Wave.svg", 80, 144)
                .createRegion();
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
        title.getStyleClass().add(Styles.TEXT_BOLD);
        AppFontSizes.title(title);

        var importDesc = new Label();
        importDesc.textProperty().bind(AppI18n.observable("storeIntroImportDescription"));
        importDesc.setWrapText(true);
        importDesc.setMaxWidth(470);

        var importButton = new Button(null, new FontIcon("mdi2g-git"));
        importButton.textProperty().bind(AppI18n.observable("importConnections"));
        importButton.setOnAction(event -> AppPrefs.get().selectCategory("vaultSync"));
        var importPane = new StackPane(importButton);
        importPane.setAlignment(Pos.CENTER);

        var fi = new FontIcon("mdi2g-git");
        fi.iconSizeProperty().bind(new ReadOnlyIntegerWrapper(80));
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
