package io.xpipe.app.comp.store;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.process.OsType;

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

public class StoreIdentitiesIntroComp extends SimpleComp {

    private Region createIntro() {
        var title = new Label();
        title.textProperty().bind(AppI18n.observable("identitiesIntroTitle"));
        if (OsType.getLocal() != OsType.MACOS) {
            title.getStyleClass().add(Styles.TEXT_BOLD);
        }
        AppFontSizes.title(title);

        var introDesc = new Label();
        introDesc.textProperty().bind(AppI18n.observable("identitiesIntroText"));
        introDesc.setWrapText(true);
        introDesc.setMaxWidth(470);

        var img = new FontIcon("mdi2a-account-group");
        img.setIconSize(80);
        var text = new VBox(title, introDesc);
        text.setSpacing(5);
        text.setAlignment(Pos.CENTER_LEFT);
        var hbox = new HBox(img, text);
        hbox.setSpacing(55);
        hbox.setAlignment(Pos.CENTER);

        var addButton = new Button(null, new FontIcon("mdi2p-play-circle"));
        addButton.textProperty().bind(AppI18n.observable("createIdentity"));
        addButton.setOnAction(event -> {
            var canSync = DataStorage.get().supportsSync();
            var prov = canSync
                    ? DataStoreProviders.byId("syncedIdentity").orElseThrow()
                    : DataStoreProviders.byId("localIdentity").orElseThrow();
            StoreCreationDialog.showCreation(prov, DataStoreCreationCategory.IDENTITY);
            event.consume();
        });

        var addPane = new StackPane(addButton);
        addPane.setAlignment(Pos.CENTER);

        var v = new VBox(hbox, addPane);
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        v.setSpacing(20);
        v.getStyleClass().add("intro");
        return v;
    }

    private Region createBottom() {
        var title = new Label();
        title.textProperty().bind(AppI18n.observable("identitiesIntroBottomTitle"));
        if (OsType.getLocal() != OsType.MACOS) {
            title.getStyleClass().add(Styles.TEXT_BOLD);
        }
        AppFontSizes.title(title);

        var importDesc = new Label();
        importDesc.textProperty().bind(AppI18n.observable("identitiesIntroBottomText"));
        importDesc.setWrapText(true);
        importDesc.setMaxWidth(470);

        var syncButton = new Button(null, new FontIcon("mdi2p-play-circle"));
        syncButton.textProperty().bind(AppI18n.observable("setupSync"));
        syncButton.setOnAction(event -> {
            AppPrefs.get().selectCategory("vaultSync");
            event.consume();
        });

        var syncPane = new StackPane(syncButton);
        syncPane.setAlignment(Pos.CENTER);

        var fi = new FontIcon("mdi2g-git");
        fi.setIconSize(80);
        var img = new StackPane(fi);
        img.setPrefWidth(100);
        img.setPrefHeight(120);
        var text = new VBox(title, importDesc);
        text.setSpacing(5);
        text.setAlignment(Pos.CENTER_LEFT);
        var hbox = new HBox(img, text);
        hbox.setSpacing(35);
        hbox.setAlignment(Pos.CENTER);

        var v = new VBox(hbox, syncPane);
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        v.setSpacing(20);
        v.getStyleClass().add("intro");
        return v;
    }

    @Override
    public Region createSimple() {
        var intro = createIntro();
        var introImport = createBottom();
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
