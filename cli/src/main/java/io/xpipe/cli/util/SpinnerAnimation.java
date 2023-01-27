package io.xpipe.cli.util;

public class SpinnerAnimation {

    private final String text;
    private final boolean countSeconds;

    public SpinnerAnimation(String text, boolean countSeconds) {
        this.text = text;
        this.countSeconds = countSeconds;
    }

    private int totalMs;
    private int animationState;
    private String lastLine = "";

    public synchronized void update(int ms) {
        var line = text != null ? text : "";
        if (countSeconds) {
            totalMs += ms;
            var seconds = totalMs / 1000;
            line += " (" + seconds + "s)";
        }
        animate(" " + line);
    }

    private void print(String line) {
        if (lastLine.length() > line.length()) {
            StringBuilder temp = new StringBuilder();
            temp.append(" ".repeat(lastLine.length()));
            if (temp.length() > 1) {
                System.out.print("\r" + temp);
            }
        }
        System.out.print("\r" + line);
        lastLine = line;
    }

    private void animate(String line) {
        switch (animationState) {
            case 1 -> print("[ \\ ]" + line);
            case 2 -> print("[ | ]" + line);
            case 3 -> print("[ / ]" + line);
            default -> {
                animationState = 0;
                print("[ - ]" + line);
            }
        }
        animationState++;
    }

    public synchronized void clear() {
        System.out.print("\r" + " ".repeat(lastLine.length()));
        System.out.print("\r");
    }
}
