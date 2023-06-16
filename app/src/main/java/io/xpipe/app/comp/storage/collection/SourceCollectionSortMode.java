package io.xpipe.app.comp.storage.collection;

import io.xpipe.app.comp.storage.source.SourceEntryWrapper;

import java.time.Instant;
import java.util.Comparator;

public interface SourceCollectionSortMode {

    SourceCollectionSortMode ALPHABETICAL_DESC = new SourceCollectionSortMode() {
        @Override
        public String getId() {
            return "alphabetical-desc";
        }

        @Override
        public Comparator<SourceEntryWrapper> comparator() {
            return Comparator.<SourceEntryWrapper, String>comparing(
                            e -> e.getName().getValue())
                    .reversed();
        }
    };

    SourceCollectionSortMode ALPHABETICAL_ASC = new SourceCollectionSortMode() {
        @Override
        public String getId() {
            return "alphabetical-asc";
        }

        @Override
        public Comparator<SourceEntryWrapper> comparator() {
            return Comparator.<SourceEntryWrapper, String>comparing(
                    e -> e.getName().getValue());
        }
    };

    SourceCollectionSortMode DATE_DESC = new SourceCollectionSortMode() {
        @Override
        public String getId() {
            return "date-desc";
        }

        @Override
        public Comparator<SourceEntryWrapper> comparator() {
            return Comparator.<SourceEntryWrapper, Instant>comparing(
                            e -> e.getLastUsed().getValue())
                    .reversed();
        }
    };

    SourceCollectionSortMode DATE_ASC = new SourceCollectionSortMode() {
        @Override
        public String getId() {
            return "date-asc";
        }

        @Override
        public Comparator<SourceEntryWrapper> comparator() {
            return Comparator.comparing(e -> e.getLastUsed().getValue());
        }
    };

    String getId();

    Comparator<SourceEntryWrapper> comparator();
}
