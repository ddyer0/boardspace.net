package online.search.neuralnet.math;

import java.util.Random;

@SuppressWarnings("unused")
public class SharedRnd {

    private static Random rnd = new Random();

    public static Random getRnd() {
        return rnd;
    }

    public static void setRnd(Random rnd) {
        SharedRnd.rnd = rnd;
    }
}
