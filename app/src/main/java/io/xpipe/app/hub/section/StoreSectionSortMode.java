package io.xpipe.app.hub.section;

import io.xpipe.app.storage.DataStoreEntry;

import java.net.InetAddress;
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
        public boolean supportsReordering() {
            return true;
        }

        @Override
        public Comparator<StoreSection> comparator(int updateIndex) {
            return Comparator.<StoreSection>comparingDouble(
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
        public boolean supportsReordering() {
            return true;
        }

        @Override
        public Comparator<StoreSection> comparator(int updateIndex) {
            return Comparator.comparingDouble(
                    e -> e.getWrapper().getOrderIndex().getValue());
        }
    };
    StoreSectionSortMode ALPHABETICAL_DESC = new StoreSectionSortMode() {

        @Override
        public boolean supportsReordering() {
            return false;
        }

        @Override
        public String getId() {
            return "alphabetical-desc";
        }

        @Override
        public Comparator<StoreSection> comparator(int updateIndex) {
            return new Comparator<>() {

                private int compare(InetAddress adr1, InetAddress adr2) {
                    byte[] ba1 = adr1.getAddress();
                    byte[] ba2 = adr2.getAddress();

                    if(ba1.length < ba2.length) {
                        return -1;
                    }

                    if(ba1.length > ba2.length) {
                        return 1;
                    }

                    for(int i = 0; i < ba1.length; i++) {
                        int b1 = unsignedByteToInt(ba1[i]);
                        int b2 = unsignedByteToInt(ba2[i]);
                        if(b1 == b2) {
                            continue;
                        }
                        if(b1 < b2) {
                            return -1;
                        }
                        else {
                            return 1;
                        }
                    }
                    return 0;
                }

                private int unsignedByteToInt(byte b) {
                    return (int) b & 0xFF;
                }

                @Override
                public int compare(StoreSection o1, StoreSection o2) {
                    var i1 = o1.getWrapper().getNameIpAddress().getValue();
                    var i2 = o2.getWrapper().getNameIpAddress().getValue();
                    if (i1 != null && i2 != null) {
                        return compare(i1, i2);
                    }

                    var n1 = o1.getWrapper().getName().getValue();
                    var n2 = o2.getWrapper().getName().getValue();
                    return n1.compareToIgnoreCase(n2);
                }
            };
        }
    };
    StoreSectionSortMode ALPHABETICAL_ASC = new StoreSectionSortMode() {
        @Override
        public String getId() {
            return "alphabetical-asc";
        }

        @Override
        public boolean supportsReordering() {
            return false;
        }

        @Override
        public Comparator<StoreSection> comparator(int updateIndex) {
            var comp = ALPHABETICAL_DESC.comparator(updateIndex);
            return comp.reversed();
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
        public boolean supportsReordering() {
            return false;
        }

        @Override
        public String getId() {
            return "date-desc";
        }
    };
    StoreSectionSortMode.DateSortMode DATE_ASC = new StoreSectionSortMode.DateSortMode() {

        @Override
        public boolean supportsReordering() {
            return false;
        }

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

    List<StoreSectionSortMode> ALL =
            List.of(INDEX_ASC, INDEX_DESC, ALPHABETICAL_DESC, ALPHABETICAL_ASC, DATE_DESC, DATE_ASC);

    static Optional<StoreSectionSortMode> fromId(String id) {
        return ALL.stream()
                .filter(storeSortMode -> storeSortMode.getId().equals(id))
                .findFirst();
    }

    boolean supportsReordering();

    String getId();

    Comparator<StoreSection> comparator(int updateIndex);

    abstract class DateSortMode implements StoreSectionSortMode {

        private final Map<StoreSection, StoreSection> cachedRepresentatives = new IdentityHashMap<>();
        private int entriesListObservableIndex = -1;

        public StoreSection computeRepresentative(StoreSection s, int updateIndex) {
            return Stream.concat(
                            s.getShownChildren().getList().stream()
                                    .filter(section ->
                                            section.getEntry().getValidity() != DataStoreEntry.Validity.LOAD_FAILED)
                                    .map(section -> getRepresentative(section, updateIndex)),
                            Stream.of(s))
                    .max(Comparator.comparing(section -> date(section)))
                    .orElseThrow();
        }

        public StoreSection getRepresentative(StoreSection s, int updateIndex) {
            if (updateIndex != entriesListObservableIndex) {
                cachedRepresentatives.clear();
                entriesListObservableIndex = updateIndex;
            }

            var found = cachedRepresentatives.get(s);
            if (found != null) {
                return found;
            }

            var r = computeRepresentative(s, updateIndex);
            cachedRepresentatives.put(s, r);
            return r;
        }

        public abstract Instant date(StoreSection s);

        protected abstract int compare(Instant s1, Instant s2);

        @Override
        public Comparator<StoreSection> comparator(int updateIndex) {
            return (o1, o2) -> {
                var r1 = getRepresentative(o1, updateIndex);
                var r2 = getRepresentative(o2, updateIndex);
                return DateSortMode.this.compare(date(r1), date(r2));
            };
        }
    }
}
