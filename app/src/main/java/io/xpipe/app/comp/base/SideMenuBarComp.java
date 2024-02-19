package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.Augment;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.UserReportComp;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.Hyperlinks;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

public class SideMenuBarComp extends Comp<CompStructure<VBox>> {

    private final Property<AppLayoutModel.Entry> value;
    private final List<AppLayoutModel.Entry> entries;

    public SideMenuBarComp(Property<AppLayoutModel.Entry> value, List<AppLayoutModel.Entry> entries) {
        this.value = value;
        this.entries = entries;
    }

    @Override
    public CompStructure<VBox> createBase() {
        var vbox = new VBox();
        vbox.setFillWidth(true);

        var selectedBorder = Bindings.createObjectBinding(() -> {
            var c = Platform.getPreferences().getAccentColor();
            return new Border(new BorderStroke(c, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                    new BorderWidths(0, 3, 0, 0)));
        }, Platform.getPreferences().accentColorProperty());

        var hoverBorder = Bindings.createObjectBinding(() -> {
            var c = Platform.getPreferences().getAccentColor().darker();
            return new Border(new BorderStroke(c, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                    new BorderWidths(0, 3, 0, 0)));
        }, Platform.getPreferences().accentColorProperty());

        var noneBorder = Bindings.createObjectBinding(() -> {
            return new Border(new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                    new BorderWidths(0, 3, 0, 0)));
        }, Platform.getPreferences().accentColorProperty());

        var selected = PseudoClass.getPseudoClass("selected");
        entries.forEach(e -> {
            var b = new IconButtonComp(e.icon(), () -> value.setValue(e)).apply(new FancyTooltipAugment<>(e.name()));
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
                struc.get().pseudoClassStateChanged(selected, value.getValue().equals(e));
                value.addListener((c, o, n) -> {
                    PlatformThread.runLaterIfNeeded(() -> {
                        struc.get().pseudoClassStateChanged(selected, n.equals(e));
                    });
                });
                struc.get().borderProperty().bind(Bindings.createObjectBinding(() -> {
                    if (value.getValue().equals(e)) {
                        return selectedBorder.get();
                    }

                    if (struc.get().isHover()) {
                        return hoverBorder.get();
                    }

                    return noneBorder.get();
                }, struc.get().hoverProperty(), value, hoverBorder, selectedBorder, noneBorder));
            });
            b.accessibleText(e.name());
            vbox.getChildren().add(b.createRegion());
        });

        Augment<CompStructure<Button>> simpleBorders = struc -> {
            struc.get().borderProperty().bind(Bindings.createObjectBinding(() -> {
                if (struc.get().isHover()) {
                    return hoverBorder.get();
                }

                return noneBorder.get();
            }, struc.get().hoverProperty(), value, hoverBorder, selectedBorder, noneBorder));
        };

        {
            var b = new IconButtonComp(
                    "mdal-bug_report",
                    () -> {
                        var event = ErrorEvent.fromMessage("User Report");
                        if (AppLogs.get().isWriteToFile()) {
                            event.attachment(AppLogs.get().getSessionLogsDirectory());
                        }
                        UserReportComp.show(event.build());
                    })
                    .apply(new FancyTooltipAugment<>("reportIssue")).apply(simpleBorders).accessibleTextKey("reportIssue");
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
            });
            vbox.getChildren().add(b.createRegion());
        }

        {
            var b = new IconButtonComp("mdi2g-github", () -> Hyperlinks.open(Hyperlinks.GITHUB))
                    .apply(new FancyTooltipAugment<>("visitGithubRepository")).apply(simpleBorders).accessibleTextKey("visitGithubRepository");
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
            });
            vbox.getChildren().add(b.createRegion());
        }

        {
            var b = new IconButtonComp("mdi2d-discord", () -> Hyperlinks.open(Hyperlinks.DISCORD))
                    .apply(new FancyTooltipAugment<>("discord")).apply(simpleBorders).accessibleTextKey("discord");
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
            });
            vbox.getChildren().add(b.createRegion());
        }

        {
            var b = new IconButtonComp("mdi2u-update", () -> UpdateAvailableAlert.showIfNeeded())
                    .apply(new FancyTooltipAugment<>("updateAvailableTooltip")).accessibleTextKey("updateAvailableTooltip");
            b.apply(struc -> {
                AppFont.setSize(struc.get(), 2);
            });
            b.hide(PlatformThread.sync(Bindings.createBooleanBinding(
                    () -> {
                        return XPipeDistributionType.get()
                                        .getUpdateHandler()
                                        .getPreparedUpdate()
                                        .getValue()
                                == null;
                    },
                    XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate())));
            vbox.getChildren().add(b.createRegion());
        }

        var filler = new Button();
        filler.setDisable(true);
        filler.setMaxHeight(3000);
        vbox.getChildren().add(filler);
        VBox.setVgrow(filler, Priority.ALWAYS);
        filler.prefWidthProperty().bind(((Region) vbox.getChildren().getFirst()).widthProperty());

        vbox.getStyleClass().add("sidebar-comp");
        return new SimpleCompStructure<>(vbox);
    }
}
