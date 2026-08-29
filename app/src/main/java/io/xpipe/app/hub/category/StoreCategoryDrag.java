package io.xpipe.app.hub.category;

import lombok.Value;
import lombok.With;

import java.util.Optional;

@Value
public class StoreCategoryDrag {

    public enum Order {
        BEFORE,
        AFTER
    }

    public interface Target {

        Order getOrder();

        Optional<StoreCategoryWrapper> getCategoryTarget();

        void execute(StoreCategoryWrapper selection);
    }

    @Value
    public static class UndeterminedTarget implements Target {

        @Override
        public Order getOrder() {
            return null;
        }

        @Override
        public Optional<StoreCategoryWrapper> getCategoryTarget() {
            return Optional.empty();
        }

        @Override
        public void execute(StoreCategoryWrapper selection) {}
    }

    @Value
    public static class CategoryTarget implements Target {

        StoreCategoryWrapper target;
        Order order;

        @Override
        public Optional<StoreCategoryWrapper> getCategoryTarget() {
            return Optional.of(this.target);
        }

        @Override
        public void execute(StoreCategoryWrapper selection) {
            target.insertSiblingCategory(selection, order == Order.AFTER);
        }
    }

    @Value
    public static class SubTarget implements Target {

        StoreCategoryWrapper target;

        @Override
        public Order getOrder() {
            return null;
        }

        @Override
        public Optional<StoreCategoryWrapper> getCategoryTarget() {
            return Optional.of(this.target);
        }

        @Override
        public void execute(StoreCategoryWrapper selection) {
            target.insertSubCategory(selection);
        }
    }

    StoreCategoryWrapper selection;

    @With
    Target target;

    public boolean isValidTarget(StoreCategoryWrapper category) {
        return category.getParent() != null && category.getParent().canMoveIntoThis(selection);
    }

    public boolean isValidSubTarget(StoreCategoryWrapper category) {
        return category.canMoveIntoThis(selection);
    }
}
