package strategy;

import regex.Automaton;
import regex.RegEx;
import regex.RegExTree;
import regex.State;

public class Uhlmann implements LineMatcher {
    private Automaton automaton;
    private final String regex;

    public Uhlmann(String regex) throws Exception {
        this.regex = regex;
        RegExTree regexTree = RegEx.parse(regex);
        this.automaton = new Automaton();
        this.automaton.buildFromRegexTree(regexTree);
        this.automaton = this.automaton.determinize(this.automaton);
        this.automaton = this.automaton.minimizeDFA(this.automaton);
        Automaton.writeDotFile(this.automaton);
    }


    @Override
    public boolean matchLine(String line, boolean debugMode) throws Exception {
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

    @Override
    public void debug() throws Exception {
        System.out.println("Parsing regex: " + regex);
        System.out.println("NFA built. States: " + this.automaton.stateCount);
        System.out.println("DFA built. States: " + this.automaton.stateCount);
        System.out.println("Minimized DFA. States: " + this.automaton.stateCount);
        System.out.println("Start state: " + this.automaton.startState);
        System.out.println("End states: " + this.automaton.endStates);
    }
}
