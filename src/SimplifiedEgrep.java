import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimplifiedEgrep {
    private Automaton automaton;

    public SimplifiedEgrep(String regex) throws Exception {
        System.out.println("Parsing regex: " + regex);
        RegExTree regexTree = RegEx.parse(regex);
        this.automaton = new Automaton();
        this.automaton.buildFromRegexTree(regexTree);
        System.out.println("NFA built. States: " + this.automaton.stateCount);
        this.automaton = this.automaton.determinize(this.automaton);
        System.out.println("DFA built. States: " + this.automaton.stateCount);
        this.automaton = this.automaton.minimizeDFA(this.automaton);
        Automaton.writeDotFile(this.automaton);
        System.out.println("Minimized DFA. States: " + this.automaton.stateCount);
        System.out.println("Start state: " + this.automaton.startState);
        System.out.println("End states: " + this.automaton.endStates);
    }

    public List<String> searchFile(String filePath) throws IOException {
        List<String> matchingLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (matchLine(line)) {  // Convert to lowercase
                    matchingLines.add("Line " + lineNumber + ": " + line);
                }
            }
        }
        return matchingLines;
    }

    private boolean matchLine(String line) {
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
                System.out.println("Match found at position " + i);
                return true; // Match found
            }
        }
        return false; // No match found
    }

    public static void main(String[] args) {
        String regex = "A(k)*ad";  // Changed to lowercase
        String filePath = "56667-0.txt";
//        String regex = args[0] ;
//        String filePath = args[1] ;
        try {
            SimplifiedEgrep egrep = new SimplifiedEgrep(regex);
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
}