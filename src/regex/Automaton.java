package regex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class Automaton {
    static final int EPSILON = -1;  // Epsilon transitions represented by -1

    public HashMap<Integer, State> transitionTable;
    //    private ArrayList<regex.Transition> transitions;
//    private ArrayList<int[]> transitions; // regex.Transition table for each state
//    private ArrayList<int[]> epsilonTransitions; // Epsilon transitions for each state
//    private ArrayList<Boolean> endStates;    // Accepting states of the automaton
    public int stateCount;  // Count of states in the automaton
    public int startState;  // Start state of the automaton
    public ArrayList<Integer> endStates; // Accepting states of the automaton
    private HashSet<Integer> alphabet; // Alphabet of the automaton

    public Automaton() {
        this.transitionTable = new HashMap<>();
        this.stateCount = 0;
        this.endStates = new ArrayList<>();
        this.alphabet = new HashSet<>();
    }

    public static void writeDotFile(Automaton automaton) {
        File file = new File("automaton.dot");
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("digraph RegexAutomaton {");
            writer.println("rankdir=LR;");
            writer.println("size=\"8,5\"");
            writer.println("node [shape = doublecircle];");
            for (int state : automaton.endStates) {
                writer.println(state + ";");
            }
            writer.println("node [shape = circle];");

            for (int i = 0; i < automaton.stateCount; i++) {

                State state = automaton.transitionTable.get(i);
                for (Transition transition : state.getTransitions()) {
                    writer.println(i + " -> " + transition.getToStateId() + " [ label = \"" + (char) transition.getTransitionSymbol() + "\" ];");
                }
            }
            writer.println("\n}");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // Example usage with a predefined regex tree
        RegExTree tree = RegEx.parse("S(a|g|r)*on");
//        RegExTree tree = RegEx.exampleAhoUllman();
        Automaton automaton = new Automaton();
        automaton.buildFromRegexTree(tree);
//        automaton.printAutomaton();
//        System.err.println(automaton.epsilonClosure(3).toString());
        Automaton dfa = automaton.determinize(automaton);
        dfa.printAutomaton();
        System.out.println("----------------------\n");
        System.out.println("----------------------\n");
        Automaton minimizedDFA = automaton.minimizeDFA(dfa);
        minimizedDFA.printAutomaton();
        writeDotFile(minimizedDFA);
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
        if (symbol != EPSILON) {
            alphabet.add(symbol);
        }
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
        System.out.println("Start regex.State: " + startState);
        System.out.println("End States: ");
        for (int i = 0; i < stateCount; i++) {
            State state = transitionTable.get(i);
            System.out.println("regex.State " + i + ":");
            System.out.println("Transitions: ");
            for (Transition transition : state.getTransitions()) {
                System.out.println("regex.State " + transition.getFromStateId()
                        + " -> regex.State " + transition.getToStateId() + " on input " +
                        (char) transition.getTransitionSymbol());
            }
            System.out.println("Epsilon Transitions: ");
            for (Transition transition : state.getEpsilonTransitions()) {
                System.out.println("regex.State " + transition.getFromStateId() + " -> regex.State " + transition.getToStateId() + " on epsilon");
            }
            if (state.isFinalState()) {
                System.out.println("regex.State " + i + " is an accepting state");
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

    public Automaton determinize(Automaton automaton) {
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
//                System.err.println("Checking state " + nfaState);
                if (automaton.transitionTable.get(nfaState).isFinalState()) {
//                    System.err.println("regex.State " + nfaState + " is an end state");
                    dfa.setEndState(currentDFAState);
                    break;
                }
            }
        }

        dfa.startState = stateMap.get(startSet); // set the start state of the DFA
        return dfa;
    }

    public Automaton minimizeDFA(Automaton dfa) {
        // Step 1: Create initial partition
        List<Set<Integer>> partition = new ArrayList<>();
        Set<Integer> acceptingStates = new HashSet<>(dfa.endStates); // accepting states of the DFA ( end states )
        Set<Integer> nonAcceptingStates = new HashSet<>();

        for (int i = 0; i < dfa.stateCount; i++) {
            if (!acceptingStates.contains(i)) {
                nonAcceptingStates.add(i);
            }
        }

        if (!acceptingStates.isEmpty()) {
            partition.add(acceptingStates);
        }
        if (!nonAcceptingStates.isEmpty()) {
            partition.add(nonAcceptingStates);
        }

        // Step 2: Refine partition
        boolean changed;
        do {
            changed = false;
            List<Set<Integer>> newPartition = new ArrayList<>();

            for (Set<Integer> group : partition) {
                List<Set<Integer>> subgroups = splitGroup(group, partition, dfa);
                newPartition.addAll(subgroups);
                if (subgroups.size() > 1) {
                    changed = true;
                }
            }

            partition = newPartition;
        } while (changed);

        // Step 3: Build minimized DFA
        Automaton minimizedDFA = new Automaton();
        // map a group of states to a single state in the minimized DFA
        Map<Set<Integer>, Integer> groupToStateMap = new HashMap<>();

        for (int i = 0; i < partition.size(); i++) {
            Set<Integer> group = partition.get(i);
            groupToStateMap.put(group, minimizedDFA.newState().getStateId());

            if (group.contains(dfa.startState)) {
                minimizedDFA.startState = groupToStateMap.get(group);
            }

            if (!Collections.disjoint(group, dfa.endStates)) { // if there are elements in common then we set the group to be an end state
                minimizedDFA.setEndState(groupToStateMap.get(group));
            }
        }

        // Add transitions to minimized DFA
        for (Set<Integer> group : partition) {
            int representativeState = group.iterator().next();
            int fromState = groupToStateMap.get(group);

            for (int symbol : dfa.alphabet) {
                State state = dfa.transitionTable.get(representativeState);
                int toStateInDFA = state.getTransition(symbol);

                if (toStateInDFA != -1) {
                    for (Set<Integer> toGroup : partition) {
                        if (toGroup.contains(toStateInDFA)) {
                            int toState = groupToStateMap.get(toGroup);
                            minimizedDFA.addTransition(fromState, symbol, toState);
                            break;
                        }
                    }
                }
            }
        }

        return minimizedDFA;
    }

    /**
     * Split a group of states into subgroups based on transitions
     *
     * @param group
     * @param partition
     * @param dfa
     * @return
     */
    private List<Set<Integer>> splitGroup(Set<Integer> group, List<Set<Integer>> partition, Automaton dfa) {
        if (group.size() <= 1) {
            return Collections.singletonList(group);
        }

        Map<String, Set<Integer>> subgroups = new HashMap<>();

        // Group states based on transitions
        // this will build a map of :  key( 'the state numbers concatenated together' ) -> value( the set of states that have the same transitions )
        for (int state : group) {
            StringBuilder key = new StringBuilder();
            for (int symbol : dfa.alphabet) {
                State currentState = dfa.transitionTable.get(state);
                int nextState = currentState.getTransition(symbol);
                int partitionIndex = getPartitionIndex(nextState, partition);
                key.append(partitionIndex).append(",");
            }
            subgroups.computeIfAbsent(key.toString(), k -> new HashSet<>()).add(state);
        }

        return new ArrayList<>(subgroups.values());
    }

    // Get the partition index of a state
    private int getPartitionIndex(int state, List<Set<Integer>> partition) {
        for (int i = 0; i < partition.size(); i++) {
            if (partition.get(i).contains(state)) {
                return i;
            }
        }
        return -1;
    }
}
