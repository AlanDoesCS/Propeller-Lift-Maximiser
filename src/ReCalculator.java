import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class ReCalculator {
    static final boolean DEBUG = false; // verbose
    static final double LTolerance = 0.01;
    /*
    Tolerance for values of L. Values within this range will maximise for CL/CD
    0.01 is 1%
     */
    static final String dataDirectory = "src/data";
    static final String csvPath = "src/table/table.csv";

    public static class InputData {
        public double L;
        public double[] S, Re;

        public InputData(double L, double[] S, double[] R) {
            this.L = L;
            this.S = S;
            this.Re = R;
        }

        public String toString() {
            return "L:  " + L + ",\nS:  " + Arrays.toString(S) + "\nRe: " + Arrays.toString(Re);
        }
    }

    public static class PolarData {
        public double re, aoa, cl, cd;

        public PolarData(double Re, double AOA, double CL, double CD) {
            this.re = Re;
            this.aoa = AOA;
            this.cl = CL;
            this.cd = CD;
        }

        double getL(double S) {
            return S * (cl - cd);
        }

        double getLCloseness(double targetL, double S) {
            return Math.abs(targetL - getL(S))/targetL;
        }

        public String toString() {
            return "RE="+re+", AOA="+aoa+", CL="+cl+", CD="+cd;
        }

        public double getCL_CD() {
            return cl/cd;
        }
    }

    public static class ReFileData {
        String fileName;
        double reValue;
        List<PolarData> polarDataList;

        public ReFileData(String fileName, double reValue, List<PolarData> polarDataList) {
            this.fileName = fileName;
            this.reValue = reValue;
            this.polarDataList = polarDataList;
        }

        public String toString() {
            return fileName+": RE="+reValue+", DataRowCount="+polarDataList.size();
        }
    }

    private static ReFileData findClosestReFile(List<ReFileData> reFileDataList, double targetRe) {
        return reFileDataList.stream()
                .min(Comparator.comparingDouble(reFileData -> Math.abs(reFileData.reValue - targetRe)))
                .orElse(null);
    }

    private static ReFileData parseReFile(String fileName) {
        double Re = 0.0;
        List<PolarData> polarDataList = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {
            String line;
            boolean startReading = false;

            while ((line = br.readLine()) != null) {
                // Check if the line contains the Re value
                if (line.contains("Re =")) {
                    String[] parts = line.trim().split("\\s+");
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].equals("Re") && parts[i+1].equals("=")) {
                            String reValue = parts[i+2] + " " + parts[i+3] + " " + parts[i+4]; // e.g., "0.100 e 6"
                            Re = parseScientificNotation(reValue);
                            break;
                        }
                    }
                }

                if (startReading) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 10) { // Check if line has exactly 10 parts
                        try {
                            double AOA = Double.parseDouble(parts[0]);
                            double CL = Double.parseDouble(parts[1]);
                            double CD = Double.parseDouble(parts[2]);

                            PolarData data = new PolarData(Re, AOA, CL, CD);
                            polarDataList.add(data);
                        } catch (NumberFormatException e) {
                        }
                    }
                } else if (line.contains("alpha") && line.contains("CL") && line.contains("CD")) {
                    startReading = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (DEBUG) System.out.println(new ReFileData(fileName, Re, polarDataList));
        return new ReFileData(fileName, Re, polarDataList);
    }

    private static double parseScientificNotation(String notation) { //e.g. "0.1 e 6"
        String[] parts = notation.split("\\s+");
        double base = Double.parseDouble(parts[0]);
        int exponent = Integer.parseInt(parts[2]);
        return base * Math.pow(10, exponent);
    }


    public static void main(String[] args) {
        InputData inputData;
        try {
            inputData = CSVParser.getInputDataFromCSV(csvPath, ",");
        } catch (FileFormatException e) {
            e.printStackTrace();
            return;
        }


        List<String> fileNames;
        try {
            fileNames = Files.walk(Paths.get(dataDirectory))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Failed to read files from data directory: " + e.getMessage());
            return;
        }

        List<ReFileData> reFileDataList = fileNames.stream()
                .map(ReCalculator::parseReFile)
                .collect(Collectors.toList());

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nPress any key to run the program or type 'exit' to quit: ");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) {
                break;
            }
            double L = inputData.L;
            double[] S = inputData.S;
            double[] Re = inputData.Re;

            System.out.println("Values from '"+csvPath+"':\n" + inputData);

            outputTableValuesForL_S_Re(L, S, Re, reFileDataList);
        }
    }

    private static void outputTableValuesForL_S_Re(double L, double[] SArray, double[] ReArray, List<ReFileData> reFileDataList) {
        System.out.println("\nRe  \tAOA \tCL    \tCD\n" + "-".repeat(30));

        if (SArray.length != ReArray.length) { System.out.println("SArray length does not match ReArray length!"); return; }

        for (int pairIndex = 0; pairIndex < SArray.length; pairIndex++) { // For each S and Re value pair
            double S=SArray[pairIndex], Re=ReArray[pairIndex];

            ReFileData closestReFile = findClosestReFile(reFileDataList, Re);

            PolarData polarData = getOptimisedPolarData(closestReFile, L, S);

            System.out.printf("%-8.0f%-8.2f%-8.5f%-8.5f%n", polarData.re, polarData.aoa, polarData.cl, polarData.cd);
        }
    }

    private static PolarData getOptimisedPolarData(ReFileData closestReFile, double L, double S) {
        PolarData closest = closestReFile.polarDataList.get(0);
        double closestCloseness = Double.MAX_VALUE; // within range of infinity
        PolarData maximised = null;
        double maxCL_CD = -Double.MAX_VALUE;

        for (PolarData polarData : closestReFile.polarDataList) {
            double CL_CD = polarData.getCL_CD();
            double closeness = polarData.getLCloseness(L, S);

            if (closeness < closestCloseness) {
                closest = polarData;
                closestCloseness = closeness;
            }

            if (closeness < LTolerance && CL_CD > maxCL_CD) {
                maximised = polarData;
                maxCL_CD = CL_CD;

                if (DEBUG) System.out.println("new Maximised Polar Data for CL/CD: (" + maximised + ")");
            }
        }
        return (maximised == null) ? closest : maximised;
    }
}
