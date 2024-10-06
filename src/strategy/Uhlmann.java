package strategy;

import regex.Automaton;
import regex.RegEx;
import regex.RegExTree;
import regex.State;

public class Uhlmann implements LineMatcher {
    private Automaton automaton;
    private final String regex;
    private RegExTree regexTree;

    public Uhlmann(String regex) {
        this.regex = regex;
    }

    public void buildAutomaton() throws Exception {
        parseRegex();
        buildNFA();
        determinize();
        minimize();
        writeDotFile();
    }

    public void parseRegex() throws Exception {
        this.regexTree = RegEx.parse(regex);
    }

    public void buildNFA() throws Exception {
        this.automaton = new Automaton();
        this.automaton.buildFromRegexTree(regexTree);
    }

    public void determinize() {
        this.automaton = this.automaton.determinize(this.automaton);
    }

    public void minimize() {
        this.automaton = this.automaton.minimizeDFA(this.automaton);
    }

    public void writeDotFile() {
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
            if (nextState == -1) {
                currentState = automaton.startState;
            } else {
                currentState = nextState;
            }
            if (automaton.endStates.contains(currentState)) {
                if (debugMode) {
                    System.out.println("Match found at position " + i);
                }
                return true;
            }
        }
        return false;
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