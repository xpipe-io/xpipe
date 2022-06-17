package io.xpipe.core.dialog;

import io.xpipe.core.util.Secret;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class Dialog {

    public static Dialog empty() {
        return new Dialog() {
            @Override
            public DialogElement start() throws Exception {
                return null;
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                return null;
            }
        };
    }

    public static class Choice extends Dialog {

        private final ChoiceElement element;

        private Choice(String description, List<io.xpipe.core.dialog.Choice> elements, int selected) {
            this.element = new ChoiceElement(description, elements, selected);
        }

        @Override
        public DialogElement start() throws Exception {
            return element;
        }

        @Override
        protected DialogElement next(String answer) throws Exception {
            if (element.apply(answer)) {
                return null;
            }

            return element;
        }

        private int getSelected() {
            return element.getSelected();
        }
    }

    public static Dialog.Choice choice(String description, List<io.xpipe.core.dialog.Choice> elements, int selected) {
        Dialog.Choice c = new Dialog.Choice(description, elements, selected);
        c.evaluateTo(c::getSelected);
        return c;
    }

    @SafeVarargs
    public static <T> Dialog.Choice choice(String description, Function<T, String> toString, T def, T... vals) {
        var elements = Arrays.stream(vals).map(v -> new io.xpipe.core.dialog.Choice(null, toString.apply(v))).toList();
        var index = Arrays.asList(vals).indexOf(def);
        var c = choice(description, elements, index);
        c.evaluateTo(() -> vals[c.getSelected()]);
        return c;
    }

    public static class Query extends Dialog {

        private final QueryElement element;

        private Query(String description, boolean newLine, boolean required, Object value, QueryConverter<?> converter, boolean hidden) {
            this.element = new QueryElement(description, newLine, required,value, converter, hidden);
        }

        @Override
        public Optional<Map.Entry<String, String>> toValue() {
            return Optional.of(new AbstractMap.SimpleEntry<>(element.getDescription(), element.getValue()));
        }

        @Override
        public DialogElement start() throws Exception {
            return element;
        }

        @Override
        protected DialogElement next(String answer) throws Exception {
            if (element.apply(answer)) {
                return null;
            }

            return element;
        }

        private  <T> T getConvertedValue() {
            return element.getConvertedValue();
        }
    }

    public static Dialog.Query query(String description, boolean newLine, boolean required, Object value, QueryConverter<?> converter) {
        var q = new Dialog.Query(description, newLine, required, value, converter, false);
        q.evaluateTo(q::getConvertedValue);
        return q;
    }
    public static Dialog.Query querySecret(String description, boolean newLine, boolean required, Secret value) {
        var q = new Dialog.Query(description, newLine, required, value, QueryConverter.SECRET, true);
        q.evaluateTo(q::getConvertedValue);
        return q;
    }

    public static Dialog chain(Dialog... ds) {
        return new Dialog() {

            private int current = 0;

            @Override
            public DialogElement start() throws Exception {
                current = 0;
                eval = null;
                return ds[0].start();
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                DialogElement currentElement = ds[current].receive(answer);
                if (currentElement == null) {
                    DialogElement next = null;
                    while (current < ds.length - 1 && (next = ds[++current].start()) == null) {

                    };
                    return next;
                }

                return currentElement;
            }
        }.evaluateTo(ds[ds.length - 1]);
    }

    public static <T> Dialog repeatIf(Dialog d, Predicate<T> shouldRepeat) {
        return new Dialog() {


            @Override
            public DialogElement start() throws Exception {
                eval = null;
                return d.start();
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                var next = d.receive(answer);
                if (next == null) {
                    if (shouldRepeat.test(d.getResult())) {
                        return d.start();
                    }
                }

                return next;
            }
        }.evaluateTo(d).onCompletion(d.completion);
    }

    public static Dialog header(String msg) {
        return of(new HeaderElement(msg)).evaluateTo(() -> msg);
    }

    public static Dialog header(Supplier<String> msg) {
        final String[] msgEval = {null};
        return new Dialog() {
            @Override
            public DialogElement start() throws Exception {
                msgEval[0] = msg.get();
                return new HeaderElement(msgEval[0]);
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                return null;
            }
        }.evaluateTo(() -> msgEval[0]);
    }


    public static Dialog busy() {
        return of(new BusyElement());
    }

    public static interface FailableSupplier<T> {

        T get() throws Exception;
    }

    public static Dialog lazy(FailableSupplier<Dialog> d) {
        return new Dialog() {

            Dialog dialog;

            @Override
            public DialogElement start() throws Exception {
                eval = null;
                dialog = d.get();
                evaluateTo(dialog);
                return dialog.start();
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                return dialog.receive(answer);
            }
        };
    }

    public static Dialog of(DialogElement e) {
        return new Dialog() {


            @Override
            public DialogElement start() throws Exception {
                eval = null;
                return e;
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                if (e.apply(answer)) {
                    return null;
                }

                return e;
            }
        };
    }


    public static Dialog skipIf(Dialog d, Supplier<Boolean> check) {
        return new Dialog() {

            private Dialog active;

            @Override
            public DialogElement start() throws Exception {
                active = check.get() ? null : d;
                return active != null ? active.start() : null;
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                return active != null ? active.receive(answer) : null;
            }
        }.evaluateTo(d).onCompletion(d.completion);
    }

    public static <T> Dialog retryIf(Dialog d, Function<T, String> msg) {
        return new Dialog() {

            private boolean retry;

            @Override
            public DialogElement start() throws Exception {
                eval = null;
                return d.start();
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                if (retry) {
                    retry = false;
                    return d.start();
                }

                var next = d.receive(answer);
                if (next == null) {
                    var s = msg.apply(d.getResult());
                    if (s != null) {
                        retry = true;
                        return new HeaderElement(s);
                    }
                }

                return next;
            }
        }.evaluateTo(d.evaluation).onCompletion(d.completion);
    }

    public static Dialog fork(String description, List<io.xpipe.core.dialog.Choice> elements, int selected, Function<Integer, Dialog> c) {
        var choice = new ChoiceElement(description, elements, selected);
        return new Dialog() {

            private Dialog choiceMade;

            @Override
            public DialogElement start() throws Exception {
                choiceMade = null;
                eval = null;
                return choice;
            }

            @Override
            protected DialogElement next(String answer) throws Exception {
                if (choiceMade != null) {
                    var r = choiceMade.receive(answer);
                    return r;
                }

                if (choice.apply(answer)) {
                    choiceMade = c.apply(choice.getSelected());
                    return choiceMade != null ? choiceMade.start() : null;
                }

                return choice;
            }
        }.evaluateTo(() -> choice.getSelected());
    }

    protected Object eval;
    private Supplier<?> evaluation;
    private final List<Consumer<?>> completion = new ArrayList<>();

    public abstract DialogElement start() throws Exception;

    public Optional<Map.Entry<String, String>> toValue() {
        return Optional.empty();
    }

    public Dialog evaluateTo(Dialog d) {
        evaluation = d.evaluation;
        return this;
    }

    public Dialog evaluateTo(Supplier<?> s) {
        evaluation = s;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> Dialog map(Function<T, ?> s) {
        var oldEval = evaluation;
        evaluation = () -> s.apply((T) oldEval.get());
        return this;
    }

    public Dialog onCompletion(Consumer<?> s) {
        completion.add(s);
        return this;
    }

    public Dialog onCompletion(Runnable r) {
        completion.add(v -> r.run());
        return this;
    }

    public Dialog onCompletion(List<Consumer<?>> s) {
        completion.addAll(s);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) eval;
    }

    @SuppressWarnings("unchecked")
    public <T> void complete() {
        if (evaluation != null) {
            eval = evaluation.get();
            completion.forEach(c -> {
                Consumer<T> ct = (Consumer<T>) c;
                ct.accept((T) eval);
            });
        }
    }

    public final DialogElement receive(String answer) throws Exception {
        var next = next(answer);
        if (next == null) {
            complete();
        }
        return next;
    }

    protected abstract DialogElement next(String answer) throws Exception;
}
