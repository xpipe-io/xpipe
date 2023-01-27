package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.util.TranslationService;
import com.dlsc.preferencesfx.PreferencesFxEvent;
import com.dlsc.preferencesfx.history.History;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.PreferencesFxModel;
import com.dlsc.preferencesfx.util.SearchHandler;
import com.dlsc.preferencesfx.util.StorageHandler;
import com.dlsc.preferencesfx.view.*;
import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.event.EventType;

import java.util.List;

public class AppPreferencesFx {

    private PreferencesFxModel preferencesFxModel;

    private NavigationView navigationView;
    private NavigationPresenter navigationPresenter;

    private UndoRedoBox undoRedoBox;

    private BreadCrumbView breadCrumbView;
    private BreadCrumbPresenter breadCrumbPresenter;

    private CategoryController categoryController;

    private PreferencesFxView preferencesFxView;
    private PreferencesFxPresenter preferencesFxPresenter;

    private AppPreferencesFx(StorageHandler storageHandler, Category... categories) {
        preferencesFxModel = new PreferencesFxModel(storageHandler, new SearchHandler(), new History(), categories);

        configure();
    }

    public static AppPreferencesFx of(Category... categories) {
        return new AppPreferencesFx(new JsonStorageHandler(), categories);
    }

    private void configure() {
        preferencesFxModel.setSaveSettings(true);
        preferencesFxModel.setHistoryDebugState(true);
        preferencesFxModel.setInstantPersistent(false);
        preferencesFxModel.setButtonsVisible(false);
    }

    public void loadSettings() {
        // setting values are only loaded if they are present already
        preferencesFxModel.loadSettingValues();
    }

    public void setupControls() {
        undoRedoBox = new UndoRedoBox(preferencesFxModel.getHistory());

        breadCrumbView = new BreadCrumbView(preferencesFxModel, undoRedoBox);
        breadCrumbPresenter = new BreadCrumbPresenter(preferencesFxModel, breadCrumbView);

        categoryController = new CategoryController();
        initializeCategoryViews();

        // display initial category
        categoryController.setView(preferencesFxModel.getDisplayedCategory());

        navigationView = new NavigationView(preferencesFxModel);
        navigationPresenter = new NavigationPresenter(preferencesFxModel, navigationView);

        preferencesFxView =
                new PreferencesFxView(preferencesFxModel, navigationView, breadCrumbView, categoryController);
        preferencesFxPresenter = new PreferencesFxPresenter(preferencesFxModel, preferencesFxView) {
            @Override
            public void setupEventHandlers() {
                // Ignore window close
            }
        };
    }

    public ObjectProperty<TranslationService> translationServiceProperty() {
        return preferencesFxModel.translationServiceProperty();
    }

    /**
     * Prepares the CategoryController by creating CategoryView / CategoryPresenter pairs from all
     * Categories and loading them into the CategoryController.
     */
    private void initializeCategoryViews() {
        preferencesFxModel.getFlatCategoriesLst().forEach(category -> {
            CategoryView categoryView = new CategoryView(preferencesFxModel, category);
            CategoryPresenter categoryPresenter =
                    new CategoryPresenter(preferencesFxModel, category, categoryView, breadCrumbPresenter);
            categoryController.addView(category, categoryView, categoryPresenter);
        });
    }

    /**
     * Call this method to manually save the changed settings when showing the preferences by using
     * {@link #getView()}.
     */
    public void saveSettings() {
        preferencesFxModel.saveSettings();
        ((JsonStorageHandler) preferencesFxModel.getStorageHandler()).save();
    }

    /**
     * Call this method to undo all changes made in the settings when showing the preferences by using
     * {@link #getView()}.
     */
    public void discardChanges() {
        preferencesFxModel.discardChanges();
    }

    /**
     * Registers an event handler with the model. The handler is called when the model receives an
     * {@code Event} of the specified type during the bubbling phase of event delivery.
     *
     * @param eventType    the type of the events to receive by the handler
     * @param eventHandler the handler to register
     * @return PreferencesFx to allow for chaining.
     * @throws NullPointerException if either event type or handler are {@code null}.
     */
    public AppPreferencesFx addEventHandler(
            EventType<PreferencesFxEvent> eventType, EventHandler<? super PreferencesFxEvent> eventHandler) {
        preferencesFxModel.addEventHandler(eventType, eventHandler);
        return this;
    }

    /**
     * Returns a PreferencesFxView, so that it can be used as a Node.
     *
     * @return a PreferencesFxView, so that it can be used as a Node.
     */
    public PreferencesFxView getView() {
        return preferencesFxView;
    }

    public List<Category> getCategories() {
        return preferencesFxModel.getCategories();
    }
}
