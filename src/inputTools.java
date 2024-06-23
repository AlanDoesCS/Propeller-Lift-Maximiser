public class inputTools {
    public static double get(String input, String variableName) {
        double val;

        try {
            val = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid "+variableName+" value: \""+input+"\". Please enter a valid number.");
        }

        return val;
    }

    public static double[] getMultiple(String input, String variableName) {
        input = input.replaceAll("\\s+", "");
        String[] vals = input.split(",");
        double[] ret = new double[vals.length];

        for (int i = 0; i < vals.length; i++) {
            ret[i] = get(vals[i], variableName);
        }
        return ret;
    }
}
