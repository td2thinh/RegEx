import strategy.KMP;
import strategy.LineMatcher;
import strategy.Uhlmann;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimplifiedEgrep {

    private boolean debugMode;
    private String algoType;
    private LineMatcher matcher;

    public SimplifiedEgrep(String regex, Boolean debugMode, String algoType) throws Exception {
        this.debugMode = debugMode;
        this.algoType = algoType;
        if (algoType.equalsIgnoreCase("default")) {
            matcher = new Uhlmann(regex);
        } else if (algoType.equalsIgnoreCase("kmp")) {
            if (!regex.contains("|") && !regex.contains("*") && !regex.contains("(") && !regex.contains(")") && !regex.contains(".")) {
                matcher = new KMP(regex);
            } else {
                matcher = new Uhlmann(regex);
                ((Uhlmann)matcher).buildAutomaton();
            }
        } else {
            throw new Exception("Invalid algorithm type: " + algoType);
        }
        if (debugMode) {
            matcher.debug();
        }
    }g

    public static void main(String[] args) {
//        if (args.length < 2) {
//            System.err.println("Usage: java sim <regex> <filePath> [algoType] <debugMode>");
//            return;
//        }

//        String regex = "S(a|g|r)*on";
        String regex = args[0];
        String filePath = args[1];
//        String filePath = "56667-0.txt";

        // Check if algoType is provided, else set default
        String algoType = (args.length > 2 && !args[2].equalsIgnoreCase("true") && !args[2].equalsIgnoreCase("false"))
                ? args[2] : "default";  // Set default algorithm here

        // Determine the debug mode, based on the position
        String debugMode = (args.length == 3 && (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")))
                ? args[2]
                : (args.length == 4 ? args[3] : "false");

        try {
            // Initialize SimplifiedEgrep with regex and debug mode
            SimplifiedEgrep egrep = new SimplifiedEgrep(regex, Boolean.parseBoolean(debugMode), algoType);

            // Search file and print results
            List<String> matchingLines = egrep.searchFile(filePath);

            if (matchingLines.isEmpty()) {
                System.out.println("No matches found.");
            } else {
                System.out.println("Matching lines:");
                for (String line : matchingLines) {
                    System.out.println(line);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<String> searchFile(String filePath) throws IOException {
        List<String> matchingLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (matcher.matchLine(line, debugMode)) {
                    matchingLines.add("Line " + lineNumber + ": " + line);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return matchingLines;
    }
}