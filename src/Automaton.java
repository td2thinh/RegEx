import java.util.*;

public class Automaton {
    private static final int EPSILON = -1;  // Epsilon transitions represented by -1

    private ArrayList<int[]> transitions; // Transition table for each state
    private ArrayList<int[]> epsilonTransitions; // Epsilon transitions for each state
    private int startState;  // Start state of the automaton
    private ArrayList<Boolean> endStates;    // Accepting states of the automaton
    private int stateCount;  // Count of states in the automaton
    private ArrayList<Integer> alphabet; // Alphabet of the automaton

    public Automaton() {
        this.transitions = new ArrayList<>();
        this.epsilonTransitions = new ArrayList<>();
        this.stateCount = 0;
        this.endStates = new ArrayList<>();
        this.alphabet = new ArrayList<>();
    }

    // Generates an automaton from a regex tree
    public void buildFromRegexTree(RegExTree tree) throws Exception {
        int[] states = buildAutomaton(tree);
        this.startState = states[0];
        setEndState(states[1]); // Mark the end state as accepting
    }

    // Builds automaton recursively from the regex tree
    private int[] buildAutomaton(RegExTree tree) throws Exception {
        if (tree.subTrees.isEmpty()) {
            // Base case: leaf node (single character)
            int s1 = newState();
            int s2 = newState();
            addTransition(s1, tree.root, s2);
            alphabet.add(tree.root);
            return new int[]{s1, s2};
        }

        if (tree.root == RegEx.CONCAT) {
            // Concatenation: A . B
            int[] left = buildAutomaton(tree.subTrees.get(0));
            int[] right = buildAutomaton(tree.subTrees.get(1));
            addEpsilonTransition(left[1], right[0]);
            return new int[]{left[0], right[1]};
        }

        if (tree.root == RegEx.ALTERN) {
            // Alternation: A | B
            int s1 = newState();
            int s2 = newState();
            int[] left = buildAutomaton(tree.subTrees.get(0));
            int[] right = buildAutomaton(tree.subTrees.get(1));
            addEpsilonTransition(s1, left[0]);
            addEpsilonTransition(s1, right[0]);
            addEpsilonTransition(left[1], s2);
            addEpsilonTransition(right[1], s2);
            return new int[]{s1, s2};
        }

        if (tree.root == RegEx.ETOILE) {
            // Kleene Star: A*
            int s1 = newState();
            int s2 = newState();
            int[] subAutomaton = buildAutomaton(tree.subTrees.get(0));
            addEpsilonTransition(s1, subAutomaton[0]);
            addEpsilonTransition(s1, s2);
            addEpsilonTransition(subAutomaton[1], subAutomaton[0]);
            addEpsilonTransition(subAutomaton[1], s2);
            return new int[]{s1, s2};
        }

        throw new Exception("Unknown regex operator");
    }

    // Adds a regular transition between two states on input symbol
    private void addTransition(int from, int symbol, int to) {
        ensureStateExists(from);
        ensureStateExists(to);
        transitions.get(from)[symbol] = to;
    }

    // Adds an epsilon transition between two states
    private void addEpsilonTransition(int from, int to) {
        ensureStateExists(from);
        ensureStateExists(to);
        epsilonTransitions.get(from)[to] = 1;
    }

    // Sets the state as an end (accepting) state
    private void setEndState(int state) {
        ensureStateExists(state);
        endStates.set(state, true);
    }

    // Ensures that a state exists in the transition table
    private void ensureStateExists(int state) {
        while (state >= transitions.size()) {
            transitions.add(new int[256]);  // 256 possible input characters
            epsilonTransitions.add(new int[stateCount+1]);
            endStates.add(false);  // Initially, states are not accepting
        }
    }

    // Generates a new state
    private int newState() {
        return stateCount++;
    }

    // Prints the automaton (states, transitions, epsilon transitions, and accepting states)
    public void printAutomaton() {
        System.out.println("Start State: " + startState);
        System.out.println("End States: ");
        for (int i = 0; i < endStates.size(); i++) {
            if (endStates.get(i)) {
                System.out.println("State " + i + " is an accepting state");
            }
        }
        System.out.println("Transitions: ");
        for (int i = 0; i < transitions.size(); i++) {
            for (int j = 0; j < 256; j++) {
                if (transitions.get(i)[j] != 0) {
                    System.out.println("State " + i + " -> State " + transitions.get(i)[j] + " on input " + (char) j);
                }
            }
        }
        System.out.println("Epsilon Transitions: ");
        for (int i = 0; i < epsilonTransitions.size(); i++) {
            for (int j = 0; j < epsilonTransitions.get(i).length; j++) {
                if (epsilonTransitions.get(i)[j] == 1) {
                    System.out.println("State " + i + " -> State " + j + " on epsilon");
                }
            }
        }
    }


    // returns a set of states that are reachable from the given state on epsilon transitions
    private HashSet<Integer> epsilonClosure(int state) {
        HashSet<Integer> closedSet = new HashSet<>();
        Queue<Integer> openSet = new LinkedList<>();

        // we will go through the epsilon transitions of the state and add them to the closed set and then recursively call the epsilon closure on the new states we have added
        closedSet.add(state);
        openSet.add(state);
        while (!openSet.isEmpty()) {
            int currentState = openSet.poll();
            for (int i = 0; i < epsilonTransitions.get(currentState).length; i++) {
                if (epsilonTransitions.get(currentState)[i] == 1 && !closedSet.contains(i)) {
                    closedSet.add(i);
                    openSet.add(i);
                }
            }
        }
        return closedSet;
    }

    // Determinizes the automaton
    private Automaton determinize(Automaton automaton) {
        Automaton dfa = new Automaton();
        HashMap<HashSet<Integer>, Integer> stateMap = new HashMap<>(); // map of sets of states to new DFA state
        Queue<HashSet<Integer>> queue = new LinkedList<>(); // queue to process states in order of discovery
        HashSet<Integer> startSet = epsilonClosure(automaton.startState); // epsilon closure of start state of NDFA
        stateMap.put(startSet, dfa.newState()); // add start state to the map
        queue.add(startSet);

        // Process all sets until no more new sets are found
        while (!queue.isEmpty()) {
            HashSet<Integer> currentSet = queue.poll(); // get the next set to process
            int currentDFAState = stateMap.get(currentSet); // get DFA state corresponding to this NFA set ( used for transition creation and end state marking)

            // Process all symbols in the alphabet
            for (int symbol : automaton.alphabet) {
                HashSet<Integer> nextSet = new HashSet<>();

                // Find all states we can reach on this symbol from any state in the current set
                for (int ndfaState : currentSet) {
                    int nextNFAState = automaton.transitions.get(ndfaState)[symbol];
                    if (nextNFAState != 0) { // if there's a transition on this symbol
                        nextSet.addAll(epsilonClosure(nextNFAState)); // add epsilon closure of the next state
                    }
                }

                // If the next set is non-empty and not yet processed, add it to the queue and map
                if (!nextSet.isEmpty()) {
                    if (!stateMap.containsKey(nextSet)) {
                        int newState = dfa.newState();
                        stateMap.put(nextSet, newState);
                        queue.add(nextSet); // enqueue for processing its transitions
                    }

                    // Add the transition to the DFA
                    dfa.addTransition(currentDFAState, symbol, stateMap.get(nextSet));
                }
            }

            // Mark ending states if any of them are ending states in the NDFA
            for (int nfaState : currentSet) {
                if (automaton.endStates.get(nfaState)) {
                    dfa.setEndState(currentDFAState);
                    break;
                }
            }
        }

        dfa.startState = stateMap.get(startSet); // set the start state of the DFA
        return dfa;
    }





    public static void main(String[] args) throws Exception {
        // Example usage with a predefined regex tree
        RegExTree tree = RegEx.exampleAhoUllman(); // Example from the Aho-Ullman book
        Automaton automaton = new Automaton();
        automaton.buildFromRegexTree(tree);
//        automaton.printAutomaton();
        System.err.println(automaton.epsilonClosure(3).toString());
        Automaton dfa = automaton.determinize(automaton);
        dfa.printAutomaton();
    }

}
