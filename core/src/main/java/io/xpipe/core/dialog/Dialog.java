package io.xpipe.core.dialog;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Dialog {

    private static class Sequence extends Dialog {

        private int index = 0;
        private final DialogElement[] es;

        public Sequence(DialogElement... es) {
            this.es = es;
        }

        @Override
        public DialogElement start() {
            index = 0;
            return es[0];
        }

        @Override
        public DialogElement receive(String answer) {
            if (es[index].apply(answer)) {
                if (index == es.length - 1) {
                    complete();
                    return null;
                } else {
                    return es[++index];
                }
            }

            return es[index];
        }
    }

    public static Dialog chain(DialogElement... es) {
        return new Dialog.Sequence(es);
    }

    public static Dialog chain(Dialog... ds) {
        return new Dialog() {

            private int current = 0;

            @Override
            public DialogElement start() {
                current = 0;
                return ds[0].start();
            }

            @Override
            public DialogElement receive(String answer) {
                DialogElement currentElement = ds[current].receive(answer);
                if (currentElement == null) {
                    ds[current].complete();
                    if (current == ds.length - 1) {
                        complete();
                        return null;
                    } else {
                        return ds[++current].start();
                    }
                }

                return currentElement;
            }
        };
    }

    public static Dialog repeatIf(Dialog d, Supplier<Boolean> shouldRepeat) {
        return new Dialog() {


            @Override
            public DialogElement start() {
                return d.start();
            }

            @Override
            public DialogElement receive(String answer) {
                var next = d.receive(answer);
                if (next == null) {
                    if (shouldRepeat.get()) {
                        return d.start();
                    }
                }

                return next;
            }
        }.evaluateTo(d.onCompletion);
    }

    public static Dialog of(DialogElement e) {
        return new Dialog() {


            @Override
            public DialogElement start() {
                return e;
            }

            @Override
            public DialogElement receive(String answer) {
                if (e.apply(answer)) {
                    complete();
                    return null;
                }

                return e;
            }
        };
    }

    public static Dialog retryIf(Dialog d, Supplier<String> msg) {
        return new Dialog() {

            private boolean retry;

            @Override
            public DialogElement start() {
                return d.start();
            }

            @Override
            public DialogElement receive(String answer) {
                if (retry) {
                    retry = false;
                    return d.start();
                }

                var next = d.receive(answer);
                if (next == null) {
                    var s = msg.get();
                    if (s != null) {
                        retry = true;
                        return new HeaderElement(s);
                    }
                }

                return next;
            }
        }.evaluateTo(d.onCompletion);
    }

    public static Dialog choice(ChoiceElement choice, Function<Integer, Dialog> c) {
        return new Dialog() {

            private Dialog choiceMade;

            @Override
            public DialogElement start() {
                choiceMade = null;
                return choice;
            }

            @Override
            public DialogElement receive(String answer) {
                if (choiceMade != null) {
                    var r = choiceMade.receive(answer);
                    if (r == null) {
                        complete();
                    }
                    return r;
                }

                if (choice.apply(answer)) {
                    choiceMade = c.apply(choice.getSelected());
                    return choiceMade.start();
                }

                return choice;
            }
        };
    }

    private Object eval;
    private Supplier<?> onCompletion;

    public abstract DialogElement start();

    public Dialog evaluateTo(Supplier<?> s) {
        onCompletion = s;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) eval;
    }

    public void complete() {
        if (onCompletion != null) {
            eval = onCompletion.get();
        }
    }

    public abstract DialogElement receive(String answer);
}
