package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DataStructureNodePointer {

    private final List<Element> path;

    public DataStructureNodePointer(List<Element> path) {
        this.path = path;

        if (path.size() == 0) {
            throw new IllegalArgumentException();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder fromBase(DataStructureNodePointer pointer) {
        return new Builder(pointer);
    }

    public String toString() {
        return "/" + path.stream().map(Element::toString).collect(Collectors.joining("/"));
    }

    public int size() {
        return path.size();
    }

    public boolean isValid(DataStructureNode input) {
        return get(input) != null;
    }

    public DataStructureNode get(DataStructureNode root) {
        DataStructureNode current = root;
        for (Element value : path) {
            var found = value.tryMatch(current);
            if (found == null) {
                return null;
            } else {
                current = found;
            }
        }
        return current;
    }

    public Optional<DataStructureNode> getIfPresent(DataStructureNode root) {
        return Optional.ofNullable(get(root));
    }

    public List<Element> getPath() {
        return path;
    }

    public static interface Element {

        DataStructureNode tryMatch(DataStructureNode n);

        default String getKey(DataStructureNode n) {
            return null;
        }
    }

    public static final record NameElement(String name) implements Element {

        @Override
        public DataStructureNode tryMatch(DataStructureNode n) {
            return n.forKeyIfPresent(name).orElse(null);
        }

        @Override
        public String getKey(DataStructureNode n) {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final record IndexElement(int index) implements Element {

        @Override
        public DataStructureNode tryMatch(DataStructureNode n) {
            if (n.size() > index) {
                return n.at(index);
            }
            return null;
        }

        @Override
        public String toString() {
            return "[" + index + "]";
        }
    }

    public static final record SupplierElement(Supplier<String> keySupplier) implements Element {

        @Override
        public DataStructureNode tryMatch(DataStructureNode n) {
            var name = keySupplier.get();
            if (name != null) {
                return n.forKeyIfPresent(name).orElse(null);
            }
            return null;
        }

        @Override
        public String getKey(DataStructureNode n) {
            return keySupplier.get();
        }

        @Override
        public String toString() {
            return "[$s]";
        }
    }

    public static final record FunctionElement(Function<DataStructureNode, String> keyFunc) implements Element {

        @Override
        public DataStructureNode tryMatch(DataStructureNode n) {
            var name = keyFunc.apply(n);
            if (name != null) {
                return n.forKeyIfPresent(name).orElse(null);
            }
            return null;
        }

        @Override
        public String getKey(DataStructureNode n) {
            return keyFunc.apply(n);
        }

        @Override
        public String toString() {
            return "[$s]";
        }
    }

    public static final record SelectorElement(Predicate<DataStructureNode> selector) implements Element {

        @Override
        public DataStructureNode tryMatch(DataStructureNode n) {
            var res = n.stream()
                    .filter(selector)
                    .findAny();
            return res.orElse(null);
        }

        @Override
        public String toString() {
            return "[$(...)]";
        }
    }

    public static class Builder {

        private final List<Element> path;

        public Builder() {
            this.path = new ArrayList<>();
        }

        private Builder(List<Element> path) {
            this.path = path;
        }

        public Builder(DataStructureNodePointer pointer) {
            this.path = new ArrayList<>(pointer.path);
        }

        public Builder copy() {
            return new Builder(new ArrayList<>(path));
        }


        public Builder name(String name) {
            path.add(new NameElement(name));
            return this;
        }

        public Builder index(int index) {
            path.add(new IndexElement(index));
            return this;
        }

        public Builder supplier(Supplier<String> keySupplier) {
            path.add(new SupplierElement(keySupplier));
            return this;
        }

        public Builder function(Function<DataStructureNode, String> keyFunc) {
            path.add(new FunctionElement(keyFunc));
            return this;
        }

        public Builder selector(Predicate<DataStructureNode> selector) {
            path.add(new SelectorElement(selector));
            return this;
        }

        public Builder pointerEvaluation(DataStructureNodePointer pointer) {
            return pointerEvaluation(pointer, n -> {
                if (!n.isValue()) {
                    return null;
                }
                return n.asString();
            });
        }

        public Builder pointerEvaluation(DataStructureNodePointer pointer, Function<DataStructureNode, String> converter) {
            path.add(new FunctionElement((current) -> {
                var res = pointer.get(current);
                if (res != null) {
                    return converter.apply(res);
                }
                return null;
            }));
            return this;
        }

        public DataStructureNodePointer build() {
            return new DataStructureNodePointer(path);
        }
    }
}
