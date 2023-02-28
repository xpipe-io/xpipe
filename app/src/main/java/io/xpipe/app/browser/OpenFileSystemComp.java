package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.core.impl.FileNames;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import static io.xpipe.app.browser.FileListModel.PREDICATE_NOT_HIDDEN;
import static io.xpipe.app.util.Controls.iconButton;

public class OpenFileSystemComp extends SimpleComp {

    private final OpenFileSystemModel model;

    public OpenFileSystemComp(OpenFileSystemModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var creatingProperty = new SimpleBooleanProperty();
        var backBtn = iconButton(Feather.ARROW_LEFT, false);
        backBtn.setOnAction(e -> model.back());
        backBtn.disableProperty().bind(model.getHistory().canGoBackProperty().not());

        var forthBtn = iconButton(Feather.ARROW_RIGHT, false);
        forthBtn.setOnAction(e -> model.forth());
        forthBtn.disableProperty().bind(model.getHistory().canGoForthProperty().not());

        var path = new SimpleStringProperty(model.getCurrentPath().get());
        var pathBar = new TextFieldComp(path, true).createRegion();
        path.addListener((observable, oldValue, newValue) -> {
            var changed = model.cd(newValue);
            changed.ifPresent(path::set);
        });
        model.getCurrentPath().addListener((observable, oldValue, newValue) -> {
            path.set(newValue);
        });
        HBox.setHgrow(pathBar, Priority.ALWAYS);

        var refreshBtn = new Button(null, new FontIcon("mdmz-refresh"));
        refreshBtn.setOnAction(e -> model.refresh());

        var terminalBtn = new Button(null, new FontIcon("mdi2c-code-greater-than"));
        terminalBtn.setOnAction(e -> model.openTerminalAsync(model.getCurrentPath().get()));

        var addBtn = new Button(null, new FontIcon("mdmz-plus"));
        addBtn.setOnAction(e -> {
            creatingProperty.set(true);
        });

        var filter = new FileFilterComp(model.getFilter()).createRegion();

        var topBar = new ToolBar();
        topBar.getItems().setAll(
                backBtn,
                forthBtn,
                new Spacer(10),
                pathBar,
                filter,
                refreshBtn,
                terminalBtn,
                addBtn
        );

        // ~

        FileListComp directoryView = new FileListComp(model.getFileList());

        var root = new VBox(topBar, directoryView);
        VBox.setVgrow(directoryView, Priority.ALWAYS);
        root.setPadding(Insets.EMPTY);
        model.getFileList().predicateProperty().set(PREDICATE_NOT_HIDDEN);

        var pane = new StackPane();
        pane.getChildren().add(root);

        var creation = createCreationWindow(creatingProperty);
        var creationPain = new StackPane(creation);
        creationPain.setAlignment(Pos.CENTER);
        creationPain.setOnMouseClicked(event -> {
            creatingProperty.set(false);
        });
        pane.getChildren().add(creationPain);
        creationPain.visibleProperty().bind(creatingProperty);
        creationPain.managedProperty().bind(creatingProperty);

        return pane;
    }

    private Region createCreationWindow(BooleanProperty creating) {
        var creationName = new TextField();
        creating.addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                creationName.setText("");
            }
        });
        var createFileButton = new Button("Create file");
        createFileButton.setOnAction(event -> {
            model.createFileAsync(FileNames.join(model.getCurrentPath().get(), creationName.getText()));
            creating.set(false);
        });
        var createDirectoryButton = new Button("Create directory");
        createDirectoryButton.setOnAction(event -> {
            model.createDirectoryAsync(FileNames.join(model.getCurrentPath().get(), creationName.getText()));
            creating.set(false);
        });
        var buttonBar = new ButtonBar();
        buttonBar.getButtons().addAll(createFileButton, createDirectoryButton);
        var creationContent = new VBox(creationName, buttonBar);
        creationContent.setSpacing(15);
        var creation = new TitledPane("New", creationContent);
        creation.setMaxWidth(400);
        creation.setCollapsible(false);
        creationContent.setPadding(new Insets(15));
        creation.getStyleClass().add("elevated-3");
        return creation;
    }
}
