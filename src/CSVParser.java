import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVParser {
    static String LTag = "Lift N", STag = "S", ReTag = "Re";
    public static ReCalculator.InputData getInputDataFromCSV(String csvPath, String delimiter) throws FileFormatException {
        String line;
        int LIndex=-1, SIndex=-1, ReIndex=-1;

        List<Double> sValues = new ArrayList<>();
        List<Double> reValues = new ArrayList<>();
        double L = 0;
        boolean firstLiftNRead = false;

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            // Skip the header line
            String[] header = br.readLine().split(delimiter);
            for (int i = 0; i < header.length; i++) {
                if (header[i].equalsIgnoreCase(LTag)) {
                    LIndex = i;
                } else if (header[i].equalsIgnoreCase(STag)) {
                    SIndex = i;
                } else if (header[i].equalsIgnoreCase(ReTag)) {
                    ReIndex = i;
                }
            }

            if ((LIndex==-1) || (SIndex==-1) || (ReIndex==-1)) throw new FileFormatException("CSV file: "+csvPath+" must contain '"+LTag+"', '"+STag+"' and '"+ReTag+"' in the header!");

            while ((line = br.readLine()) != null) {
                String[] values = line.split(delimiter);

                try {
                    double S = Double.parseDouble(values[5]);
                    double Re = Double.parseDouble(values[6]);

                    sValues.add(S);
                    reValues.add(Re);

                    if (!firstLiftNRead) {
                        L = Double.parseDouble(values[7]);
                        firstLiftNRead = true;

                        if (ReCalculator.DEBUG) System.out.println("L: " + L);
                    }
                    if (ReCalculator.DEBUG) System.out.println("S: " + S + ", Re: " + Re);
                } catch (NumberFormatException e) {
                    // Handle case where conversion to double fails
                    if (ReCalculator.DEBUG) System.out.println("Skipping line due to parsing error: " + line);
                } catch (ArrayIndexOutOfBoundsException e) {
                    break;
                }
            }
        } catch (IOException | FileFormatException e) {
            e.printStackTrace();
        }

        // Convert lists to arrays
        double[] sArray = sValues.stream().mapToDouble(Double::doubleValue).toArray();
        double[] reArray = reValues.stream().mapToDouble(Double::doubleValue).toArray();

        return new ReCalculator.InputData(L, sArray, reArray);
    }
}
