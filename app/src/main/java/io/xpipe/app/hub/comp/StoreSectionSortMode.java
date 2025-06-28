package io.xpipe.app.hub.comp;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

public interface StoreSectionSortMode {

    StoreSectionSortMode INDEX_DESC = new StoreSectionSortMode() {

        @Override
        public String getId() {
            return "index-desc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.<StoreSection>comparingInt(
                            e -> e.getWrapper().getOrderIndex().getValue())
                    .reversed();
        }
    };
    StoreSectionSortMode INDEX_ASC = new StoreSectionSortMode() {
        @Override
        public String getId() {
            return "index-asc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.comparingInt(e -> e.getWrapper().getOrderIndex().getValue());
        }
    };
    StoreSectionSortMode ALPHABETICAL_DESC = new StoreSectionSortMode() {

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
    StoreSectionSortMode ALPHABETICAL_ASC = new StoreSectionSortMode() {
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
    StoreSectionSortMode.DateSortMode DATE_DESC = new StoreSectionSortMode.DateSortMode() {

        public Instant date(StoreSection s) {
            var la = s.getWrapper().getLastAccess().getValue();
            if (la == null) {
                return Instant.MAX;
            }

            return la;
        }

        @Override
        protected int compare(Instant s1, Instant s2) {
            return s1.compareTo(s2);
        }

        @Override
        public String getId() {
            return "date-desc";
        }
    };
    StoreSectionSortMode.DateSortMode DATE_ASC = new StoreSectionSortMode.DateSortMode() {

        public Instant date(StoreSection s) {
            var la = s.getWrapper().getLastAccess().getValue();
            if (la == null) {
                return Instant.MIN;
            }

            return la;
        }

        @Override
        protected int compare(Instant s1, Instant s2) {
            return s2.compareTo(s1);
        }

        @Override
        public String getId() {
            return "date-asc";
        }
    };

    List<StoreSectionSortMode> ALL = List.of(ALPHABETICAL_DESC, ALPHABETICAL_ASC, DATE_DESC, DATE_ASC);

    static Optional<StoreSectionSortMode> fromId(String id) {
        return ALL.stream()
                .filter(storeSortMode -> storeSortMode.getId().equals(id))
                .findFirst();
    }

    String getId();

    Comparator<StoreSection> comparator();

    abstract class DateSortMode implements StoreSectionSortMode {

        private int entriesListObservableIndex = -1;
        private final Map<StoreSection, StoreSection> cachedRepresentatives = new IdentityHashMap<>();

        public StoreSection computeRepresentative(StoreSection s) {
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

        public StoreSection getRepresentative(StoreSection s) {
            if (StoreViewState.get().getEntriesListUpdateObservable().get() != entriesListObservableIndex) {
                cachedRepresentatives.clear();
                entriesListObservableIndex =
                        StoreViewState.get().getEntriesListUpdateObservable().get();
            }

            if (cachedRepresentatives.containsKey(s)) {
                return cachedRepresentatives.get(s);
            }

            var r = computeRepresentative(s);
            cachedRepresentatives.put(s, r);
            return r;
        }

        public abstract Instant date(StoreSection s);

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
