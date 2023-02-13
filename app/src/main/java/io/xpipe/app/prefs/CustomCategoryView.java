package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.PreferencesFxModel;
import com.dlsc.preferencesfx.view.CategoryView;

public class CustomCategoryView extends CategoryView {

    public CustomCategoryView(PreferencesFxModel model, Category categoryModel) {
        super(model, categoryModel);
    }

    public void initializeFormRenderer(Form form) {
        getChildren().clear();
        var preferencesFormRenderer = new CustomFormRenderer(form);
        getChildren().add(preferencesFormRenderer);
    }
}
