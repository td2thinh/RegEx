import java.lang.reflect.Array;
import java.util.*;

public class Automaton {
    static final int EPSILON = -1;  // Epsilon transitions represented by -1

    HashMap<Integer, State> transitionTable;
//    private ArrayList<Transition> transitions;
//    private ArrayList<int[]> transitions; // Transition table for each state
//    private ArrayList<int[]> epsilonTransitions; // Epsilon transitions for each state
//    private ArrayList<Boolean> endStates;    // Accepting states of the automaton
    private int stateCount;  // Count of states in the automaton
    private int startState;  // Start state of the automaton
    private ArrayList<Integer> endStates; // Accepting states of the automaton
    private ArrayList<Integer> alphabet; // Alphabet of the automaton

    public Automaton() {
        this.transitionTable = new HashMap<>();
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
            State s1 = newState();
            State s2 = newState();
            addTransition(s1.getStateId(), tree.root, s2.getStateId());
            alphabet.add(Integer.valueOf(tree.root));
            return new int[]{s1.getStateId(), s2.getStateId()};
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
            State s1 = newState();
            State s2 = newState();
            int[] left = buildAutomaton(tree.subTrees.get(0));
            int[] right = buildAutomaton(tree.subTrees.get(1));
            addEpsilonTransition(s1.getStateId(), left[0]);
            addEpsilonTransition(s1.getStateId(), right[0]);
            addEpsilonTransition(left[1], s2.getStateId());
            addEpsilonTransition(right[1], s2.getStateId());
            return new int[]{s1.getStateId(), s2.getStateId()};
        }

        if (tree.root == RegEx.ETOILE) {
            // Kleene Star: A*
            State s1 = newState();
            State s2 = newState();
            int[] subAutomaton = buildAutomaton(tree.subTrees.get(0));
            addEpsilonTransition(s1.getStateId(), subAutomaton[0]);
            addEpsilonTransition(s1.getStateId(), s2.getStateId());
            addEpsilonTransition(subAutomaton[1], subAutomaton[0]);
            addEpsilonTransition(subAutomaton[1], s2.getStateId());
            return new int[]{s1.getStateId(), s2.getStateId()};
        }

        throw new Exception("Unknown regex operator");
    }

    // Adds a regular transition between two states on input symbol
    private void addTransition(int from, int symbol, int to) {
        ensureStateExists(from);
        ensureStateExists(to);
        transitionTable.get(from).addTransition(new Transition(from, to, symbol));
    }

    // Adds an epsilon transition between two states
    private void addEpsilonTransition(int from, int to) {
        ensureStateExists(from);
        ensureStateExists(to);
        transitionTable.get(from).addTransition(new Transition(from, to, EPSILON));
    }

    // Sets the state as an end (accepting) state
    private void setEndState(int state) {
        ensureStateExists(state);
        endStates.add(transitionTable.get(state).getStateId());
        transitionTable.get(state).setFinalState(true);
    }

    // Ensures that a state exists in the transition table
    private void ensureStateExists(int state) {
        if (state >= stateCount) {
            for (int i = stateCount; i <= state; i++) {
                State newState = new State(i, false, false);
                transitionTable.put(i, newState);
            }
            stateCount = state + 1;
        }
    }

    // Generates a new state
    private State newState() {
        State state = new State(stateCount++, false, false);
        transitionTable.put(state.getStateId(), state);
        return state;
    }

    // Prints the automaton (states, transitions, epsilon transitions, and accepting states)
    public void printAutomaton() {
        System.out.println("Start State: " + startState);
        System.out.println("End States: ");
        for (int i = 0; i < stateCount; i++) {
            State state = transitionTable.get(i);
            System.out.println("State " + i + ":");
            System.out.println("Transitions: ");
            for (Transition transition : state.getTransitions()) {
                System.out.println("State " + transition.getFromStateId()
                        + " -> State " + transition.getToStateId() + " on input " +
                        (char) transition.getTransitionSymbol());
            }
            System.out.println("Epsilon Transitions: ");
            for (Transition transition : state.getEpsilonTransitions()) {
                System.out.println("State " + transition.getFromStateId() + " -> State " + transition.getToStateId() + " on epsilon");
            }
            if (state.isFinalState()) {
                System.out.println("State " + i + " is an accepting state");
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
//            for (int i = 0; i < epsilonTransitions.get(currentState).length; i++) {
//                if (epsilonTransitions.get(currentState)[i] == 1 && !closedSet.contains(i)) {
//                    closedSet.add(i);
//                    openSet.add(i);
//                }
//            }
            State state1 = transitionTable.get(openSet.poll());
            for (Transition transition : state1.getEpsilonTransitions()) {
                if (!closedSet.contains(transition.getToStateId())) {
                    closedSet.add(transition.getToStateId());
                    openSet.add(transition.getToStateId());
                }
            }
        }
        return closedSet;
    }

    private Automaton determinize(Automaton automaton) {
        Automaton dfa = new Automaton();
        HashMap<HashSet<Integer>, Integer> stateMap = new HashMap<>(); // map of sets of states to new DFA state
        Queue<HashSet<Integer>> queue = new LinkedList<>(); // queue to process states in order of discovery
        HashSet<Integer> startSet = epsilonClosure(automaton.startState); // epsilon closure of start state of NDFA
        stateMap.put(startSet, dfa.newState().getStateId()); // add start state to the map
        queue.add(startSet);

        // Process all sets until no more new sets are found
        while (!queue.isEmpty()) {
            HashSet<Integer> currentSet = queue.poll(); // get the next set to process
            int currentDFAState = stateMap.get(currentSet); // get DFA state corresponding to this NFA set

            // Process all symbols in the alphabet
            for (int symbol : automaton.alphabet) {
                HashSet<Integer> nextSet = new HashSet<>();

                // Find all states we can reach on this symbol from any state in the current set
                for (int ndfaState : currentSet) {
                    State currentNFAState = automaton.transitionTable.get(ndfaState);
                    if (currentNFAState != null) {
                        int nextNFAState = currentNFAState.getTransition(symbol);
                        if (nextNFAState != -1) { // if there's a transition on this symbol
                            nextSet.addAll(epsilonClosure(nextNFAState)); // add epsilon closure of the next state
                        }
                    }
                }

                // If the next set is non-empty and not yet processed, add it to the queue and map
                if (!nextSet.isEmpty()) {
                    if (!stateMap.containsKey(nextSet)) {
                        int newState = dfa.newState().getStateId();
                        stateMap.put(nextSet, newState);
                        queue.add(nextSet); // enqueue for processing its transitions
                    }

                    // Add the transition to the DFA
                    dfa.addTransition(currentDFAState, symbol, stateMap.get(nextSet));
                }
            }

            // Mark ending states if any of them are ending states in the NDFA
            for (int nfaState : currentSet) {
                System.err.println("Checking state " + nfaState);
                if ( automaton.transitionTable.get(nfaState).isFinalState() ) {
                    System.err.println("State " + nfaState + " is an end state");
                    dfa.setEndState(currentDFAState);
                    break;
                }
            }
        }

        dfa.startState = stateMap.get(startSet); // set the start state of the DFA
        return dfa;
    }


//    private boolean isDistinguishable(int state1, int state2, ArrayList<Integer> partitions){
//            for (int alpha : alphabet) {
//                if (transitions.get(state1)[alpha] != transitions.get(state2)[alpha] &&
//                        (partitions.contains(transitions.get(state1)[alpha]) && partitions.contains(transitions.get(state2)[alpha]))) {
//                    return true;
//                }
//            }
//        return true;
//    }
//
//    private ArrayList<ArrayList<Integer>> partition(ArrayList<ArrayList<Integer>> toPartition){
//        ArrayList<ArrayList<Integer>> partitions = new ArrayList<>();
//
//        for (ArrayList<Integer> part : toPartition) {
//            for (int i = 0; i < part.size(); i++) {
//                ArrayList<Integer> newPart = new ArrayList<>();
//                newPart.add(part.get(i));
//                for (int j = i + 1; j < part.size(); j++) {
//                    if (!isDistinguishable(part.get(i), part.get(j), part)) {
//                       // not distinguishable so the partition stays the same
//                        newPart.add(part.get(j));
//                    }else{
//                        // distinguishable so we create a new partition and partition the rest of the states
//                        ArrayList<Integer> newPart2 = new ArrayList<>();
//                        newPart2.add(part.get(j));
//                        for (int k = j + 1; k < part.size(); k++) {
//                            if (!isDistinguishable(part.get(j), part.get(k), part)) {
//                                newPart2.add(part.get(k));
//                            }
//                        }
//
//                    }
//                }
//                partitions.add(newPart);
//
//            }
//        }
//        return  partitions;
//    }
//
//    private Automaton minimize(Automaton automaton){
//        Automaton dfa = new Automaton();
//        // step 1 : accept and non accept states
//        ArrayList<Integer> acceptingStates = new ArrayList<>();
//        ArrayList<Integer> nonAcceptingStates = new ArrayList<>();
//        for (int i = 0; i < automaton.endStates.size(); i++) {
//            if (automaton.endStates.get(i)) {
//                acceptingStates.add(i);
//            } else {
//                nonAcceptingStates.add(i);
//            }
//        }
//
//        int k = 1;
//
//        ArrayList<ArrayList<Integer>> partitions = new ArrayList<>();
//        partitions.add(acceptingStates);
//        partitions.add(nonAcceptingStates);
//
//
//        return  dfa;
//    }





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
