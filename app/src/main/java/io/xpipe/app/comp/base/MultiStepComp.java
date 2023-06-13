package io.xpipe.app.comp.base;

import atlantafx.base.controls.Spacer;
import com.jfoenix.controls.JFXTabPane;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.*;

import java.util.List;

public abstract class MultiStepComp extends Comp<CompStructure<VBox>> {
    private static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");
    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");
    private static final PseudoClass NEXT = PseudoClass.getPseudoClass("next");
    private final Property<Boolean> completed = new SimpleBooleanProperty();
    private final Property<Step<?>> currentStep = new SimpleObjectProperty<>();
    private List<Entry> entries;
    private int currentIndex = 0;

    private Step<?> getValue() {
        return currentStep.getValue();
    }

    private void set(Step<?> step) {
        currentStep.setValue(step);
    }

    public void next() {
        PlatformThread.runLaterIfNeeded(() -> {
            if (isFinished()) {
                return;
            }

            if (!getValue().canContinue()) {
                return;
            }

            if (isLastPage()) {
                getValue().onContinue();
                finish();
                currentIndex++;
                completed.setValue(true);
                return;
            }

            int index = Math.min(getCurrentIndex() + 1, entries.size() - 1);
            if (currentIndex == index) {
                return;
            }

            getValue().onContinue();
            entries.get(index).step().onInit();
            currentIndex = index;
            set(entries.get(index).step());
        });
    }

    public void previous() {
        PlatformThread.runLaterIfNeeded(() -> {
            int index = Math.max(currentIndex - 1, 0);
            if (currentIndex == index) {
                return;
            }

            getValue().onBack();
            currentIndex = index;
            set(entries.get(index).step());
        });
    }

    public boolean isCompleted(Entry e) {
        return entries.indexOf(e) < currentIndex;
    }

    public boolean isNext(Entry e) {
        return entries.indexOf(e) > currentIndex;
    }

    public boolean isCurrent(Entry e) {
        return entries.indexOf(e) == currentIndex;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isFirstPage() {
        return currentIndex == 0;
    }

    public boolean isLastPage() {
        return currentIndex == entries.size() - 1;
    }

    public boolean isFinished() {
        return currentIndex == entries.size();
    }

    protected Region createStepOverview(Region content) {
        if (entries.size() == 1) {
            return new Region();
        }

        HBox box = new HBox();
        box.setFillHeight(true);
        box.getStyleClass().add("top");
        box.setAlignment(Pos.CENTER);

        var comp = this;
        int number = 1;
        for (var entry : comp.getEntries()) {
            VBox element = new VBox();
            element.setFillWidth(true);
            element.setAlignment(Pos.CENTER);
            var label = new Label();
            label.textProperty().bind(entry.name);
            label.getStyleClass().add("name");
            element.getChildren().add(label);
            element.getStyleClass().add("entry");

            var line = new Region();
            boolean first = number == 1;
            boolean last = number == comp.getEntries().size();
            line.prefWidthProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> element.getWidth() / ((first || last) ? 2 : 1), element.widthProperty()));
            line.setMinWidth(0);
            line.getStyleClass().add("line");
            var lineBox = new HBox(line);
            lineBox.setFillHeight(true);
            if (first) {
                lineBox.setAlignment(Pos.CENTER_RIGHT);
            } else if (last) {
                lineBox.setAlignment(Pos.CENTER_LEFT);
            } else {
                lineBox.setAlignment(Pos.CENTER);
            }

            var circle = new Region();
            circle.getStyleClass().add("circle");
            var numberLabel = new Label("" + number);
            numberLabel.getStyleClass().add("number");
            var stack = new StackPane();
            stack.getChildren().add(lineBox);
            stack.getChildren().add(circle);
            stack.getChildren().add(numberLabel);
            stack.setAlignment(Pos.CENTER);
            element.getChildren().add(stack);

            Runnable updatePseudoClasses = () -> {
                element.pseudoClassStateChanged(CURRENT, comp.isCurrent(entry));
                element.pseudoClassStateChanged(NEXT, comp.isNext(entry));
                element.pseudoClassStateChanged(COMPLETED, comp.isCompleted(entry));
            };
            updatePseudoClasses.run();
            comp.currentStep.addListener((c, o, n) -> {
                updatePseudoClasses.run();
            });

            box.getChildren().add(element);

            element.prefWidthProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> content.getWidth() / comp.getEntries().size(), content.widthProperty()));

            number++;
        }

        return box;
    }

    protected Region createStepNavigation() {
        MultiStepComp comp = this;

        HBox buttons = new HBox();
        buttons.setFillHeight(true);
        buttons.getChildren().add(new Region());
        buttons.getChildren().add(new Spacer());
        buttons.getStyleClass().add("buttons");
        buttons.setSpacing(5);

        SimpleChangeListener.apply(currentStep, val -> {
            buttons.getChildren().set(0, val.bottom() != null ? val.bottom().createRegion() : new Region());
        });

        buttons.setAlignment(Pos.CENTER_RIGHT);
        var nextText = Bindings.createStringBinding(
                () -> isLastPage() ? AppI18n.get("finishStep") : AppI18n.get("nextStep"), currentStep);
        var nextButton = new ButtonComp(nextText, null, comp::next).apply(struc -> struc.get().setDefaultButton(true)).styleClass("next");

        var previousButton = new ButtonComp(AppI18n.observable("previousStep"), null, comp::previous)
                .styleClass("next")
                .apply(struc -> struc.get()
                        .disableProperty()
                        .bind(Bindings.createBooleanBinding(this::isFirstPage, currentStep)));

        previousButton.apply(
                s -> s.get().visibleProperty().bind(Bindings.createBooleanBinding(() -> !isFirstPage(), currentStep)));

        buttons.getChildren().add(previousButton.createRegion());
        buttons.getChildren().add(nextButton.createRegion());

        return buttons;
    }

    @Override
    public CompStructure<VBox> createBase() {
        this.entries = setup();
        this.set(entries.get(currentIndex).step);

        VBox content = new VBox();
        var comp = this;
        Region box = createStepOverview(content);

        var compContent = new JFXTabPane();
        compContent.getStyleClass().add("content");
        for (var ignored : comp.getEntries()) {
            compContent.getTabs().add(new Tab(null, null));
        }
        var entryR = comp.getValue().createRegion();
        entryR.getStyleClass().add("step");
        compContent.getTabs().set(currentIndex, new Tab(null, entryR));
        compContent.getSelectionModel().select(currentIndex);

        content.getChildren().addAll(box, compContent, createStepNavigation());
        content.getStyleClass().add("multi-step-comp");
        content.setFillWidth(true);
        VBox.setVgrow(compContent, Priority.ALWAYS);
        currentStep.addListener((c, o, n) -> {
            var nextTab = compContent
                    .getTabs()
                    .get(entries.stream().map(e -> e.step).toList().indexOf(n));
            if (nextTab.getContent() == null) {
                var createdRegion = n.createRegion();
                createdRegion.getStyleClass().add("step");
                nextTab.setContent(createdRegion);
            }
            compContent.getSelectionModel().select(comp.getCurrentIndex());
        });
        return new SimpleCompStructure<>(content);
    }

    protected abstract List<Entry> setup();

    protected abstract void finish();

    public List<Entry> getEntries() {
        return entries;
    }

    public ReadOnlyProperty<Boolean> completedProperty() {
        return completed;
    }

    public abstract static class Step<S extends CompStructure<?>> extends Comp<S> {

        public Comp<?> bottom() {
            return null;
        }

        public void onInit() {}

        public void onBack() {}

        public void onContinue() {}

        public boolean canContinue() {
            return true;
        }
    }

    public static record Entry(ObservableValue<String> name, Step<?> step) {}
}
