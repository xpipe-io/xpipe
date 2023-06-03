package io.xpipe.app.comp.source;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataSourceTarget;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.util.CustomComboBoxBuilder;
import javafx.beans.property.Property;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Predicate;

public class DataSourceTargetChoiceComp extends Comp<CompStructure<ComboBox<Node>>> {

    private final Property<DataSourceTarget> selectedApplication;
    private final List<DataSourceTarget> all;

    private DataSourceTargetChoiceComp(Property<DataSourceTarget> selectedApplication, List<DataSourceTarget> all) {
        this.selectedApplication = selectedApplication;
        this.all = all;
    }

    public static DataSourceTargetChoiceComp create(
            Property<DataSourceTarget> selectedApplication, Predicate<DataSourceTarget> filter) {
        selectedApplication.addListener((observable, oldValue, val) -> {
            AppCache.update("application-last-used", val != null ? val.getId() : null);
        });
        var all =
                DataSourceTarget.getAll().stream().filter((p) -> filter.test(p)).toList();

        if (selectedApplication.getValue() == null) {
            String selectedId = AppCache.get("application-last-used", String.class, () -> null);
            var selectedProvider = selectedId != null
                    ? DataSourceTarget.byId(selectedId).filter(filter).orElse(null)
                    : null;
            selectedApplication.setValue(selectedProvider);
        }

        return new DataSourceTargetChoiceComp(selectedApplication, all);
    }

    private String getIconCode(DataSourceTarget p) {
        return p.getGraphicIcon() != null
                ? p.getGraphicIcon()
                : p.getCategory().equals(DataSourceTarget.Category.PROGRAMMING_LANGUAGE)
                        ? "mdi2c-code-tags"
                        : "mdral-indeterminate_check_box";
    }

    private Region createLabel(DataSourceTarget p) {
        var g = new FontIcon(getIconCode(p));
        var l = new Label(p.getName().getValue(), g);
        l.setAlignment(Pos.CENTER);
        g.iconColorProperty().bind(l.textFillProperty());
        return l;
    }

    @Override
    public CompStructure<ComboBox<Node>> createBase() {
        var addMoreLabel = new Label(AppI18n.get("addMore"), new FontIcon("mdmz-plus"));

        var builder = new CustomComboBoxBuilder<DataSourceTarget>(
                selectedApplication, app -> createLabel(app), dataSourceTarget -> dataSourceTarget.getName().getValue(), new Label(""), v -> true);

        // builder.addFilter((v, s) -> v.getName().getValue().toLowerCase().contains(s));

        builder.addHeader(AppI18n.get("programmingLanguages"));
        all.stream()
                .filter(p -> p.getCategory().equals(DataSourceTarget.Category.PROGRAMMING_LANGUAGE))
                .forEach(builder::add);

        builder.addHeader(AppI18n.get("applications"));
        all.stream()
                .filter(p -> p.getCategory().equals(DataSourceTarget.Category.APPLICATION))
                .forEach(builder::add);

        builder.addHeader(AppI18n.get("other"));
        all.stream()
                .filter(p -> p.getCategory().equals(DataSourceTarget.Category.OTHER))
                .forEach(builder::add);

        //        builder.addSeparator();
        //        builder.addAction(addMoreLabel, () -> {
        //
        //        });

        var cb = builder.build();
        cb.getStyleClass().add("application-choice-comp");
        cb.setMaxWidth(2000);
        return new SimpleCompStructure<>(cb);
    }
}
