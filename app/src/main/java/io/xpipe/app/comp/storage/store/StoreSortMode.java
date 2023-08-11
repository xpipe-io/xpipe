package io.xpipe.app.comp.storage.store;

import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;

public interface StoreSortMode {

    static StoreSortMode ALPHABETICAL_DESC = new StoreSortMode() {
        @Override
        public String getId() {
            return "alphabetical-desc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.<StoreSection, String>comparing(
                            e -> e.getWrapper().getName().toLowerCase(Locale.ROOT));
        }
    };

    static StoreSortMode ALPHABETICAL_ASC = new StoreSortMode() {
        @Override
        public String getId() {
            return "alphabetical-asc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.<StoreSection, String>comparing(
                    e -> e.getWrapper().getName().toLowerCase(Locale.ROOT))
                    .reversed();
        }
    };

    static StoreSortMode DATE_DESC = new StoreSortMode() {
        @Override
        public String getId() {
            return "date-desc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.<StoreSection, Instant>comparing(
                            e -> e.getWrapper().getLastAccess());
        }
    };

    static StoreSortMode DATE_ASC = new StoreSortMode() {
        @Override
        public String getId() {
            return "date-asc";
        }

        @Override
        public Comparator<StoreSection> comparator() {
            return Comparator.<StoreSection, Instant>comparing(e -> e.getWrapper().getLastAccess())
                    .reversed();
        }
    };

    String getId();

    Comparator<StoreSection> comparator();
}
