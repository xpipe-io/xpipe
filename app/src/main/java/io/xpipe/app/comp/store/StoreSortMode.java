package io.xpipe.app.comp.store;

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
        @Override
        public StoreSection representative(StoreSection s) {
            return Stream.concat(
                            s.getShownChildren().stream()
                                    .filter(section -> section.getWrapper()
                                            .getEntry()
                                            .getValidity()
                                            .isUsable())
                                    .map(this::representative),
                            Stream.of(s))
                    .max(Comparator.comparing(
                            section -> section.getWrapper().getEntry().getLastAccess()))
                    .orElseThrow();
        }

        @Override
        public String getId() {
            return "date-desc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.comparing(e -> {
                return e.getWrapper().getEntry().getLastAccess();
            });
        }
    };
    StoreSortMode DATE_ASC = new StoreSortMode() {
        @Override
        public StoreSection representative(StoreSection s) {
            return Stream.concat(
                            s.getShownChildren().stream()
                                    .filter(section -> section.getWrapper()
                                            .getEntry()
                                            .getValidity()
                                            .isUsable())
                                    .map(this::representative),
                            Stream.of(s))
                    .max(Comparator.comparing(
                            section -> section.getWrapper().getEntry().getLastAccess()))
                    .orElseThrow();
        }

        @Override
        public String getId() {
            return "date-asc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.<StoreSection, Instant>comparing(e -> {
                        return e.getWrapper().getEntry().getLastAccess();
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

    StoreSection representative(StoreSection s);

    String getId();

    Comparator<StoreSection> comparator();
}
