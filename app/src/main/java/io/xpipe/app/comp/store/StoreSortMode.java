package io.xpipe.app.comp.store;

import io.xpipe.core.store.FixedChildStore;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public interface StoreSortMode {

    StoreSortMode ALPHABETICAL_DESC = new StoreSortMode() {
        @Override
        public StoreSection representative(StoreSection s) {
            return s;
        }

        @Override
        public String getId() {
            return "alphabetical-desc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.comparing(
                    e -> e.getWrapper().nameProperty().getValue().toLowerCase(Locale.ROOT));
        }
    };
    StoreSortMode ALPHABETICAL_ASC = new StoreSortMode() {
        @Override
        public StoreSection representative(StoreSection s) {
            return s;
        }

        @Override
        public String getId() {
            return "alphabetical-asc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.<StoreSection, String>comparing(
                            e -> e.getWrapper().nameProperty().getValue().toLowerCase(Locale.ROOT))
                    .reversed();
        }
    };
    StoreSortMode DATE_DESC = new StoreSortMode() {

        private Instant date(StoreSection s) {
            var la = s.getWrapper().getLastAccessApplied().getValue();
            if (la == null) {
                return s.getWrapper().getEntry().getStore() instanceof FixedChildStore ?
                        Instant.MIN : s.getWrapper().getEntry().getLastAccess();
            }

            return la;
        }

        @Override
        public StoreSection representative(StoreSection s) {
            return Stream.concat(
                            s.getShownChildren().getList().stream()
                                    .filter(section -> section.getWrapper()
                                            .getEntry()
                                            .getValidity()
                                            .isUsable())
                                    .map(this::representative),
                            Stream.of(s))
                    .max(Comparator.comparing(
                            section -> date(section)))
                    .orElseThrow();
        }

        @Override
        public String getId() {
            return "date-desc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.comparing(e -> {
                return date(e);
            });
        }
    };
    StoreSortMode DATE_ASC = new StoreSortMode() {

        private Instant date(StoreSection s) {
            var la = s.getWrapper().getLastAccessApplied().getValue();
            if (la == null) {
                return s.getWrapper().getEntry().getStore() instanceof FixedChildStore ?
                        Instant.MAX : s.getWrapper().getEntry().getLastAccess();
            }

            return la;
        }

        @Override
        public StoreSection representative(StoreSection s) {
            return Stream.concat(
                            s.getShownChildren().getList().stream()
                                    .filter(section -> section.getWrapper()
                                            .getEntry()
                                            .getValidity()
                                            .isUsable())
                                    .map(this::representative),
                            Stream.of(s))
                    .max(Comparator.comparing(section ->
                            section.getWrapper().getLastAccessApplied().getValue()))
                    .orElseThrow();
        }

        @Override
        public String getId() {
            return "date-asc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.<StoreSection, Instant>comparing(e -> {
                        return date(e);
                    })
                    .reversed();
        }
    };
    List<StoreSortMode> ALL = List.of(ALPHABETICAL_DESC, ALPHABETICAL_ASC, DATE_DESC, DATE_ASC);

    static Optional<StoreSortMode> fromId(String id) {
        return ALL.stream()
                .filter(storeSortMode -> storeSortMode.getId().equals(id))
                .findFirst();
    }

    static StoreSortMode getDefault() {
        return DATE_ASC;
    }

    StoreSection representative(StoreSection s);

    String getId();

    Comparator<StoreSection> comparator();
}
