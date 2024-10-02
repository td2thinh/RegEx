import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimplifiedEgrep {
    private Automaton automaton;
    private boolean debugMode;
    private String algoType;

    public SimplifiedEgrep(String regex, Boolean debugMode, String algoType) throws Exception {
        this.debugMode = debugMode;
        this.algoType = algoType;
        if (algoType.equalsIgnoreCase("default") || algoType.equalsIgnoreCase("uhlmann")) {
            RegExTree regexTree = RegEx.parse(regex);
            this.automaton = new Automaton();
            this.automaton.buildFromRegexTree(regexTree);
            this.automaton = this.automaton.determinize(this.automaton);
            this.automaton = this.automaton.minimizeDFA(this.automaton);
            Automaton.writeDotFile(this.automaton);
            if (debugMode) {
                System.out.println("Parsing regex: " + regex);
                System.out.println("NFA built. States: " + this.automaton.stateCount);
                System.out.println("DFA built. States: " + this.automaton.stateCount);
                System.out.println("Minimized DFA. States: " + this.automaton.stateCount);
                System.out.println("Start state: " + this.automaton.startState);
                System.out.println("End states: " + this.automaton.endStates);
            }
        } else if (algoType.equalsIgnoreCase("kmp")) {
            // TODO : KMP algorithm
        } else {
            throw new Exception("Invalid algorithm type: " + algoType);
        }
    }

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
                if (matchLine(line)) {
                    matchingLines.add("Line " + lineNumber + ": " + line);
                }
            }
        }
        return matchingLines;
    }

    private boolean matchLine(String line) {
        if (algoType.equalsIgnoreCase("uhlmann")) {
            return matchLineUhlmann(line);
        } else {
            return matchLineDefault(line);
        }
    }

    private boolean matchLineUhlmann(String line) {
        return false;
    }

    private boolean matchLineDefault(String line) {
        int currentState = automaton.startState;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            int symbol = (int) c;
            State state = automaton.transitionTable.get(currentState);
            int nextState = state.getTransition(symbol);
//            System.out.println("Current char: " + c + ", Current state: " + currentState + ", Next state: " + nextState);
            if (nextState == -1) {
                // No transition for this symbol, reset to start state
                currentState = automaton.startState;
            } else {
                currentState = nextState;
            }
            if (automaton.endStates.contains(currentState)) {
                if (debugMode) {
                    System.out.println("Match found at position " + i);
                }
                return true; // Match found
            }
        }
        return false; // No match found
    }
}