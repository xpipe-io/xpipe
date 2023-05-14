package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.app.fxcomps.impl.TextFieldComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.Shortcuts;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
        Shortcuts.addShortcut(refreshBtn, new KeyCodeCombination(KeyCode.F5));

        var terminalBtn = new Button(null, new FontIcon("mdi2c-code-greater-than"));
        terminalBtn.setOnAction(e -> model.openTerminalAsync(model.getCurrentPath().get()));
        terminalBtn.disableProperty().bind(PlatformThread.sync(model.getNoDirectory()));

        var addBtn = new Button(null, new FontIcon("mdmz-plus"));
        addBtn.setOnAction(e -> {
            creatingProperty.set(true);
        });
        addBtn.disableProperty().bind(PlatformThread.sync(model.getNoDirectory()));
        Shortcuts.addShortcut(addBtn, new KeyCodeCombination(KeyCode.PLUS));

        var filter = new FileFilterComp(model.getFilter()).createStructure();
        Shortcuts.addShortcut(filter.toggleButton(), new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));

        var topBar = new ToolBar();
        topBar.getItems().setAll(
                backBtn,
                forthBtn,
                new Spacer(10),
                pathBar,
                filter.get(),
                refreshBtn,
                terminalBtn,
                addBtn
        );

        // ~

        FileListComp directoryView = new FileListComp(model.getFileList());

        var root = new VBox(topBar, directoryView);
        if (model.getBrowserModel().getMode() == FileBrowserModel.Mode.BROWSER) {
            root.getChildren().add(new FileBrowserStatusBarComp(model).createRegion());
        }
        VBox.setVgrow(directoryView, Priority.ALWAYS);
        root.setPadding(Insets.EMPTY);
        model.getFileList().predicateProperty().set(PREDICATE_NOT_HIDDEN);

        var pane = new StackPane();
        pane.getChildren().add(root);

        var creation = createCreationWindow(creatingProperty);
        var creationPane = new StackPane(creation);
        creationPane.setAlignment(Pos.CENTER);
        creationPane.setOnMouseClicked(event -> {
            creatingProperty.set(false);
        });
        pane.getChildren().add(creationPane);
        creationPane.visibleProperty().bind(creatingProperty);
        creationPane.managedProperty().bind(creatingProperty);

        return pane;
    }

    private Region createCreationWindow(BooleanProperty creating) {
        var creationName = new TextField();
        creating.addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                creationName.setText("");
            }
        });
        var createFileButton = new Button("File", new PrettyImageComp(new SimpleStringProperty("file_drag_icon.png"), 20, 20).createRegion());
        createFileButton.setOnAction(event -> {
            model.createFileAsync(creationName.getText());
            creating.set(false);
        });
        var createDirectoryButton = new Button("Directory", new PrettyImageComp(new SimpleStringProperty("folder_closed.svg"), 20, 20).createRegion());
        createDirectoryButton.setOnAction(event -> {
            model.createDirectoryAsync(creationName.getText());
            creating.set(false);
        });
        var buttonBar = new ButtonBar();
        buttonBar.getButtons().addAll(createFileButton, createDirectoryButton);
        var creationContent = new VBox(creationName, buttonBar);
        creationContent.setSpacing(15);
        var creation = new TitledPane("New ...", creationContent);
        creation.setMaxWidth(400);
        creation.setCollapsible(false);
        creationContent.setPadding(new Insets(15));
        creation.getStyleClass().add("elevated-3");

        creating.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                creationName.requestFocus();
            }
        });

        return creation;
    }
}
