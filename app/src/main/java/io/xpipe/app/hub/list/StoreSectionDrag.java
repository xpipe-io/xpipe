package io.xpipe.app.hub.list;

import io.xpipe.app.hub.category.StoreCategoryWrapper;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.storage.DataStorage;

import lombok.Value;
import lombok.With;

import java.util.List;
import java.util.Optional;

@Value
public class StoreSectionDrag {

    public enum Order {
        BEFORE,
        AFTER
    }

    public interface Target {

        Order getOrder();

        Optional<StoreSection> getSectionTarget();

        void execute(List<StoreEntryWrapper> selection);
    }

    @Value
    public static class UndeterminedTarget implements Target {

        @Override
        public Order getOrder() {
            return null;
        }

        @Override
        public Optional<StoreSection> getSectionTarget() {
            return Optional.empty();
        }

        @Override
        public void execute(List<StoreEntryWrapper> selection) {}
    }

    @Value
    public static class SiblingTarget implements Target {

        StoreSection target;
        Order order;

        @Override
        public Optional<StoreSection> getSectionTarget() {
            return Optional.of(target);
        }

        @Override
        public void execute(List<StoreEntryWrapper> selection) {
            if (selection.stream().allMatch(wrapper -> target.getWrapper()
                            .getEntry()
                            .equals(DataStorage.get()
                                    .getDefaultDisplayParent(wrapper.getEntry())
                                    .orElse(null)))
                    && order == Order.AFTER) {
                target.insertSections(selection, 0, false, false);
                return;
            }

            var parent = StoreViewState.get()
                    .getParentSectionForWrapper(target.getWrapper())
                    .orElseThrow();
            var index = parent.getShownChildren().getList().indexOf(target);
            var pin = parent.getDepth() == 0
                    && selection.stream()
                            .anyMatch(wrapper -> wrapper.getPinToTop().get());
            parent.insertSections(selection, index, order == Order.AFTER, pin);
        }
    }

    @Value
    public static class TopLevelTarget implements Target {

        StoreSection target;
        Order order;

        @Override
        public Optional<StoreSection> getSectionTarget() {
            return Optional.of(target);
        }

        @Override
        public void execute(List<StoreEntryWrapper> selection) {
            var parent = StoreViewState.get()
                    .getParentSectionForWrapper(target.getWrapper())
                    .orElseThrow();
            var index = parent.getShownChildren().getList().indexOf(target);
            parent.insertSections(selection, index, order == Order.AFTER, true);
        }
    }

    @Value
    public static class CategoryTarget implements Target {

        StoreCategoryWrapper target;

        @Override
        public Order getOrder() {
            return null;
        }

        @Override
        public Optional<StoreSection> getSectionTarget() {
            return Optional.empty();
        }

        @Override
        public void execute(List<StoreEntryWrapper> selection) {
            for (StoreEntryWrapper w : selection) {
                w.moveTo(target.getCategory());
            }
        }
    }

    List<StoreEntryWrapper> selection;

    @With
    Target target;

    public boolean isChildSiblingTarget(StoreSection section) {
        var parent = StoreViewState.get().getParentSectionForWrapper(section.getWrapper());

        if (parent.isPresent()
                && selection.stream().allMatch(wrapper -> wrapper.getPinToTop().get())) {
            var defParent = DataStorage.get()
                    .getDefaultDisplayParent(selection.getFirst().getEntry());
            var allSameParent = selection.stream().allMatch(wrapper -> DataStorage.get()
                    .getDefaultDisplayParent(wrapper.getEntry())
                    .equals(defParent));
            if (allSameParent
                    && defParent.isPresent()
                    && parent.get().getWrapper() != null
                    && defParent.get().equals(parent.get().getEntry())) {
                return true;
            } else if (allSameParent
                    && defParent.isPresent()
                    && defParent.get().equals(section.getWrapper().getEntry())) {
                return true;
            }
        }

        return parent.isPresent()
                && selection.stream().allMatch(wrapper -> StoreViewState.get()
                        .getParentSectionForWrapper(wrapper)
                        .equals(parent));
    }

    public boolean isTopLevelTarget(StoreSection section) {
        return section.getDepth() == 1;
    }
}
