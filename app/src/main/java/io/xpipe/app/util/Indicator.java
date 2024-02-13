package io.xpipe.app.util;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

public class Indicator {

    private static final Color lColor = Color.rgb(0x66, 0x66, 0x66);
    private static final Color rColor = Color.rgb(0x0f, 0x87, 0xc3);

    private static final PathElement[] ELEMS = new PathElement[] {
            new MoveTo(9.2362945,19.934046),
            new CubicCurveTo(-1.3360939,-0.28065, -1.9963146,-1.69366, -1.9796182,-2.95487),
            new CubicCurveTo(-0.1152909,-1.41268, -0.5046634,-3.07081, -1.920768,-3.72287),
            new CubicCurveTo(-1.4711631,-0.77284, -3.4574873,-0.11153, -4.69154031,-1.40244),
            new CubicCurveTo(-1.30616123,-1.40422, -0.5308003,-4.1855799, 1.46313121,-4.4219799),
            new CubicCurveTo(1.4290018,-0.25469, 3.1669517,-0.0875, 4.1676818,-1.36207),
            new CubicCurveTo(0.9172241,-1.12206, 0.9594176,-2.63766, 1.0685793,-4.01259),
            new CubicCurveTo(0.4020299,-1.95732999, 3.2823027,-2.72818999, 4.5638567,-1.15760999),
            new CubicCurveTo(1.215789,1.31824999, 0.738899,3.90740999, -1.103778,4.37267999),
            new CubicCurveTo(-1.3972543,0.40868, -3.0929979,0.0413, -4.2208253,1.16215),
            new CubicCurveTo(-1.3524806,1.26423, -1.3178578,3.29187, -1.1086673,4.9895199),
            new CubicCurveTo(0.167826,1.28946, 1.0091133,2.5347, 2.3196964,2.86608),
            new CubicCurveTo(1.6253079,0.53477, 3.4876372,0.45004, 5.0294052,-0.30121),
            new CubicCurveTo(1.335829,-0.81654, 1.666839,-2.49408, 1.717756,-3.9432),
            new CubicCurveTo(0.08759,-1.1232899, 0.704887,-2.3061299, 1.871843,-2.5951699),
            new CubicCurveTo(1.534558,-0.50726, 3.390804,0.62784, 3.467269,2.28631),
            new CubicCurveTo(0.183147,1.4285099, -0.949563,2.9179999, -2.431156,2.9383699),
            new CubicCurveTo(-1.390597,0.17337, -3.074035,0.18128, -3.971365,1.45069),
            new CubicCurveTo(-0.99314,1.271, -0.676157,2.98683, -1.1715,4.43018),
            new CubicCurveTo(-0.518248,1.11436, -1.909118,1.63902, -3.0700005,1.37803),
            new ClosePath()
    };

    static {
        for(int i = 1; i < ELEMS.length; ++i) {
            ELEMS[i].setAbsolute(false);
        }
    }

    private final Path left;
    private final Path right;
    private final Group g;
    private final int steps;

    private boolean fw = true;
    private int step = 0;

    public Indicator(int ticksPerCycle, double scale) {
        this.steps = ticksPerCycle;

        left = new Path(ELEMS);
        right = new Path(ELEMS);

        left.setScaleX(scale);
        left.setScaleY(scale);
        right.setScaleX(-1 * scale);
        right.setScaleY(-1 * scale);
        right.setTranslateX(7.266 * scale);
        right.setOpacity(0.0);

        left.setStroke(null);
        right.setStroke(null);
        left.setFill(lColor);
        right.setFill(rColor);

        g = new Group(left, right);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                step();
            }
        };
        timer.start();
    }

    public Parent getNode() {
        return g;
    }

    private void step() {
        double lOpacity, rOpacity;

        step += fw ? 1 : -1;

        if(step == steps) {
            fw = false;
            lOpacity = 0.0;
            rOpacity = 1.0;
        } else if(step == 0) {
            fw = true;
            lOpacity = 1.0;
            rOpacity = 0.0;
        } else {
            lOpacity = 1.0 * (steps - step) / steps;
            rOpacity = 1.0 * step / steps;
        }

        left.setOpacity(lOpacity);
        right.setOpacity(rOpacity);
    }
}
