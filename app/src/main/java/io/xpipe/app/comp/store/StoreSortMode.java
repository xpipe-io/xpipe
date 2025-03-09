package io.xpipe.app.comp.store;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

public interface StoreSortMode {

    StoreSortMode ALPHABETICAL_DESC = new StoreSortMode() {

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
    StoreSortMode DATE_DESC = new StoreSortMode.DateSortMode() {

        protected Instant date(StoreSection s) {
            var la = s.getWrapper().getLastAccess().getValue();
            if (la == null) {
                return Instant.MAX;
            }

            return la;
        }

        @Override
        protected int compare(Instant s1, Instant s2) {
            return s2.compareTo(s1);
        }

        @Override
        public String getId() {
            return "date-desc";
        }
    };
    StoreSortMode DATE_ASC = new StoreSortMode.DateSortMode() {

        protected Instant date(StoreSection s) {
            var la = s.getWrapper().getLastAccess().getValue();
            if (la == null) {
                return Instant.MIN;
            }

            return la;
        }

        @Override
        protected int compare(Instant s1, Instant s2) {
            return s1.compareTo(s2);
        }

        @Override
        public String getId() {
            return "date-asc";
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

    String getId();

    Comparator<StoreSection> comparator();

    abstract class DateSortMode implements StoreSortMode {

        private int sortModeIndex = -1;
        private final Map<StoreSection, StoreSection> cachedRepresentatives = new IdentityHashMap<>();

        private StoreSection computeRepresentative(StoreSection s) {
            return Stream.concat(
                            s.getShownChildren().getList().stream()
                                    .filter(section -> section.getWrapper()
                                            .getEntry()
                                            .getValidity()
                                            .isUsable())
                                    .map(this::getRepresentative),
                            Stream.of(s))
                    .max(Comparator.comparing(section -> date(section)))
                    .orElseThrow();
        }

        private StoreSection getRepresentative(StoreSection s) {
            if (StoreViewState.get().getSortModeObservable().get() != sortModeIndex) {
                cachedRepresentatives.clear();
                sortModeIndex = StoreViewState.get().getSortModeObservable().get();
            }

            if (cachedRepresentatives.containsKey(s)) {
                return cachedRepresentatives.get(s);
            }

            var r = computeRepresentative(s);
            cachedRepresentatives.put(s, r);
            return r;
        }

        protected abstract Instant date(StoreSection s);

        protected abstract int compare(Instant s1, Instant s2);

        @Override
        public Comparator<StoreSection> comparator() {
            return (o1, o2) -> {
                var r1 = getRepresentative(o1);
                var r2 = getRepresentative(o2);
                return DateSortMode.this.compare(date(r1), date(r2));
            };
        }
    }
}
