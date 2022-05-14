package online.search.neuralnet.util;

@SuppressWarnings("unused")
public class Util {

    public static String prettyString(double[][] data) {
        StringBuilder sb = new StringBuilder();
        for (double[] row : data) {
            sb.append("[");
            for (int col = 0; col < row.length; col++) {
                sb.append(String.format("%8.5f", row[col]));
                if (col < row.length - 1) sb.append(" ");
            }
            sb.append("]\n");
        }
        return sb.toString();
    }
}
