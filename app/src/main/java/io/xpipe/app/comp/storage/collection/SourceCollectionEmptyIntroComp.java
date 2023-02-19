package io.xpipe.app.comp.storage.collection;

import io.xpipe.app.comp.storage.DataSourceTypeComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.core.source.DataSourceType;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class SourceCollectionEmptyIntroComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var title = new Label(AppI18n.get("dataSourceIntroTitle"));
        AppFont.setSize(title, 7);
        title.getStyleClass().add("title-header");

        var descFi = new FontIcon("mdi2i-information-outline");
        var introDesc = new Label(AppI18n.get("dataSourceIntroDescription"));
        introDesc.heightProperty().addListener((c, o, n) -> {
            descFi.iconSizeProperty().set(n.intValue());
        });

        var tableFi = new DataSourceTypeComp(DataSourceType.TABLE, null).createRegion();
        var table = new Label(AppI18n.get("dataSourceIntroTable"), tableFi);
        tableFi.prefWidthProperty().bind(table.heightProperty());
        tableFi.prefHeightProperty().bind(table.heightProperty());

        var structureFi = new DataSourceTypeComp(DataSourceType.STRUCTURE, null).createRegion();
        var structure = new Label(AppI18n.get("dataSourceIntroStructure"), structureFi);
        structureFi.prefWidthProperty().bind(structure.heightProperty());
        structureFi.prefHeightProperty().bind(structure.heightProperty());

        var textFi = new DataSourceTypeComp(DataSourceType.TEXT, null).createRegion();
        var text = new Label(AppI18n.get("dataSourceIntroText"), textFi);
        textFi.prefWidthProperty().bind(text.heightProperty());
        textFi.prefHeightProperty().bind(text.heightProperty());

        var binaryFi = new DataSourceTypeComp(DataSourceType.RAW, null).createRegion();
        var binary = new Label(AppI18n.get("dataSourceIntroBinary"), binaryFi);
        binaryFi.prefWidthProperty().bind(binary.heightProperty());
        binaryFi.prefHeightProperty().bind(binary.heightProperty());

        var collectionFi = new DataSourceTypeComp(DataSourceType.COLLECTION, null).createRegion();
        var collection = new Label(AppI18n.get("dataSourceIntroCollection"), collectionFi);
        collectionFi.prefWidthProperty().bind(collection.heightProperty());
        collectionFi.prefHeightProperty().bind(collection.heightProperty());

        var v = new VBox(
                title,
                introDesc,
                new Separator(Orientation.HORIZONTAL),
                table,
                new Separator(Orientation.HORIZONTAL),
                structure,
                new Separator(Orientation.HORIZONTAL),
                text,
                new Separator(Orientation.HORIZONTAL),
                binary,
                new Separator(Orientation.HORIZONTAL),
                collection);
        v.setMinWidth(Region.USE_PREF_SIZE);
        v.setMaxWidth(Region.USE_PREF_SIZE);
        v.setMinHeight(Region.USE_PREF_SIZE);
        v.setMaxHeight(Region.USE_PREF_SIZE);

        v.setSpacing(10);
        v.getStyleClass().add("intro");

        var sp = new StackPane(v);
        sp.setAlignment(Pos.CENTER);
        return sp;
    }
}
